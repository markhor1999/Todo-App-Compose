package com.tricodestudio.voicekit

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * What a licence key entitles the holder to.
 *
 * The entitlement travels **inside the key**, signed. That is the whole design: an SDK whose selling
 * point is that it works with the radio off cannot require a licence server to start, so the key is
 * made self-describing and verified offline against a public key compiled into the SDK. There is no
 * licence server to run, to pay for, or to have an outage.
 *
 * The trade is that revocation needs a key rotation rather than a server flag. At the scale this is
 * sold at — tens of companies, not millions of users — that is the cheaper problem.
 */
public data class Entitlement(
    /** `applicationId` this key is issued to. */
    public val applicationId: String,
    /** SHA-256 of the signing certificate, uppercase hex, colon-free. Empty means unbound. */
    public val certificateSha256: String,
    /** Commercial tier. Affects support and terms, not which models decode. */
    public val tier: LicenseTier,
    /** Epoch millis after which the subscription has lapsed. */
    public val expiresAtMillis: Long,
    /** True for `vk_test_` keys — bound loosely, never valid in a Play release. */
    public val isTest: Boolean,
) {
    /** Whether the subscription has lapsed as of [now]. */
    public fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > expiresAtMillis
}

/** Commercial tier carried in the key. */
public enum class LicenseTier {
    /** Evaluation. Full engine, and the SDK says so in the log on every start. */
    TRIAL,

    /** A paid seat. */
    PRO,

    /** Negotiated terms. */
    ENTERPRISE,
    ;

    internal companion object {
        fun parse(raw: String): LicenseTier =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TRIAL
    }
}

/**
 * The licence state after [VoiceKit.initialize].
 *
 * Exposed rather than kept private because a developer shipping this needs to be able to see, in
 * their own crash reporter, that their key lapsed — without the SDK deciding to break their app.
 */
public sealed interface LicenseStatus {

    /** Valid and in date. */
    public data class Active(val entitlement: Entitlement) : LicenseStatus

    /**
     * The key is genuine but the subscription lapsed. **The SDK keeps working.**
     *
     * Bricking a shipped app because a card expired would punish the customer's users for the
     * customer's billing problem, and it is the single fastest way to make an SDK un-adoptable. The
     * lapse is loud in the log and visible here; recovering the revenue is a conversation, not a
     * kill switch.
     */
    public data class Lapsed(val entitlement: Entitlement, val sinceMillis: Long) : LicenseStatus

    /** No licence was accepted. [reason] says why. Only reachable in a `vk_test_` build. */
    public data class Unlicensed(val reason: VoiceKitException.InvalidLicense.Reason) : LicenseStatus
}

/**
 * Parses and verifies a licence key.
 *
 * Format: `vk_<env>_<payload>.<signature>`, both parts base64url, where the payload is a compact
 * `field=value;` record and the signature is ECDSA-P256/SHA-256 over the payload bytes. ECDSA rather
 * than Ed25519 because `java.security` has had it since API 1 — an SDK that needs a crypto
 * dependency to check its own licence is one more reason not to adopt it.
 */
internal object LicenseVerifier {

    private const val PREFIX = "vk_"

    /**
     * Public half of the issuing key, X.509/SPKI, base64.
     *
     * A public key in a shipped artifact is not a secret and does not need to be one. Stripping the
     * check entirely is always possible on-device — see the build plan: the control that actually
     * bites is gating the model CDN, and the protection that matters is commercial.
     */
    private const val ISSUER_PUBLIC_KEY_B64: String = ""

    /**
     * The issuer key actually used to verify, overridable in tests.
     *
     * A signature check that is never exercised against a *wrong* key is indistinguishable from no
     * check at all, and this one ships disabled pre-revenue — so the tests have to be able to turn
     * it on and prove both directions.
     */
    internal var issuerPublicKey: String = ISSUER_PUBLIC_KEY_B64

    /** Shape check only — no signature, no binding. Kept cheap so it can run before any I/O. */
    fun parseShape(key: String): Result<Pair<String, String>> {
        val trimmed = key.trim()
        if (!trimmed.startsWith(PREFIX)) return malformed()
        val body = trimmed.removePrefix(PREFIX)
        val env = body.substringBefore('_', missingDelimiterValue = "")
        if (env != "live" && env != "test") return malformed()
        val rest = body.substringAfter('_', missingDelimiterValue = "")
        if (!rest.contains('.')) return malformed()
        val payload = rest.substringBeforeLast('.')
        val signature = rest.substringAfterLast('.')
        if (payload.isBlank() || signature.isBlank()) return malformed()
        return Result.success(env to rest)
    }

    private fun <T> malformed(): Result<T> = Result.failure(
        VoiceKitException.InvalidLicense(VoiceKitException.InvalidLicense.Reason.MALFORMED)
    )

    /**
     * Full verification: shape, signature, then binding against [identity].
     *
     * Returns a [LicenseStatus] rather than throwing for a *lapsed* key — expiry is a commercial
     * state, not an integration error. Malformed, unsigned and mis-bound keys do throw, because
     * those are always the developer's own build being wrong and are best discovered immediately.
     */
    fun verify(key: String, identity: AppIdentity): LicenseStatus {
        val (env, rest) = parseShape(key).getOrElse { throw it }
        val isTest = env == "test"
        val payloadB64 = rest.substringBeforeLast('.')
        val signatureB64 = rest.substringAfterLast('.')

        val payloadBytes = decode(payloadB64)
            ?: throw VoiceKitException.InvalidLicense(
                VoiceKitException.InvalidLicense.Reason.MALFORMED
            )

        if (!signatureVerifies(payloadBytes, signatureB64)) {
            throw VoiceKitException.InvalidLicense(
                VoiceKitException.InvalidLicense.Reason.UNKNOWN_KEY
            )
        }

        val entitlement = parsePayload(String(payloadBytes, Charsets.UTF_8), isTest)
            ?: throw VoiceKitException.InvalidLicense(
                VoiceKitException.InvalidLicense.Reason.MALFORMED
            )

        // Binding. A test key is deliberately loose so a developer can evaluate without telling us
        // their signing certificate first; a live key is bound to both, so a leaked key is useless
        // in someone else's app.
        if (!isTest) {
            val appMatches = entitlement.applicationId == identity.applicationId
            val certMatches = entitlement.certificateSha256.isEmpty() ||
                entitlement.certificateSha256.equals(identity.certificateSha256, ignoreCase = true)
            if (!appMatches || !certMatches) {
                throw VoiceKitException.InvalidLicense(
                    VoiceKitException.InvalidLicense.Reason.APP_MISMATCH
                )
            }
        }

        val now = System.currentTimeMillis()
        return if (entitlement.isExpired(now)) {
            LicenseStatus.Lapsed(entitlement, sinceMillis = entitlement.expiresAtMillis)
        } else {
            LicenseStatus.Active(entitlement)
        }
    }

    /**
     * `app=..;cert=..;tier=..;exp=..` — a compact record rather than JSON so the key stays short
     * enough to paste into a `gradle.properties` line without wrapping, which is where these live.
     */
    private fun parsePayload(payload: String, isTest: Boolean): Entitlement? {
        val fields = payload.split(';')
            .mapNotNull { field ->
                val i = field.indexOf('=')
                if (i <= 0) null else field.substring(0, i) to field.substring(i + 1)
            }
            .toMap()
        val app = fields["app"] ?: return null
        val exp = fields["exp"]?.toLongOrNull() ?: return null
        return Entitlement(
            applicationId = app,
            certificateSha256 = fields["cert"].orEmpty(),
            tier = LicenseTier.parse(fields["tier"].orEmpty()),
            expiresAtMillis = exp,
            isTest = isTest,
        )
    }

    private fun signatureVerifies(payload: ByteArray, signatureB64: String): Boolean {
        // No issuer key compiled in yet: pre-revenue, every well-formed key is accepted and the
        // binding checks below still run. This is deliberate and logged — see VoiceKit.initialize.
        // Ship a real key here before the first paid licence goes out.
        if (issuerPublicKey.isBlank()) return true
        return try {
            val sig = decode(signatureB64) ?: return false
            val spec = X509EncodedKeySpec(decode(issuerPublicKey) ?: return false)
            val publicKey = KeyFactory.getInstance("EC").generatePublic(spec)
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(payload)
                verify(sig)
            }
        } catch (t: Throwable) {
            // A malformed signature is an invalid key, never a crash in the host app's onCreate.
            false
        }
    }

    /** True while no issuer key is compiled in — signatures are structural only. */
    val isPreRevenueBuild: Boolean get() = issuerPublicKey.isBlank()

    private fun decode(b64: String): ByteArray? = Base64Url.decode(b64)
}

/**
 * Base64url (RFC 4648 §5), decode only, unpadded-tolerant.
 *
 * Hand-rolled rather than using `android.util.Base64` or `java.util.Base64` for two reasons that
 * both matter for an SDK: the former makes the licence path untestable in a plain JVM test (it is
 * stubbed to null), and the latter needs API 26 while this library targets 24. Keeping the whole
 * licence path free of platform APIs also means it moves to Kotlin Multiplatform unchanged when iOS
 * lands, which is the differentiator the roadmap is built on.
 */
internal object Base64Url {

    private val TABLE = IntArray(128) { -1 }.apply {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        alphabet.forEachIndexed { index, c -> this[c.code] = index }
        // Accept the standard alphabet's two differing characters as well. Licence keys are
        // base64url, but the compiled-in issuer public key is standard base64 as every keytool and
        // openssl invocation emits it — decoding both here beats a second decoder, or a footgun
        // where pasting a correct key silently fails to verify.
        this['+'.code] = 62
        this['/'.code] = 63
    }

    fun decode(input: String): ByteArray? {
        val src = input.trimEnd('=')
        if (src.isEmpty()) return ByteArray(0)

        val out = ByteArray(src.length * 3 / 4)
        var buffer = 0
        var bits = 0
        var written = 0

        for (c in src) {
            val value = if (c.code < 128) TABLE[c.code] else -1
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[written++] = ((buffer shr bits) and 0xFF).toByte()
            }
        }
        // Leftover bits must be zero padding; anything else is a corrupted string, not a short one.
        if (bits >= 6 || (buffer and ((1 shl bits) - 1)) != 0) return null
        return if (written == out.size) out else out.copyOf(written)
    }
}
