package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The download-gating seam. Build plan §4 calls this the highest-leverage control — a licence check
 * inside an AAR can be stripped, but re-hosting 274 MB of weights is a different kind of effort.
 */
class ModelSourceTest {

    private val spec = VoiceModel.MULTILINGUAL.spec
    private val file = spec.files.first()

    @Test
    fun theDirectSourceSendsNoCredentialsAndUsesTheCatalogUrl() {
        val resolved = DirectModelSource.resolve(spec, file)
        assertEquals(file.url, resolved.url)
        assertTrue("public mirrors must not receive a licence key", resolved.headers.isEmpty())
    }

    @Test
    fun theGatedSourceAddressesOurEndpointAndCarriesTheKey() {
        val resolved = GatedModelSource("vk_live_abc.def", "https://voice.tricodestudio.com")
            .resolve(spec, file)
        assertEquals(
            "https://voice.tricodestudio.com/v1/models/${spec.id}/${file.fileName}",
            resolved.url,
        )
        assertEquals("Bearer vk_live_abc.def", resolved.headers["Authorization"])
        assertEquals(VoiceKitBuild.VERSION, resolved.headers["X-VoiceKit-Version"])
    }

    /**
     * A trailing slash in a developer-supplied base URL is the single most common way an endpoint
     * config produces `//v1/models/...`, which some proxies normalise and some 404 on.
     */
    @Test
    fun aTrailingSlashInTheEndpointDoesNotProduceADoubleSlash() {
        VoiceKit.resetForTesting()
        val resolved = GatedModelSource("vk_live_abc.def", "https://voice.tricodestudio.com/".trimEnd('/'))
            .resolve(spec, file)
        assertTrue("no double slash in ${resolved.url}", !resolved.url.contains("dev//"))
    }

    /**
     * The gated URL must not leak the key into the path or query — it goes in a header, so it stays
     * out of proxy logs, CDN access logs and crash reports.
     */
    @Test
    fun theLicenceKeyNeverAppearsInTheUrl() {
        val key = "vk_live_secretpayload.secretsig"
        val resolved = GatedModelSource(key, "https://voice.tricodestudio.com").resolve(spec, file)
        assertTrue("key leaked into ${resolved.url}", !resolved.url.contains("secret"))
    }

    /**
     * Every catalog entry is currently unpinned, and that is deliberate — the digests of the public
     * mirrors have not been verified, and asserting a hash nobody checked is worse than asserting
     * none. This test states the current position so filling them in is a visible decision.
     */
    @Test
    fun catalogDigestsAreUnpinnedUntilModelsMoveToOurOwnDistribution() {
        val pinned = VoiceModel.entries.flatMap { it.spec.files }.count { it.sha256 != null }
        assertEquals(
            "if a digest was pinned, update this test and LICENSING.md's status section",
            0,
            pinned,
        )
    }
}
