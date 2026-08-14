package com.codingwithsalman.voicenotes.core.common.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * All cases resolve against a fixed reference so they don't drift: **Wednesday 12 August 2026,
 * 14:30 local.** Assertions are on calendar fields rather than raw millis so a machine in any
 * timezone gets the same answer.
 */
class DeadlineParserTest {

    private val reference: Long = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 12, 14, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun parse(text: String): Calendar? =
        DeadlineParser.parse(text, reference)?.let { d ->
            Calendar.getInstance().apply { timeInMillis = d.atMs }
        }

    private fun assertDate(text: String, year: Int, month: Int, day: Int) {
        val c = requireNotNull(parse(text)) { "expected a date in: $text" }
        assertEquals("year in \"$text\"", year, c.get(Calendar.YEAR))
        assertEquals("month in \"$text\"", month, c.get(Calendar.MONTH))
        assertEquals("day in \"$text\"", day, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `relative day expressions`() {
        assertDate("send it today", 2026, Calendar.AUGUST, 12)
        assertDate("I'll do it tonight", 2026, Calendar.AUGUST, 12)
        assertDate("get it to me tomorrow", 2026, Calendar.AUGUST, 13)
        assertDate("the day after tomorrow works", 2026, Calendar.AUGUST, 14)
    }

    @Test
    fun `day after tomorrow is not swallowed by tomorrow`() {
        // Rule ordering regression: the generic \btomorrow\b rule must not win here.
        assertDate("day after tomorrow", 2026, Calendar.AUGUST, 14)
    }

    @Test
    fun `in N days weeks and months, digits and words`() {
        assertDate("in 3 days", 2026, Calendar.AUGUST, 15)
        assertDate("in three days", 2026, Calendar.AUGUST, 15)
        assertDate("in 2 weeks", 2026, Calendar.AUGUST, 26)
        assertDate("in a couple weeks", 2026, Calendar.AUGUST, 26)
        assertDate("in 1 month", 2026, Calendar.SEPTEMBER, 12)
    }

    @Test
    fun `next weekday is the following week, not the upcoming one`() {
        // Reference is Wednesday Aug 12. The upcoming Friday is Aug 14; "next Friday" is Aug 21.
        assertDate("next Friday", 2026, Calendar.AUGUST, 21)
        assertDate("by Friday", 2026, Calendar.AUGUST, 14)
    }

    @Test
    fun `bare weekday is always in the future`() {
        // Said on a Wednesday, "Wednesday" means the next one — not today.
        assertDate("let's review Wednesday", 2026, Calendar.AUGUST, 19)
        assertDate("ship it Monday", 2026, Calendar.AUGUST, 17)
    }

    @Test
    fun `end of week and end of month`() {
        assertDate("end of the week", 2026, Calendar.AUGUST, 14)   // upcoming Friday
        assertDate("end of month", 2026, Calendar.AUGUST, 31)
        assertDate("next week", 2026, Calendar.AUGUST, 19)
    }

    @Test
    fun `explicit calendar dates in both orders`() {
        assertDate("by September 3rd", 2026, Calendar.SEPTEMBER, 3)
        assertDate("on 3 September", 2026, Calendar.SEPTEMBER, 3)
        assertDate("the 15th of December", 2026, Calendar.DECEMBER, 15)
        assertDate("Sept 9", 2026, Calendar.SEPTEMBER, 9)
    }

    @Test
    fun `a calendar date already past rolls to next year`() {
        // Reference is August 2026, so "March 1" must mean 2027.
        assertDate("by March 1st", 2027, Calendar.MARCH, 1)
    }

    @Test
    fun `bare day of month rolls into next month when passed`() {
        assertDate("the 20th", 2026, Calendar.AUGUST, 20)
        assertDate("the 5th", 2026, Calendar.SEPTEMBER, 5)
    }

    @Test
    fun `impossible dates are rejected rather than silently rolled`() {
        assertNull(parse("February 30th"))
        assertNull(parse("the 45th"))
    }

    @Test
    fun `sentences with no date produce nothing`() {
        assertNull(parse("I'll send the deck over"))
        assertNull(parse("we should talk about the budget"))
        assertNull(parse(""))
    }

    @Test
    fun `deadlines land at 9am with zeroed seconds`() {
        val c = requireNotNull(parse("tomorrow"))
        assertEquals(DeadlineParser.DEFAULT_HOUR, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(0, c.get(Calendar.SECOND))
        assertEquals(0, c.get(Calendar.MILLISECOND))
    }

    @Test
    fun `resolution is relative to the recording, not to now`() {
        // A note recorded a year ago saying "tomorrow" resolves near that recording, not near today.
        val lastYear = Calendar.getInstance().apply {
            set(2025, Calendar.MARCH, 10, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val d = requireNotNull(DeadlineParser.parse("finish it tomorrow", lastYear))
        val c = Calendar.getInstance().apply { timeInMillis = d.atMs }
        assertEquals(2025, c.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, c.get(Calendar.MONTH))
        assertEquals(11, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `matched text is reported for the UI`() {
        val d = requireNotNull(DeadlineParser.parse("Please send it by next Friday", reference))
        assertTrue("matched was '${d.matched}'", d.matched.equals("next Friday", ignoreCase = true))
    }

    @Test
    fun `case is ignored`() {
        assertDate("BY NEXT FRIDAY", 2026, Calendar.AUGUST, 21)
        assertDate("ToMoRrOw", 2026, Calendar.AUGUST, 13)
    }
}
