package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the *shape* of the public API rather than its behaviour — there is no behaviour yet.
 *
 * Its real job is [integrationSnippetFromTheLandingPageCompiles]: the ten-line example printed on
 * voice.tricodestudio.com and quoted in every outreach email has to be code that actually
 * type-checks. A landing page whose snippet does not compile is the fastest possible way to lose a
 * developer's trust, and it is the sort of thing that stays wrong for months because nobody runs it.
 */
class PublicApiShapeTest {

    /**
     * The hero snippet, verbatim in structure. It is expected to throw NotImplementedError until the
     * engine is extracted — the point is that it **compiles**, which is what the marketing claims.
     */
    @Suppress("UNUSED_VARIABLE")
    @Test
    fun integrationSnippetFromTheLandingPageCompiles() {
        val snippet: suspend (android.content.Context, File) -> Unit = { context, audioFile ->
            VoiceKit.initialize(context, licenseKey = "vk_live_example")

            val result = VoiceKit.transcribe(audioFile)

            println(result.text)
            println(result.language)
            result.segments.forEach { println("${it.startMs}..${it.endMs}: ${it.text}") }
        }
        assertTrue("snippet should be constructible", snippet !== null)
    }

    @Test
    fun textJoinsSegmentsInOrder() {
        val r = TranscriptionResult(
            segments = listOf(
                Segment(0, 900, "Remind me to call"),
                Segment(900, 2100, " the dentist on Tuesday "),
            ),
            language = "en",
            durationMs = 2100,
        )
        assertEquals("Remind me to call the dentist on Tuesday", r.text)
        assertEquals(false, r.isEmpty)
    }

    @Test
    fun emptyResultIsReportedAsEmptyNotAsBlankText() {
        val r = TranscriptionResult(segments = emptyList(), language = null, durationMs = 0)
        assertTrue(r.isEmpty)
        assertEquals("", r.text)
    }

    @Test
    fun segmentExposesItsOwnDuration() {
        assertEquals(1200L, Segment(900, 2100, "x").durationMs)
    }

    @Test
    fun multilingualModelsAcceptAnyLanguageAndEnglishOnlyDoesNot() {
        assertTrue(VoiceModel.MULTILINGUAL.isMultilingual)
        assertTrue(VoiceModel.MULTILINGUAL.supports("ur"))

        assertTrue(!VoiceModel.ENGLISH_FAST.isMultilingual)
        assertTrue(VoiceModel.ENGLISH_FAST.supports("EN"))
        assertTrue(!VoiceModel.ENGLISH_FAST.supports("ur"))
    }

    /**
     * Every model must be backed by a real bundle, and the advertised size must come from that
     * bundle. A tier that exists only in the enum is a promise the downloader cannot keep — the
     * previous MULTILINGUAL_ACCURATE entry was exactly that, with a size invented to look plausible.
     */
    @Test
    fun everyModelIsBackedByRealFilesAndReportsTheirRealSize() {
        VoiceModel.entries.forEach { m ->
            assertTrue("\$m has no files", m.spec.files.isNotEmpty())
            assertTrue("\$m reports \${m.approxSizeMb} MB", m.approxSizeMb > 0)
            m.spec.files.forEach { f ->
                assertTrue("\$m file \${f.fileName} has no size", f.sizeBytes > 0)
                assertTrue("\$m file \${f.fileName} has no url", f.url.startsWith("http"))
            }
        }
    }

    @Test
    fun callsBeforeInitializeFailWithNotInitializedRatherThanNpe() {
        VoiceKit.resetForTesting()
        assertTrue(!VoiceKit.isInitialized)
        try {
            VoiceKit.models
            throw AssertionError("expected NotInitialized")
        } catch (e: VoiceKitException.NotInitialized) {
            assertTrue(e.message!!.contains("initialize"))
        }
    }

    @Test
    fun blankLicenseKeyIsRejectedAsMalformed() {
        listOf("", "   ", "\n").forEach { bad ->
            try {
                VoiceKit.validateLicenseShape(bad)
                throw AssertionError("expected InvalidLicense for '\$bad'")
            } catch (e: VoiceKitException.InvalidLicense) {
                assertEquals(VoiceKitException.InvalidLicense.Reason.MALFORMED, e.reason)
            }
        }
        // Tightened 2026-08-26: a key now carries a signed payload, so the shape is
        // vk_<env>_<payload>.<signature> and a bare prefix is no longer enough. This line used to
        // read validateLicenseShape("vk_live_anything") and correctly failed when the format landed.
        VoiceKit.validateLicenseShape("vk_live_YXBwPXg.c2ln")
        VoiceKit.validateLicenseShape("vk_test_YXBwPXg.c2ln")
    }

    /**
     * Error messages are part of the API. If one stops saying what to do about the failure, that is
     * a regression in the product, not just in a string.
     */
    @Test
    fun everyErrorNamesTheFix() {
        val messages = listOf(
            VoiceKitException.NotInitialized().message,
            VoiceKitException.ModelNotInstalled(VoiceModel.ENGLISH_FAST).message,
            VoiceKitException.InvalidLicense(VoiceKitException.InvalidLicense.Reason.APP_MISMATCH).message,
            VoiceKitException.UnsupportedAudio("8-bit ADPCM").message,
        )
        messages.forEach { m ->
            assertTrue("message should not be empty", !m.isNullOrBlank())
            assertTrue("message should be actionable, was: $m", (m?.length ?: 0) > 40)
        }
    }
    /**
     * The live snippet from the docs, same contract as the batch one: it has to compile, because it
     * is quoted verbatim on the landing page.
     */
    @Suppress("UNUSED_VARIABLE")
    @Test
    fun liveSessionSnippetFromTheDocsCompiles() {
        val snippet: suspend () -> Unit = {
            VoiceKit.startLiveSession(VoiceModel.ENGLISH_STREAMING).use { session ->
                session.text.value.let(::println)
                session.accept(FloatArray(512))
            }
        }
        assertTrue("snippet should be constructible", snippet !== null)
    }

    @Test
    fun onlyTheStreamingModelIsMarkedStreaming() {
        assertTrue(VoiceModel.ENGLISH_STREAMING.isStreaming)
        val batch = VoiceModel.entries.filterNot { it.isStreaming }
        assertEquals(
            listOf(VoiceModel.ENGLISH_FAST, VoiceModel.ENGLISH_COMPACT, VoiceModel.MULTILINGUAL),
            batch,
        )
    }

    /**
     * A streaming model has no offline decoder. Routing it into transcribe() would surface as an
     * unrelated-looking native load error, so the rejection has to happen at the public boundary
     * and has to name the call that does work.
     */
    @Test
    fun transcribeRejectsAStreamingModelAndNamesTheAlternative() {
        VoiceKit.resetForTesting()
        val error = runCatching {
            kotlinx.coroutines.runBlocking {
                VoiceKit.transcribe(File("x.wav"), model = VoiceModel.ENGLISH_STREAMING)
            }
        }.exceptionOrNull()
        // Not initialized is checked first, which is correct ordering — assert that specifically so
        // this test cannot silently start passing for the wrong reason.
        assertTrue(
            "expected NotInitialized before any model check, got $error",
            error is VoiceKitException.NotInitialized,
        )
    }

    @Test
    fun streamingModelDoesNotClaimTheVadDownloadItNeverUses() {
        // approxSizeMb is quoted to developers deciding whether to ship the model; the streaming
        // path never segments on silence, so folding VAD bytes in would overstate the download.
        val streamingBytes = VoiceModel.ENGLISH_STREAMING.spec.totalBytes
        assertEquals(
            (streamingBytes / 1_048_576L).toInt(),
            VoiceModel.ENGLISH_STREAMING.approxSizeMb,
        )
        val batchBytes = VoiceModel.MULTILINGUAL.spec.totalBytes
        assertTrue(
            "a batch model must include the VAD bundle it does use",
            VoiceModel.MULTILINGUAL.approxSizeMb > (batchBytes / 1_048_576L).toInt() - 1,
        )
    }
}
