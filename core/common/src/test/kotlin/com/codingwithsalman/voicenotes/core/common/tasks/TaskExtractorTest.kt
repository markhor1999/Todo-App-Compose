package com.codingwithsalman.voicenotes.core.common.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Reference: Wednesday 12 August 2026, 14:30 local — same fixed point as the parser tests. */
class TaskExtractorTest {

    private val reference: Long = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 12, 14, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun lines(vararg text: String): List<TaskSourceLine> =
        text.mapIndexed { i, t -> TaskSourceLine(t, i * 1000L) }

    private fun extract(vararg text: String, existing: List<String> = emptyList()) =
        TaskExtractor.extract(lines(*text), reference, existing)

    @Test
    fun `commitment with a deadline becomes a dated item`() {
        val items = extract("I'll send the deck by Friday.")
        assertEquals(1, items.size)
        assertEquals("I'll send the deck by Friday", items[0].text)
        val due = Calendar.getInstance().apply { timeInMillis = items[0].dueAtMs!! }
        assertEquals(Calendar.AUGUST, due.get(Calendar.MONTH))
        assertEquals(14, due.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `commitment without a date still becomes an item`() {
        val items = extract("I'll put together the budget summary.")
        assertEquals(1, items.size)
        assertNull(items[0].dueAtMs)
    }

    @Test
    fun `ordinary discussion is not an action item`() {
        val items = extract(
            "So the revenue was up about twelve percent this quarter.",
            "That's really interesting, look at that chart.",
            "Check this out, the graph is wild.",
        )
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `past tense is disqualified even with a commitment-shaped verb`() {
        val items = extract("I already sent the deck last week.")
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `narrated past date is not attached as a deadline`() {
        // "we met on Tuesday" is narration; the sentence has a cue but the date isn't a due date.
        val items = extract("We need to discuss what we covered on Tuesday.")
        assertEquals(1, items.size)
        assertNull("Tuesday here is narration, not a deadline", items[0].dueAtMs)
    }

    @Test
    fun `forward-looking dates attach without a preposition`() {
        val items = extract("We need to ship the release tomorrow.")
        assertEquals(1, items.size)
        assertNotNull(items[0].dueAtMs)
    }

    @Test
    fun `assignment phrased at another person is captured`() {
        val items = extract("Can you review the contract before Monday?")
        assertEquals(1, items.size)
        assertNotNull(items[0].dueAtMs)
    }

    @Test
    fun `the audio anchor is carried through`() {
        val items = extract("Nothing here.", "I'll email the client tomorrow.")
        assertEquals(1, items.size)
        assertEquals(1000L, items[0].sourceStartMs)
    }

    @Test
    fun `dated items sort before undated, soonest first`() {
        val items = extract(
            "I'll draft the proposal.",
            "We need to send the invoice by next Friday.",
            "Please book the room tomorrow.",
        )
        assertEquals(3, items.size)
        assertNotNull(items[0].dueAtMs)
        assertNotNull(items[1].dueAtMs)
        assertNull(items[2].dueAtMs)
        assertTrue("soonest first", items[0].dueAtMs!! < items[1].dueAtMs!!)
    }

    @Test
    fun `existing items are never re-suggested`() {
        val items = extract(
            "I'll send the deck by Friday.",
            existing = listOf("I'll send the deck by Friday"),
        )
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `duplicate detection ignores case and punctuation`() {
        val items = extract(
            "I'll send the deck by Friday.",
            existing = listOf("i'll SEND the deck, by friday!!"),
        )
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `the same sentence twice yields one item`() {
        val items = extract("I'll send the deck by Friday.", "I'll send the deck by Friday.")
        assertEquals(1, items.size)
    }

    @Test
    fun `very short fragments are ignored`() {
        val items = extract("I'll do it.", "Let's go.")
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `output is capped`() {
        val many = (1..20).map { "I'll finish task number $it for the team." }.toTypedArray()
        assertEquals(TaskExtractor.MAX_ITEMS, extract(*many).size)
    }

    @Test
    fun `multiple sentences in one transcript line are split`() {
        val items = extract("Great work everyone. I'll send the notes tomorrow. Thanks all.")
        assertEquals(1, items.size)
        assertEquals("I'll send the notes tomorrow", items[0].text)
    }

    @Test
    fun `non-english text yields nothing rather than nonsense`() {
        val items = extract("Ich schicke dir die Unterlagen bis Freitag.")
        assertTrue("expected nothing, got $items", items.isEmpty())
    }

    @Test
    fun `matched date text is reported for display`() {
        val items = extract("I'll send the deck by next Friday.")
        assertEquals(1, items.size)
        assertTrue(items[0].matchedDate!!.equals("next Friday", ignoreCase = true))
    }

    @Test
    fun `empty input is safe`() {
        assertTrue(TaskExtractor.extract(emptyList(), reference).isEmpty())
    }
}
