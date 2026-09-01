# VoiceKit licensing

## The design in one line

**The entitlement travels inside the key, signed — so verification is fully offline and there is no
licence server.**

An SDK whose entire selling point is that it works with the radio off cannot require a network call
to start. So a key is self-describing: it carries who it is for, what tier, and when it expires, and
the SDK verifies that against an issuer public key compiled into the AAR.

Nothing to host, nothing to pay for, nothing to have an outage. The trade is that **revocation needs
a key rotation rather than a server flag** — at tens of B2B customers, that is the cheaper problem.

## Key format

```
vk_<env>_<payload>.<signature>
     │      │          └── ECDSA P-256 / SHA-256 over the payload bytes, base64url
     │      └───────────── base64url of `app=..;cert=..;tier=..;exp=..`
     └──────────────────── live | test
```

Compact record rather than JSON so the key fits on one `gradle.properties` line without wrapping,
which is where these actually live.

## Issuing keys

```bash
cd voicekit/tools

java IssueLicense.java keygen            # once, ever. Writes issuer-private.key
java IssueLicense.java issue --app com.acme.notes \
                             --cert 3A:F1:... --tier PRO --days 365
java IssueLicense.java issue --app com.acme.notes --test --days 30
```

`keygen` prints the public half to paste into `License.kt` → `ISSUER_PUBLIC_KEY_B64`.

🔴 **`issuer-private.key` must never be committed.** It is gitignored. If it leaks, anyone can mint
licences; if it is lost, no new licences can be issued and every existing one still verifies.

## What is enforced, and what is deliberately not

| Check | Behaviour |
|---|---|
| Malformed key | **Throws** at `initialize` — always the developer's own build being wrong, best found immediately |
| Signature invalid / foreign issuer | **Throws** `UNKNOWN_KEY` |
| Wrong `applicationId` or signing certificate | **Throws** `APP_MISMATCH` — a leaked key is useless elsewhere |
| **Subscription lapsed** | **Keeps working.** Logs loudly, and `VoiceKit.license` reports `Lapsed` |
| Inference | **Never gated on anything.** No network, ever |

### Why a lapsed licence does not throw

Bricking a shipped app because the customer's card expired punishes *their users* for *their billing
problem*. It is the fastest way to make an SDK un-adoptable, and it turns a renewal conversation into
an outage. The lapse is loud in the log and readable at `VoiceKit.license` so the customer's own
crash reporter surfaces it. Recovering that revenue is a conversation, not a kill switch.

Pinned by `LicenseTest.aLapsedLicenceReportsLapsedAndDoesNotThrow`.

### `vk_test_` keys are unbound on purpose

So a developer can evaluate without sending us their signing certificate first. They are logged with
a warning and must not ship. Pinned by `LicenseTest.testKeysSkipBindingAndLiveKeysDoNot`.

## 🔴 Current state: pre-revenue

`ISSUER_PUBLIC_KEY_B64` is **empty**, so signature verification is **structural only** — any
well-formed key is accepted. Binding, expiry and shape checks all still run.

This is deliberate: it lets evaluation keys be handed out before the signing key exists. **Run
`keygen`, paste the public key in, and rebuild before the first paid licence goes out.** `initialize`
logs a line on every start while this is the case, so it cannot be forgotten silently.

## What this does not do, honestly

You cannot technically stop a determined pirate from using an on-device SDK. The AAR decompiles, the
check can be stripped, and the underlying models (Whisper, sherpa-onnx) are open source and free —
what is sold is the productized integration, not the weights.

The controls that actually matter, in order:

1. **Gate the model CDN with the licence key.** Piracy then costs stripping the check *and*
   re-hosting 274 MB of models. Highest leverage. **Client and server are now built** — see
   `projects/voicekit-distribution/` — but **nothing is deployed**, so today the catalog still
   points at public HuggingFace mirrors and the gate does nothing. Set
   `VoiceKitConfig.distributionEndpoint` once the worker is live.
2. **App binding** — done.
3. **R8 obfuscation.** Raises cost; does not prevent.
4. **The protection that actually works is commercial.** At $99–199/mo the buyers are companies, and
   companies do not pirate SDKs — they need support, updates, and a licence their legal team can
   point at. Piracy risk is concentrated in the segment that was never going to pay.

⇒ Per the build plan: **do not spend nights on anti-piracy before the first paying customer.**


## Model distribution gating

Built 2026-08-26. `projects/voicekit-distribution/` — a Cloudflare Worker that validates the licence
and **302-redirects to a short-lived signed R2 URL**. It never streams model bytes: a worker in the
path of a 274 MB download is slow, expensive, and becomes the thing that breaks when a customer
onboards ten thousand users at once.

```kotlin
VoiceKit.initialize(context, licenseKey, VoiceKitConfig(
    distributionEndpoint = "https://voice.tricodestudio.com",  // null = public mirrors, ungated
))
```

### The deliberate asymmetry

| | Lapsed licence |
|---|---|
| **Inference** | Keeps working — never break a shipped app |
| **New model downloads** | **Refused** by the endpoint |

Existing users are unaffected; the account cannot keep *growing* on an unpaid licence. That is the
commercial pressure point, and it costs nobody an outage.

### What the server checks that the SDK cannot

**Revocation** — a key can be killed server-side without a client update, closing the offline
design's one real gap on the side that can be closed. And **expiry against a clock the customer does
not control**.

### Integrity

`ModelStore` now verifies **SHA-256** on every downloaded file when `ModelFileSpec.sha256` is
present. Size alone was the only check before, and a substituted or wrong-revision model passes a
size check then decodes to nonsense — far more expensive to diagnose than a failed download.

🔴 **Every catalog digest is currently null**, because the public mirrors' digests have not been
verified and asserting an unchecked hash is worse than asserting none. Fill them in at upload time,
when they are known. Pinned by `ModelSourceTest.catalogDigestsAreUnpinnedUntilModelsMoveToOurOwnDistribution`.
