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
    fun extract(file: File, bars: Int = DEFAULT_BARS): List<Float> {
        val samples = runCatching { decoder.decodeToMono16k(file) }.getOrNull()
            ?: return emptyList()
        return fromSamples(samples, bars)
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
