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
        assertTrue(VoiceModel.MULTILINGUAL_FAST.isMultilingual)
        assertTrue(VoiceModel.MULTILINGUAL_FAST.supports("ur"))

        assertTrue(!VoiceModel.ENGLISH_FAST.isMultilingual)
        assertTrue(VoiceModel.ENGLISH_FAST.supports("EN"))
        assertTrue(!VoiceModel.ENGLISH_FAST.supports("ur"))
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
}
