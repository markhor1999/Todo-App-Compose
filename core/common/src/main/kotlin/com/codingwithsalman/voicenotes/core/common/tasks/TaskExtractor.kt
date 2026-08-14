package com.codingwithsalman.voicenotes.core.common.tasks

import java.util.Locale

/** One action item lifted out of a transcript. */
data class TaskCandidate(
    /** Display text, cleaned up from the spoken sentence. */
    val text: String,
    /** Resolved deadline, or null when the sentence committed to something with no date. */
    val dueAtMs: Long?,
    /** Where in the audio it was said, so the item can seek like a transcript line. */
    val sourceStartMs: Long,
    /** The literal date words that produced [dueAtMs] ("by next Friday"), for the UI to show. */
    val matchedDate: String?,
)

/** A transcript line the extractor reads: the text and where it starts in the audio. */
data class TaskSourceLine(val text: String, val startMs: Long)

/**
 * Finds commitments in a meeting transcript — "I'll send the deck by Friday" — and turns them into
 * action items, attaching a deadline via [DeadlineParser] when the sentence carries one.
 *
 * Like [com.codingwithsalman.voicenotes.core.common.tasks.DeadlineParser] and the Key-points
 * summariser, this is a **pure algorithm: no model, no network, nothing leaves the device.** It also
 * never invents wording — every item is the speaker's own sentence, lightly cleaned.
 *
 * ### Why it is tuned to under-report
 * The failure modes are not symmetric. A missed action item costs the user a few seconds of typing
 * (the manual add field is right there). A *fabricated* action item with a *wrong* deadline fires a
 * notification at 9am about something nobody agreed to — which is worse than the feature not
 * existing. So a sentence must carry an explicit commitment cue; a bare imperative verb is not
 * enough, because ordinary meeting chatter is full of them ("check this out", "look at that").
 *
 * ### Known limits, stated plainly
 * - **English only** — the cue tables are English, as is the deadline parser. Other languages yield
 *   nothing rather than nonsense.
 * - **No assignee.** "John will send it" produces the task but cannot record that it is John's,
 *   because the transcript has no speaker labels yet. Diarization is the prerequisite, and until it
 *   lands every item belongs to the person whose phone recorded it.
 * - It reads ASR output, so it inherits its mistakes — most painfully on numbers and dates.
 */
object TaskExtractor {

    /** Hard cap: a long meeting shouldn't dump forty checkboxes into a note. */
    const val MAX_ITEMS = 8

    private const val MIN_WORDS = 4
    private const val MAX_CHARS = 140

    /**
     * Explicit commitment cues. Each is a phrase someone uses when work is being *taken on* or
     * *handed out* — not merely discussed.
     */
    private val COMMITMENT_CUES = listOf(
        "i'll", "i will", "we'll", "we will", "you'll", "you will", "he'll", "she'll", "they'll",
        "i'm going to", "we're going to", "i am going to", "we are going to",
        "need to", "needs to", "have to", "has to", "we should", "you should", "i should",
        "must ", "make sure", "don't forget", "do not forget", "remember to",
        "can you", "could you", "would you", "please ",
        "let's", "let us", "action item", "follow up", "follow-up", "take care of",
        "responsible for", "assigned to", "will handle", "will take", "will send", "will get",
        "i'll take", "on my plate", "i owe", "we owe", "to-do", "todo",
    )

    /**
     * Deadline prepositions. A date preceded by one of these is a *due date*; a bare date is often
     * just narration ("we met last Tuesday"), so context decides whether to attach it.
     */
    private val DEADLINE_CUES = listOf(
        "by", "before", "due", "deadline", "no later than", "until", "on or before", "ahead of",
    )

    /** Past-tense / hypothetical markers that disqualify a sentence outright. */
    private val DISQUALIFIERS = listOf(
        "we did", "i did", "already", "last week", "last month", "yesterday", "we had", "i had",
        "used to", "would have", "should have", "could have", "didn't", "did not", "wasn't",
    )

    /**
     * Meeting-flow phrases. "Let's" is a real commitment cue ("let's book the venue"), but the same
     * word runs the meeting itself — and *"okay let's wrap up the quarterly review"* is narration of
     * what is happening right now, not a task anyone leaves with. Caught on a real device run:
     * it was the only false positive in a five-item transcript.
     */
    private val FLOW_PHRASES = listOf(
        "wrap up", "get started", "let's begin", "let's start", "move on", "take a look",
        "let's see", "go over", "talk about", "dive in", "kick off", "circle back to that",
    )

    private val SENTENCE_SPLIT = Regex("(?<=[.!?])\\s+")
    private val WORD_SPLIT = Regex("\\s+")

    /**
     * Extract up to [MAX_ITEMS] action items from [lines].
     *
     * @param referenceMs when the audio was recorded — all relative dates resolve against this, not
     *   against the current time.
     * @param existingTexts action items the note already has; matching candidates are skipped so
     *   re-running extraction never duplicates what the user already accepted or typed.
     */
    fun extract(
        lines: List<TaskSourceLine>,
        referenceMs: Long,
        existingTexts: Collection<String> = emptyList(),
    ): List<TaskCandidate> {
        val seen = existingTexts.mapTo(HashSet()) { it.normalizedForCompare() }
        val out = ArrayList<TaskCandidate>()

        for (line in lines) {
            for (sentence in line.text.trim().split(SENTENCE_SPLIT)) {
                val clean = sentence.trim().trimEnd('.', ',', ';', ' ')
                if (clean.split(WORD_SPLIT).count { it.isNotBlank() } < MIN_WORDS) continue

                val lower = clean.lowercase(Locale.US)
                if (DISQUALIFIERS.any { it in lower }) continue
                if (COMMITMENT_CUES.none { it in lower }) continue

                val deadline = DeadlineParser.parse(clean, referenceMs)
                    ?.takeIf { it.isDueDate(lower) }

                // Flow phrases only disqualify a sentence with no real deadline: "let's go over the
                // numbers by Thursday" schedules something, while a bare "let's go over the numbers"
                // is what the meeting is doing right now. Note this deliberately reuses the same
                // is-this-really-a-deadline test as the attachment above — an earlier version asked
                // only whether a date-shaped phrase existed, which let "...on Thursday" (narration)
                // through the filter and then attached no date to it anyway.
                if (deadline == null && FLOW_PHRASES.any { it in lower }) continue

                val key = clean.normalizedForCompare()
                if (!seen.add(key)) continue

                out += TaskCandidate(
                    text = clean.take(MAX_CHARS).trim(),
                    dueAtMs = deadline?.atMs,
                    sourceStartMs = line.startMs,
                    matchedDate = deadline?.matched,
                )
                if (out.size >= MAX_ITEMS) return out.sortedForDisplay()
            }
        }
        return out.sortedForDisplay()
    }

    /**
     * True when the date reads as a deadline rather than narration. Either a deadline preposition
     * immediately precedes it ("...**by** Friday"), or the expression is inherently forward-looking
     * ("tomorrow", "end of the month") and so can't be describing the past.
     */
    private fun Deadline.isDueDate(lowerSentence: String): Boolean {
        val self = matched.lowercase(Locale.US)
        // Some parser rules capture the preposition as part of the match ("by Friday"), so the cue
        // can sit either side of range.first — check both, or "by Friday" reads as narration.
        if (DEADLINE_CUES.any { self.startsWith("$it ") }) return true
        val before = lowerSentence.take(range.first).trimEnd()
        if (DEADLINE_CUES.any { before.endsWith(it) }) return true
        return self.startsWith("tomorrow") || self.startsWith("day after") ||
            self.startsWith("next ") || self.startsWith("in ") ||
            self.startsWith("end of") || self.startsWith("today") || self.startsWith("tonight") ||
            self == "eod"
    }

    /** Dated items first (soonest first), then undated ones in the order they were spoken. */
    private fun List<TaskCandidate>.sortedForDisplay(): List<TaskCandidate> =
        sortedWith(compareBy({ it.dueAtMs == null }, { it.dueAtMs ?: Long.MAX_VALUE }, { it.sourceStartMs }))

    /** Loose key for duplicate detection: case, punctuation and spacing shouldn't matter. */
    private fun String.normalizedForCompare(): String =
        lowercase(Locale.US).replace(Regex("[^a-z0-9 ]"), "").replace(WORD_SPLIT, " ").trim()
}
