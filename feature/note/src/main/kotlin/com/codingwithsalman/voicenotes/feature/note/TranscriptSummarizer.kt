package com.codingwithsalman.voicenotes.feature.note

import com.codingwithsalman.voicenotes.core.model.TranscriptSegment
import kotlin.math.ln

/** One extracted key line + the audio position to seek to when it's tapped. */
data class KeyPoint(val text: String, val startMs: Long)

/**
 * Extractive summariser — a TextRank-style ranking over the transcript's own sentences. It is a
 * pure algorithm (no model, no network), so it keeps Murmur's privacy claim literal and runs
 * instantly offline. It never invents words: every "key point" is a sentence lifted verbatim from
 * the transcript, anchored to the timestamp of the segment it came from.
 */
object TranscriptSummarizer {

    // A small English stop-word set. For other languages the overlap ranking still works; it just
    // can't discount their function words — an acceptable v1 trade for staying dependency-free.
    private val STOP = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then", "so", "of", "to", "in", "on", "for",
        "with", "at", "by", "from", "up", "down", "is", "are", "was", "were", "be", "been", "being",
        "it", "its", "this", "that", "these", "those", "i", "you", "he", "she", "we", "they", "me",
        "him", "her", "us", "them", "my", "your", "our", "their", "as", "just", "like", "okay", "ok",
        "yeah", "um", "uh", "so", "do", "does", "did", "have", "has", "had", "not", "no", "yes",
        "can", "will", "would", "should", "could", "there", "here", "what", "when", "which", "who",
        "how", "about", "into", "over", "than", "too", "very", "really", "gonna", "wanna",
    )

    private val SENTENCE_SPLIT = Regex("(?<=[.!?。！？])\\s+")
    private val TOKENS = Regex("[^\\p{L}\\p{N}]+")

    private data class Sentence(val text: String, val startMs: Long, val order: Int)

    /**
     * Up to [max] key sentences, returned in chronological (reading) order. Empty if the transcript
     * is too thin to summarise usefully.
     */
    fun keyPoints(segments: List<TranscriptSegment>, max: Int = 5): List<KeyPoint> {
        val sentences = splitSentences(segments)
        if (sentences.isEmpty()) return emptyList()
        // Short transcripts: everything is a "key point"; ranking adds nothing.
        if (sentences.size <= 3) return sentences.map { KeyPoint(it.text, it.startMs) }

        val target = (if (sentences.size <= 8) 3 else max).coerceAtMost(sentences.size)
        val words = sentences.map { s ->
            s.text.lowercase().split(TOKENS).filter { it.length > 1 && it !in STOP }.toSet()
        }
        val scores = pageRank(words)

        return sentences.indices
            .sortedByDescending { scores[it] }
            .take(target)
            .sortedBy { sentences[it].order }   // back into reading order
            .map { KeyPoint(sentences[it].text, sentences[it].startMs) }
    }

    private fun splitSentences(segments: List<TranscriptSegment>): List<Sentence> {
        val out = ArrayList<Sentence>()
        var order = 0
        for (segment in segments) {
            val parts = segment.text.trim().split(SENTENCE_SPLIT)
            for (raw in parts) {
                val s = raw.trim()
                // Keep only sentences with some substance; skip "Okay." / "Mm-hmm."
                if (s.split(TOKENS).count { it.isNotBlank() } >= 4) {
                    out += Sentence(s, segment.startMs, order++)
                }
            }
        }
        return out
    }

    /** Classic TextRank: PageRank over a sentence-similarity graph (power iteration). */
    private fun pageRank(words: List<Set<String>>): DoubleArray {
        val n = words.size
        val sim = Array(n) { DoubleArray(n) }
        val rowSum = DoubleArray(n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val s = similarity(words[i], words[j])
                sim[i][j] = s
                sim[j][i] = s
            }
            rowSum[i] = sim[i].sum()
        }

        val d = 0.85
        var score = DoubleArray(n) { 1.0 / n }
        repeat(30) {
            val next = DoubleArray(n) { (1 - d) / n }
            for (i in 0 until n) {
                if (rowSum[i] == 0.0) continue
                val share = d * score[i] / rowSum[i]
                for (j in 0 until n) if (sim[i][j] != 0.0) next[j] += share * sim[i][j]
            }
            score = next
        }
        return score
    }

    /** Word-overlap normalised by sentence lengths — the standard TextRank sentence similarity. */
    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val common = a.count { it in b }
        if (common == 0) return 0.0
        val denom = ln(a.size + 1.0) + ln(b.size + 1.0)
        return if (denom == 0.0) 0.0 else common / denom
    }
}
