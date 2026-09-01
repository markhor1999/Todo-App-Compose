package com.tricodestudio.voicekit

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The licence path, end to end, against keys minted by the real issuer (`voicekit/tools/IssueLicense.java`).
 *
 * The keys below were produced by that tool with the keypair whose public half is [ISSUER_PUBLIC],
 * so these tests exercise the actual wire format rather than a re-implementation of it — if the
 * issuer and the verifier ever disagree about the payload encoding, that is exactly the bug that
 * would ship a licence nobody can use, and it would not be caught by testing the verifier alone.
 */
class LicenseTest {

    private companion object {
        /** Public half of the throwaway keypair used to mint the fixtures below. */
        const val ISSUER_PUBLIC =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPe6fpJzPnLdXSAGNAehPmbegcvLGUGQCxEWcSi5k+8P/" +
                "5oJwaO0Ubrj14i9WxuPD+UxDHLS8t7ekqA2V5N3Gow=="

        /** app=com.acme.notes; cert=ABCDEF0123; tier=PRO; expires 2027. */
        const val SIGNED_LIVE =
            "vk_live_YXBwPWNvbS5hY21lLm5vdGVzO2NlcnQ9QUJDREVGMDEyMzt0aWVyPVBSTztleHA9MTgxOTI3" +
                "NDczMTEyOQ.MEYCIQDlvfeJzGb_N1veBRY2snLxrBpLVazml_Yh_Iiw7-ndwAIhALfzSSBr9J1_VOwh" +
                "l8jZBTctjNW1_SMzk3Nysajm8yvZ"

        /** app=com.acme.notes; unbound cert; expired five days before it was minted. */
        const val SIGNED_EXPIRED =
            "vk_live_YXBwPWNvbS5hY21lLm5vdGVzO2NlcnQ9O3RpZXI9UFJPO2V4cD0xNzg3MzA2NzMxNDI2" +
                ".MEQCIH1X1H_7PeG_bEK51g8vpo5u5OPEAZa43lZYbbqfEYMBAiAfti5vNP4GwoV4TPS0FNsNUCiTjXIwW43eL5e1kpLIcQ"

        val ACME = AppIdentity("com.acme.notes", "ABCDEF0123")
    }

    @After
    fun tearDown() {
        LicenseVerifier.issuerPublicKey = ""
    }

    // ---------- signature ----------

    @Test
    fun aSignedKeyVerifiesAgainstTheIssuerPublicKey() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        val status = LicenseVerifier.verify(SIGNED_LIVE, ACME)
        assertTrue("expected Active, got $status", status is LicenseStatus.Active)
        val e = (status as LicenseStatus.Active).entitlement
        assertEquals("com.acme.notes", e.applicationId)
        assertEquals(LicenseTier.PRO, e.tier)
        assertEquals(false, e.isTest)
    }

    /**
     * The direction that actually matters. Signature verification that only ever sees the correct
     * key proves nothing — anyone could mint licences if this passed.
     */
    @Test
    fun aKeySignedByADifferentIssuerIsRejected() {
        // A structurally valid P-256 public key from a different keypair.
        LicenseVerifier.issuerPublicKey =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo3HqPHR7T4rXG5w0RcXcH2Xk1P6oQq5vDqZ8gJx0nZ4" +
                "8yUu1u5PjHgVQ8yF1ZG5xk2Bx7mJ0oQ3TjQb2rN8sZA=="
        val error = runCatching { LicenseVerifier.verify(SIGNED_LIVE, ACME) }.exceptionOrNull()
        assertTrue(
            "a foreign-signed key must be rejected, got $error",
            error is VoiceKitException.InvalidLicense &&
                error.reason == VoiceKitException.InvalidLicense.Reason.UNKNOWN_KEY,
        )
    }

    @Test
    fun aTamperedPayloadIsRejected() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        // Flip one character of the payload; the signature no longer covers it.
        val i = SIGNED_LIVE.indexOf('.')
        val tampered = SIGNED_LIVE.substring(0, i - 1) + "A" + SIGNED_LIVE.substring(i)
        val error = runCatching { LicenseVerifier.verify(tampered, ACME) }.exceptionOrNull()
        assertTrue("expected rejection, got $error", error is VoiceKitException.InvalidLicense)
    }

    // ---------- binding ----------

    @Test
    fun aKeyForAnotherAppIsRejectedAsAppMismatch() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        val other = AppIdentity("com.someoneelse.app", "ABCDEF0123")
        val error = runCatching { LicenseVerifier.verify(SIGNED_LIVE, other) }.exceptionOrNull()
        assertTrue(
            "a leaked key must be useless in another app, got $error",
            error is VoiceKitException.InvalidLicense &&
                error.reason == VoiceKitException.InvalidLicense.Reason.APP_MISMATCH,
        )
    }

    @Test
    fun aKeyForAnotherSigningCertificateIsRejected() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        val resigned = AppIdentity("com.acme.notes", "0000000000")
        val error = runCatching { LicenseVerifier.verify(SIGNED_LIVE, resigned) }.exceptionOrNull()
        assertTrue(
            "re-signing the APK must not carry the licence with it, got $error",
            error is VoiceKitException.InvalidLicense &&
                error.reason == VoiceKitException.InvalidLicense.Reason.APP_MISMATCH,
        )
    }

    @Test
    fun anUnboundCertificateInTheKeyBindsOnApplicationIdAlone() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        // SIGNED_EXPIRED carries cert= (empty). It should reach the expiry check, not fail binding.
        val anyCert = AppIdentity("com.acme.notes", "9999999999")
        val status = LicenseVerifier.verify(SIGNED_EXPIRED, anyCert)
        assertTrue("expected Lapsed, got $status", status is LicenseStatus.Lapsed)
    }

    // ---------- expiry is commercial, not fatal ----------

    /**
     * The decision this test exists to protect: a lapsed subscription must never throw. Bricking a
     * shipped app because the customer's card expired punishes their users for their billing
     * problem, and is the fastest way to make an SDK un-adoptable.
     */
    @Test
    fun aLapsedLicenceReportsLapsedAndDoesNotThrow() {
        LicenseVerifier.issuerPublicKey = ISSUER_PUBLIC
        val status = LicenseVerifier.verify(SIGNED_EXPIRED, AppIdentity("com.acme.notes", ""))
        assertTrue("expected Lapsed, got $status", status is LicenseStatus.Lapsed)
        assertEquals(
            "com.acme.notes",
            (status as LicenseStatus.Lapsed).entitlement.applicationId,
        )
    }

    // ---------- shape ----------

    @Test
    fun malformedKeysAreRejectedWithAReasonThatNamesTheFix() {
        listOf(
            "",
            "   ",
            "not-a-key",
            "vk_live_",                    // no payload
            "vk_live_abc",                 // no signature separator
            "vk_staging_abc.def",          // unknown environment
            "sk_live_abc.def",             // wrong product prefix
        ).forEach { bad ->
            val error = runCatching { VoiceKit.validateLicenseShape(bad) }.exceptionOrNull()
            assertTrue(
                "expected MALFORMED for '$bad', got $error",
                error is VoiceKitException.InvalidLicense &&
                    error.reason == VoiceKitException.InvalidLicense.Reason.MALFORMED,
            )
        }
    }

    @Test
    fun preRevenueBuildsAcceptWellFormedKeysButStillEnforceBinding() {
        LicenseVerifier.issuerPublicKey = ""      // no issuer key compiled in
        assertTrue(LicenseVerifier.isPreRevenueBuild)

        // Unsigned key, minted by the tool before `keygen` was run.
        val unsigned = "vk_live_" +
            "YXBwPWNvbS5hY21lLm5vdGVzO2NlcnQ9QUJDREVGMDEyMzt0aWVyPVBSTztleHA9MTgxOTI3NDY3NjIwOQ" +
            ".unsigned"
        assertTrue(LicenseVerifier.verify(unsigned, ACME) is LicenseStatus.Active)

        // Binding is still enforced — the disabled check is the signature, not the identity.
        val error = runCatching {
            LicenseVerifier.verify(unsigned, AppIdentity("com.other.app", "ABCDEF0123"))
        }.exceptionOrNull()
        assertTrue(
            "binding must hold even without an issuer key, got $error",
            error is VoiceKitException.InvalidLicense &&
                error.reason == VoiceKitException.InvalidLicense.Reason.APP_MISMATCH,
        )
    }

    // ---------- base64url ----------

    @Test
    fun base64UrlDecodesUnpaddedAndRejectsGarbage() {
        assertEquals("app=x", String(Base64Url.decode("YXBwPXg")!!))
        assertEquals("app=x", String(Base64Url.decode("YXBwPXg=")!!))   // tolerates padding
        assertEquals(0, Base64Url.decode("")!!.size)
        assertNull("a character outside the alphabet is not decodable", Base64Url.decode("abc!def"))
        assertNull("a lone trailing sextet cannot form a byte", Base64Url.decode("A"))
    }

    @Test
    fun base64UrlAcceptsTheStandardAlphabetSoAPastedIssuerKeyVerifies() {
        // The issuer public key is emitted as standard base64 by every keytool/openssl invocation.
        val decoded = Base64Url.decode(ISSUER_PUBLIC)
        assertTrue("standard-alphabet key should decode", decoded != null && decoded.isNotEmpty())
    }
    /**
     * Test keys are deliberately unbound so a developer can evaluate without sending us their
     * signing certificate first. That is a real loosening, so it is pinned here: if a `vk_test_`
     * key ever started binding, evaluation would break; if a `vk_live_` key ever stopped binding,
     * the leak protection would be gone and nothing would fail.
     */
    @Test
    fun testKeysSkipBindingAndLiveKeysDoNot() {
        LicenseVerifier.issuerPublicKey = ""
        val payload = "YXBwPWNvbS5hY21lLm5vdGVzO2NlcnQ9QUJDREVGMDEyMzt0aWVyPVBSTztleHA9MTgxOTI3NDY3NjIwOQ"
        val stranger = AppIdentity("com.totally.different", "0000000000")

        val test = LicenseVerifier.verify("vk_test_$payload.unsigned", stranger)
        assertTrue("a test key must work in any app, got $test", test is LicenseStatus.Active)
        assertTrue((test as LicenseStatus.Active).entitlement.isTest)

        val live = runCatching {
            LicenseVerifier.verify("vk_live_$payload.unsigned", stranger)
        }.exceptionOrNull()
        assertTrue(
            "a live key must not, got $live",
            live is VoiceKitException.InvalidLicense &&
                live.reason == VoiceKitException.InvalidLicense.Reason.APP_MISMATCH,
        )
    }
}
