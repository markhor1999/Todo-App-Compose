@file:OptIn(VoiceKitInternalApi::class)

package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression cover for the 2026-09-01 crash cluster: 19 Play issues, 22.2% of distinct users, one
 * cause — the offline recognizer was built unconditionally on hardware that could not hold it.
 * See brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md.
 */
class DeviceCapabilitiesTest {

    private fun caps(
        supportsNativeAsr: Boolean = true,
        isLowRam: Boolean = false,
        totalMemBytes: Long = 8L * 1000 * 1000 * 1000,
    ) = DeviceCapabilities(supportsNativeAsr, isLowRam, totalMemBytes)

    // --- Family A: the armeabi-v7a SIGBUS population -------------------------------------------

    @Test
    fun `a device without a 64-bit ABI fits no model at all`() {
        val c = caps(supportsNativeAsr = false)
        // Even the smallest bundle is refused: the failure mode there is a native SIGBUS inside
        // OfflineRecognizer_newFromFile, which no try/catch can contain.
        assertFalse(c.fits(ModelCatalog.whisperTinyEn))
        assertFalse(c.fits(ModelCatalog.whisperBaseMultilingual))
        assertFalse(c.fits(ModelCatalog.liveEnStreaming))
    }

    // --- Family B: the low-RAM OutOfMemoryError population --------------------------------------

    @Test
    fun `a low-RAM device keeps the tiny model and drops the multilingual one`() {
        val c = caps(isLowRam = true, totalMemBytes = 2L * 1000 * 1000 * 1000)
        assertTrue("whisper-tiny (~104 MB) must stay usable", c.fits(ModelCatalog.whisperTinyEn))
        assertFalse(
            "whisper-base (~160 MB) is the one that costs ~1 GB of native heap",
            c.fits(ModelCatalog.whisperBaseMultilingual),
        )
    }

    @Test
    fun `a low-RAM device still has a MULTILINGUAL option`() {
        // MUR-17. Before whisper-tiny-multilingual existed, capping low-RAM devices at 120 MB left
        // anyone who did not speak English with no working model at all — the gate stopped the
        // crash by removing the feature. This is the test that says that is no longer true.
        val c = caps(isLowRam = true, totalMemBytes = 2L * 1000 * 1000 * 1000)
        assertTrue(
            "the compact multilingual bundle must clear the low-RAM ceiling",
            c.fits(ModelCatalog.whisperTinyMultilingual),
        )
        assertTrue(
            "and it must actually be multilingual",
            ModelCatalog.whisperTinyMultilingual.languages.isEmpty(),
        )
        assertTrue(
            "the picker must offer at least one multilingual model on low-RAM hardware",
            ModelCatalog.all.any { c.fits(it) && it.languages.isEmpty() },
        )
    }

    @Test
    fun `lowRamDefault is not larger than the model low-RAM devices are refused`() {
        // It used to name Moonshine (~287 MB), which is bigger than the bundle we exclude for
        // being too big. That was a live trap for the next person to wire it up.
        assertTrue(
            ModelCatalog.lowRamDefault.approxSizeMb <=
                ModelCatalog.whisperBaseMultilingual.approxSizeMb,
        )
        assertTrue(caps(isLowRam = true).fits(ModelCatalog.lowRamDefault))
        assertTrue(caps(isLowRam = true).fits(ModelCatalog.lowRamMultilingual))
    }

    @Test
    fun `a normal device fits every catalogued model`() {
        val c = caps()
        assertTrue(ModelCatalog.all.all(c::fits))
    }

    @Test
    fun `low-RAM halves the inference thread count`() {
        // ONNX Runtime allocates a per-thread arena, so threads multiply peak native memory.
        assertTrue(caps(isLowRam = true).asrThreadCount <= 2)
        assertTrue(caps(isLowRam = false).asrThreadCount >= 2)
        assertTrue(caps(isLowRam = true).asrThreadCount < caps(isLowRam = false).asrThreadCount ||
            Runtime.getRuntime().availableProcessors() <= 2)
    }

    @Test
    fun `the model ceiling only applies to low-RAM devices`() {
        assertEquals(DeviceCapabilities.LOW_RAM_MODEL_LIMIT_MB, caps(isLowRam = true).maxOfflineModelMb)
        assertEquals(Int.MAX_VALUE, caps(isLowRam = false).maxOfflineModelMb)
    }

    @Test
    fun `the low-RAM ceiling sits between the tiny and multilingual bundles`() {
        // If either model is ever resized this test is the thing that should fail, not a phone.
        assertTrue(
            "tiny must sit under the ceiling",
            ModelCatalog.whisperTinyEn.approxSizeMb < DeviceCapabilities.LOW_RAM_MODEL_LIMIT_MB,
        )
        assertTrue(
            "base must sit above it",
            ModelCatalog.whisperBaseMultilingual.approxSizeMb > DeviceCapabilities.LOW_RAM_MODEL_LIMIT_MB,
        )
    }

    @Test
    fun `UNRESTRICTED is permissive so JVM callers are unaffected`() {
        assertTrue(DeviceCapabilities.UNRESTRICTED.supportsNativeAsr)
        assertFalse(DeviceCapabilities.UNRESTRICTED.isLowRam)
        assertTrue(ModelCatalog.all.all(DeviceCapabilities.UNRESTRICTED::fits))
    }

    // --- what callers are told ------------------------------------------------------------------

    @Test
    fun `refusalReason names the actual blocker`() {
        assertTrue(
            caps(supportsNativeAsr = false)
                .refusalReason(ModelCatalog.whisperTinyEn)!!.contains("64-bit"),
        )
        assertTrue(
            caps(isLowRam = true)
                .refusalReason(ModelCatalog.whisperBaseMultilingual)!!.contains("exceeds"),
        )
        assertNull(
            "a model that fits has no blocker to report",
            caps(isLowRam = true).refusalReason(ModelCatalog.whisperTinyEn),
        )
    }

    @Test
    fun `the user-facing message is prose, not a log line`() {
        val noAbi = caps(supportsNativeAsr = false).refusalMessage(ModelCatalog.whisperTinyEn)!!
        // SettingsScreen renders this verbatim, so it must not leak identifiers or byte counts.
        assertFalse(noAbi.contains("ABI"))
        assertFalse(noAbi.contains("whisper"))
        assertTrue("must tell them what still works", noAbi.contains("Recording"))

        val tooBig = caps(isLowRam = true).refusalMessage(ModelCatalog.whisperBaseMultilingual)!!
        assertFalse(tooBig.contains("MB limit"))
        assertNull(caps().refusalMessage(ModelCatalog.whisperTinyEn))
    }

    @Test
    fun `no 64-bit ABI outranks the size check`() {
        // A 32-bit device must be refused for the reason that actually kills it, not for size.
        val reason = caps(supportsNativeAsr = false, isLowRam = true)
            .refusalReason(ModelCatalog.whisperTinyEn)
        assertTrue(reason!!.contains("64-bit"))
    }
}
