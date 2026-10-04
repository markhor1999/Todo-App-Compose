package com.tricodestudio.voicekit

// Ported from projects/murmur voicekit (C14, 2026-10-03). Keep the two copies the same until the
// Android app moves onto the multiplatform voicekit.

/**
 * Whisper, stuck on a stretch it cannot read, repeats itself: "i'n rhaid i'n rhaid i'n rhaid …" or
 * "rhaen, rhaen, rhaen, …" (both in the AMI test meetings, 2026-10-03). Nobody says a phrase four or
 * more times in a row, so such a run is cut back to one saying of it. Shorter repeats ("no, no, no")
 * are left alone: people do say those.
 */
@VoiceKitInternalApi
public object Repetition {

    /** Runs of a phrase of 1..[maxPhrase] words said [minRepeats]+ times in a row, cut back to once. */
    public fun collapse(text: String, minRepeats: Int = 4, maxPhrase: Int = 6): String {
        val spaced = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        // Scripts written without spaces (Chinese, Japanese, Thai) loop in characters instead.
        if (spaced.size <= 2 && text.trim().length >= 8) {
            return collapseTokens(text.trim().map { it.toString() }, minRepeats, maxPhrase + 2).joinToString("")
        }
        return collapseTokens(spaced, minRepeats, maxPhrase).joinToString(" ")
    }

    /** Whether [text] holds such a run. */
    public fun hasLoop(text: String, minRepeats: Int = 4, maxPhrase: Int = 6): Boolean =
        collapse(text, minRepeats, maxPhrase) != text.trim().replace(Regex("\\s+"), " ")

    private fun collapseTokens(tokens: List<String>, minRepeats: Int, maxPhrase: Int): List<String> {
        val keys = tokens.map { t -> t.lowercase().filter { it.isLetterOrDigit() || it == '\'' } }
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            var cut = false
            for (n in 1..maxPhrase) {
                if (i + n * minRepeats > tokens.size) break
                val phrase = keys.subList(i, i + n)
                if (phrase.all { it.isEmpty() }) continue
                var repeats = 1
                while (i + (repeats + 1) * n <= tokens.size && keys.subList(i + repeats * n, i + (repeats + 1) * n) == phrase) repeats++
                if (repeats >= minRepeats) {
                    out += tokens.subList(i, i + n)
                    i += repeats * n
                    cut = true
                    break
                }
            }
            if (!cut) out += tokens[i++]
        }
        return out
    }
}
