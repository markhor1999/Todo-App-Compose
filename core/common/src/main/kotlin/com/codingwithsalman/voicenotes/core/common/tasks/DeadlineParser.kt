package com.codingwithsalman.voicenotes.core.common.tasks

import java.util.Calendar
import java.util.Locale

/**
 * A date found inside a sentence.
 *
 * @param atMs resolved deadline, epoch millis, at [DeadlineParser.DEFAULT_HOUR] local time.
 * @param matched the literal text that produced it ("next Friday"), for showing the user *why* a
 *   date was attached — an extracted deadline the user can't trace is one they can't trust.
 * @param range where [matched] sits in the source sentence, so callers can inspect what precedes it.
 */
data class Deadline(val atMs: Long, val matched: String, val range: IntRange)

/**
 * Resolves English date expressions in speech to a concrete instant, relative to when the audio was
 * recorded (never to "now" — a note transcribed on Monday about "tomorrow" said on Friday means
 * Saturday).
 *
 * **Deliberately English-only.** A temporal parser is inherently language-specific and Murmur ships
 * in 9 locales; the honest options were English-first or nothing this release, and a wrong deadline
 * is worse than no deadline. Non-English transcripts simply yield no dates, which degrades to
 * "action item without a deadline" — still useful, never wrong. Adding a locale means adding a
 * table here plus tests, not restructuring.
 *
 * **Tuned for precision over recall.** It would rather miss a deadline than invent one, because the
 * cost is asymmetric: a missed date is a mild disappointment, an invented reminder at 9am on the
 * wrong day is a reason to uninstall.
 *
 * Uses [Calendar] rather than `java.time` because minSdk is 24 and core-library desugaring is not
 * enabled — the same reason the rest of the codebase does.
 */
object DeadlineParser {

    /** Deadlines land at 9am local: a morning nudge on the due day, not midnight. */
    const val DEFAULT_HOUR = 9

    private val WEEKDAYS = mapOf(
        "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "saturday" to Calendar.SATURDAY,
        "sunday" to Calendar.SUNDAY,
    )

    private val MONTHS = mapOf(
        "january" to 0, "february" to 1, "march" to 2, "april" to 3, "may" to 4, "june" to 5,
        "july" to 6, "august" to 7, "september" to 8, "october" to 9, "november" to 10,
        "december" to 11,
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3, "jun" to 5, "jul" to 6, "aug" to 7,
        "sep" to 8, "sept" to 8, "oct" to 9, "nov" to 10, "dec" to 11,
    )

    private val NUMBER_WORDS = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7,
        "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12, "a" to 1, "an" to 1,
        "couple" to 2, "few" to 3,
    )

    private val WEEKDAY_ALT = WEEKDAYS.keys.joinToString("|")
    private val MONTH_ALT = MONTHS.keys.sortedByDescending { it.length }.joinToString("|")
    private val NUM_ALT = NUMBER_WORDS.keys.joinToString("|")

    /**
     * Ordered most-specific first. The first rule that matches wins, so "next Friday" is never
     * consumed by the bare-weekday rule and "day after tomorrow" never by "tomorrow".
     */
    private val RULES: List<Pair<Regex, (MatchResult, Calendar) -> Calendar?>> = listOf(
        Regex("\\bday after tomorrow\\b") to { _, c -> c.plusDays(2) },
        Regex("\\btomorrow\\b") to { _, c -> c.plusDays(1) },
        Regex("\\b(today|tonight|end of (the )?day|eod)\\b") to { _, c -> c },

        Regex("\\bend of (the )?week\\b") to { _, c -> c.nextOrSame(Calendar.FRIDAY) },
        Regex("\\bend of (the )?month\\b") to { _, c ->
            c.apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
        },
        Regex("\\bnext week\\b") to { _, c -> c.plusDays(7) },
        Regex("\\bnext month\\b") to { _, c -> c.apply { add(Calendar.MONTH, 1) } },

        // The optional article covers "in a couple of weeks" / "in a few days", where the count
        // word is preceded by one ("a couple" is two tokens, not one).
        Regex("\\bin (?:an? )?(\\d+|$NUM_ALT) (?:of )?(day|days)\\b") to { m, c -> c.plusDays(m.count()) },
        Regex("\\bin (?:an? )?(\\d+|$NUM_ALT) (?:of )?(week|weeks)\\b") to { m, c -> c.plusDays(m.count() * 7) },
        Regex("\\bin (?:an? )?(\\d+|$NUM_ALT) (?:of )?(month|months)\\b") to { m, c ->
            c.apply { add(Calendar.MONTH, m.count()) }
        },

        // "next Friday" = the Friday of the following week, not the upcoming one.
        Regex("\\bnext ($WEEKDAY_ALT)\\b") to { m, c ->
            c.nextOrSame(WEEKDAYS.getValue(m.groupValues[1])).plusDays(7)
        },
        Regex("\\b(this|on|by|before) ($WEEKDAY_ALT)\\b") to { m, c ->
            c.strictlyNext(WEEKDAYS.getValue(m.groupValues[2]))
        },
        Regex("\\b($WEEKDAY_ALT)\\b") to { m, c ->
            c.strictlyNext(WEEKDAYS.getValue(m.groupValues[1]))
        },

        // "March 15" / "March 15th"
        Regex("\\b($MONTH_ALT)\\.? (\\d{1,2})(st|nd|rd|th)?\\b") to { m, c ->
            c.onMonthDay(MONTHS.getValue(m.groupValues[1]), m.groupValues[2].toInt())
        },
        // "15 March" / "15th of March"
        Regex("\\b(\\d{1,2})(st|nd|rd|th)? (of )?($MONTH_ALT)\\b") to { m, c ->
            c.onMonthDay(MONTHS.getValue(m.groupValues[4]), m.groupValues[1].toInt())
        },
        // "the 15th" — day-of-month alone, next occurrence.
        Regex("\\bthe (\\d{1,2})(st|nd|rd|th)\\b") to { m, c -> c.onDayOfMonth(m.groupValues[1].toInt()) },
    )

    /**
     * The first date expression in [sentence], resolved against [referenceMs], or null if there
     * isn't one. [sentence] may be any case; matching is lowercase.
     */
    fun parse(sentence: String, referenceMs: Long): Deadline? {
        val lower = sentence.lowercase(Locale.US)
        for ((regex, resolve) in RULES) {
            val match = regex.find(lower) ?: continue
            val base = Calendar.getInstance().apply {
                timeInMillis = referenceMs
                set(Calendar.HOUR_OF_DAY, DEFAULT_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val resolved = resolve(match, base) ?: continue
            return Deadline(
                atMs = resolved.timeInMillis,
                matched = sentence.substring(match.range.first, match.range.last + 1),
                range = match.range,
            )
        }
        return null
    }

    // ---------------------------------------------------------------- Calendar helpers

    private fun MatchResult.count(): Int {
        val raw = groupValues[1]
        return raw.toIntOrNull() ?: NUMBER_WORDS[raw] ?: 1
    }

    private fun Calendar.plusDays(days: Int): Calendar = apply { add(Calendar.DAY_OF_YEAR, days) }

    /** Today if it already is that weekday, else the next one. */
    private fun Calendar.nextOrSame(weekday: Int): Calendar = apply {
        while (get(Calendar.DAY_OF_WEEK) != weekday) add(Calendar.DAY_OF_YEAR, 1)
    }

    /** Always in the future: "see you Monday" said on a Monday means the *next* Monday. */
    private fun Calendar.strictlyNext(weekday: Int): Calendar = apply {
        do { add(Calendar.DAY_OF_YEAR, 1) } while (get(Calendar.DAY_OF_WEEK) != weekday)
    }

    /** A month/day this year, rolling to next year if it has already passed. */
    private fun Calendar.onMonthDay(month: Int, day: Int): Calendar? = apply {
        val reference = timeInMillis
        set(Calendar.MONTH, month)
        if (day > getActualMaximum(Calendar.DAY_OF_MONTH)) return null
        set(Calendar.DAY_OF_MONTH, day)
        if (timeInMillis < reference) add(Calendar.YEAR, 1)
    }

    /** A day-of-month, this month or next if it has already passed. */
    private fun Calendar.onDayOfMonth(day: Int): Calendar? = apply {
        val reference = timeInMillis
        if (day > getActualMaximum(Calendar.DAY_OF_MONTH)) return null
        set(Calendar.DAY_OF_MONTH, day)
        if (timeInMillis < reference) {
            add(Calendar.MONTH, 1)
            if (day > getActualMaximum(Calendar.DAY_OF_MONTH)) return null
            set(Calendar.DAY_OF_MONTH, day)
        }
    }
}
