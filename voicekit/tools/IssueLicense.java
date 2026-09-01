import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * VoiceKit licence issuer. Single file, no dependencies, runs straight from source:
 *
 *   java IssueLicense.java keygen
 *   java IssueLicense.java issue --app com.acme.notes --cert AB:CD:.. --tier PRO --days 365
 *   java IssueLicense.java issue --app com.acme.notes --test --days 30
 *
 * `keygen` writes issuer-private.key (KEEP THIS SECRET, never commit) and prints the public half to
 * paste into License.kt's ISSUER_PUBLIC_KEY_B64. Until that constant is set, the SDK accepts any
 * well-formed key — which is intentional pre-revenue, and must be closed before the first paid seat.
 */
public final class IssueLicense {

    private static final Path PRIVATE_KEY = Path.of("issuer-private.key");
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }
        switch (args[0]) {
            case "keygen" -> keygen();
            case "issue"  -> issue(args);
            default       -> usage();
        }
    }

    private static void usage() {
        System.out.println("""
            VoiceKit licence issuer

              keygen                            generate the issuing keypair (once, ever)
              issue --app <applicationId>       mint a key
                    [--cert <SHA-256>]          bind to a signing certificate (recommended)
                    [--tier TRIAL|PRO|ENTERPRISE]  default PRO
                    [--days N]                  default 365
                    [--test]                    mint vk_test_ (unbound, for evaluation)
            """);
    }

    private static void keygen() throws Exception {
        if (Files.exists(PRIVATE_KEY)) {
            System.err.println("issuer-private.key already exists. Refusing to overwrite it —");
            System.err.println("regenerating invalidates every licence already issued.");
            System.exit(1);
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = gen.generateKeyPair();

        try (FileOutputStream out = new FileOutputStream(PRIVATE_KEY.toFile())) {
            out.write(pair.getPrivate().getEncoded());
        }
        PRIVATE_KEY.toFile().setReadable(false, false);
        PRIVATE_KEY.toFile().setReadable(true, true);

        System.out.println("Wrote " + PRIVATE_KEY.toAbsolutePath());
        System.out.println("\nPaste this into License.kt -> ISSUER_PUBLIC_KEY_B64:\n");
        System.out.println(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        System.out.println("""

            Keep issuer-private.key out of git. If it leaks, anyone can mint licences; if it is
            lost, no new licences can be issued and every existing one still verifies.""");
    }

    private static void issue(String[] args) throws Exception {
        String app = null, cert = "", tier = "PRO";
        int days = 365;
        boolean test = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--app"  -> app  = args[++i];
                case "--cert" -> cert = args[++i].replace(":", "").toUpperCase();
                case "--tier" -> tier = args[++i].toUpperCase();
                case "--days" -> days = Integer.parseInt(args[++i]);
                case "--test" -> test = true;
                default -> { System.err.println("unknown flag: " + args[i]); System.exit(1); }
            }
        }
        if (app == null) { System.err.println("--app is required"); System.exit(1); }

        long expires = Instant.now().plus(days, ChronoUnit.DAYS).toEpochMilli();
        // Must match LicenseVerifier.parsePayload exactly: a compact field=value; record.
        String payload = "app=" + app + ";cert=" + cert + ";tier=" + tier + ";exp=" + expires;
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        String sig;
        if (Files.exists(PRIVATE_KEY)) {
            PrivateKey key = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(PRIVATE_KEY)));
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(key);
            signer.update(payloadBytes);
            sig = B64.encodeToString(signer.sign());
        } else {
            // Pre-revenue: the SDK accepts structurally valid keys, so an unsigned one still works
            // for evaluation. Say so rather than silently minting something that looks signed.
            System.err.println("WARNING: no issuer-private.key — minting an UNSIGNED key.");
            System.err.println("Fine for evaluation. Run `keygen` before the first paid licence.\n");
            sig = "unsigned";
        }

        String key = "vk_" + (test ? "test" : "live") + "_" + B64.encodeToString(payloadBytes) + "." + sig;

        System.out.println("applicationId : " + app);
        System.out.println("certificate   : " + (cert.isEmpty() ? "(unbound)" : cert));
        System.out.println("tier          : " + tier);
        System.out.println("expires       : " + Instant.ofEpochMilli(expires));
        System.out.println("\n" + key + "\n");
    }
}
