package com.codingwithsalman.voicenotes.core.media

import com.tricodestudio.voicekit.AudioDecoder
import java.io.File
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Amplitude envelope for waveform rendering: decode → RMS per bucket → normalize
 * to the loudest bucket. Used for imported files; live recordings capture their
 * envelope from the microphone meter instead.
 */
class WaveformExtractor @Inject constructor(
    private val decoder: AudioDecoder,
) {
    /**
     * Envelope for a file on disk, without ever holding the file in memory.
     *
     * This used to decode the whole recording into one `FloatArray` just to produce 56 numbers —
     * on a one-hour note that is a ~230 MB allocation, in the library UI, which is exactly where
     * the `OutOfMemoryError` reports of 2026-09-01 were landing. Now the decoder streams and this
     * accumulates a fixed-resolution RMS envelope (a few tens of KB whatever the length), which is
     * downsampled to [bars] at the end.
     */
    fun extract(file: File, bars: Int = DEFAULT_BARS): List<Float> {
        if (bars <= 0) return emptyList()
        val coarse = ArrayList<Float>(1024)
        var acc = 0.0
        var count = 0
        val ok = runCatching {
            decoder.decodeStreamingMono16k(file) { chunk ->
                for (v in chunk) {
                    acc += v.toDouble() * v
                    if (++count == ENVELOPE_WINDOW) {
                        coarse += sqrt(acc / count).toFloat()
                        acc = 0.0
                        count = 0
                    }
                }
            }
        }.isSuccess
        if (!ok) return emptyList()
        if (count > 0) coarse += sqrt(acc / count).toFloat()
        if (coarse.isEmpty()) return emptyList()
        return bucketize(coarse, bars)
    }

    /** Average the fixed-resolution envelope down to [bars], then normalize to the loudest bar. */
    private fun bucketize(coarse: List<Float>, bars: Int): List<Float> {
        val out = FloatArray(bars)
        val per = coarse.size.toDouble() / bars
        for (bar in 0 until bars) {
            val from = (bar * per).toInt()
            val to = ((bar + 1) * per).toInt().coerceAtLeast(from + 1).coerceAtMost(coarse.size)
            if (from >= coarse.size) break
            var sum = 0.0
            for (i in from until to) sum += coarse[i]
            out[bar] = (sum / (to - from)).toFloat()
        }
        val max = out.max().takeIf { it > 0f } ?: return List(bars) { 0f }
        return out.map { (it / max).coerceIn(0f, 1f) }
    }

    fun fromSamples(samples: FloatArray, bars: Int = DEFAULT_BARS): List<Float> {
        if (samples.isEmpty() || bars <= 0) return emptyList()
        val bucketSize = (samples.size / bars).coerceAtLeast(1)
        val rms = FloatArray(bars)
        for (bar in 0 until bars) {
            val from = bar * bucketSize
            if (from >= samples.size) break
            val to = (from + bucketSize).coerceAtMost(samples.size)
            var acc = 0.0
            for (i in from until to) acc += samples[i] * samples[i]
            rms[bar] = sqrt(acc / (to - from)).toFloat()
        }
        val max = rms.max().takeIf { it > 0f } ?: return List(bars) { 0f }
        return rms.map { (it / max).coerceIn(0f, 1f) }
    }

    companion object {
        const val DEFAULT_BARS = 56

        /**
         * Samples per RMS point while streaming — 4096 at 16 kHz is ~256 ms, so a one-hour note
         * yields ~14k floats (~56 KB) instead of ~230 MB.
         */
        private const val ENVELOPE_WINDOW = 4096

        /** Downsample a live meter history (arbitrary length) to [bars] peaks. */
        fun downsamplePeaks(history: List<Float>, bars: Int = DEFAULT_BARS): List<Float> {
            if (history.isEmpty()) return emptyList()
            if (history.size <= bars) return history
            val bucket = history.size.toFloat() / bars
            return List(bars) { bar ->
                val from = (bar * bucket).toInt()
                val to = ((bar + 1) * bucket).toInt().coerceAtMost(history.size)
                history.subList(from, to.coerceAtLeast(from + 1)).max()
            }
        }
    }
}
