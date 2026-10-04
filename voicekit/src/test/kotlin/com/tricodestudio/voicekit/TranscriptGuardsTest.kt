package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The same cases as projects/murmur's TranscriptGuardsTest (Repetition only). */
@OptIn(VoiceKitInternalApi::class)
class TranscriptGuardsTest {

    @Test
    fun whispersLoopsFromTheAmiMeetingsAreCutBackToOneSaying() {
        // ES2004b, 2:03 and ES2004a, 16:12 (2026-10-03), as Whisper wrote them.
        assertEquals("i'n rhaid i", Repetition.collapse("i'n rhaid i'n rhaid i'n rhaid i'n rhaid i'n rhaid i'n rhaid i'n rhaid i"))
        assertEquals("Rhaen, ok, rhaen, rha", Repetition.collapse("Rhaen, ok, rhaen, rhaen, rhaen, rhaen, rhaen, rhaen, rhaen, rha"))
    }

    @Test
    fun aLoopInsideRealSpeechKeepsTheSpeechAroundIt() {
        val text = "so if there's a program on you want to record it you press it you press it you press it you press it and that's fine"
        assertEquals("so if there's a program on you want to record it you press it and that's fine", Repetition.collapse(text))
    }

    @Test
    fun thingsPeopleReallyRepeatAreLeftAlone() {
        for (said in listOf("No, no, no, that's not it.", "Okay okay okay.", "user user interface", "very very very good", "We have a, we have a plan.")) {
            assertEquals(said, said, Repetition.collapse(said))
            assertFalse(said, Repetition.hasLoop(said))
        }
    }

    @Test
    fun punctuationAndCaseDoNotHideALoop() {
        assertTrue(Repetition.hasLoop("Yes. yes, YES! yes yes"))
        assertEquals("Yes.", Repetition.collapse("Yes. yes, YES! yes yes"))
    }

    @Test
    fun scriptsWithoutSpacesLoopInCharacters() {
        assertEquals("好的我们", Repetition.collapse("好的我们我们我们我们我们我们"))
        assertEquals("今天的会议很重要", Repetition.collapse("今天的会议很重要"))
    }
}
