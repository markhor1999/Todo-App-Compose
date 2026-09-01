package com.tricodestudio.voicekit

/**
 * Linear resampler that works chunk-by-chunk instead of on a whole recording.
 *
 * Added 2026-09-01 alongside the streaming decode path. The batch resampler it replaces needed the
 * entire decoded file in memory *and* allocated a second array the same size for its output — on a
 * one-hour note at 44.1 kHz that pair alone is over a gigabyte, before sherpa's recognizer asks for
 * its own. See brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md.
 *
 * The only subtlety is the chunk boundary: output sample *i* interpolates between two neighbouring
 * input samples, and the pair can straddle two chunks. [carry] holds the unconsumed tail and
 * [phase] the fractional read position within it, so the seam produces exactly the samples the
 * batch version would have. Not thread-safe; one instance per decode.
 */
internal class StreamingResampler(
    private val fromRate: Int,
    private val toRate: Int,
) {
    private val step: Double = fromRate.toDouble() / toRate.toDouble()

    /** Input samples read but not yet fully consumed — at most a couple of samples. */
    private var carry: FloatArray = FloatArray(0)

    /** Fractional read position inside [carry]. */
    private var phase: Double = 0.0

    /** True when the rates match and every chunk passes straight through. */
    val isIdentity: Boolean get() = fromRate == toRate

    fun resample(input: FloatArray): FloatArray {
        if (isIdentity) return input
        if (input.isEmpty()) return EMPTY

        val buf = if (carry.isEmpty()) input else carry + input
        // Need a sample on each side of the read position to interpolate.
        if (buf.size < 2) {
            carry = buf
            return EMPTY
        }

        // Number of outputs available before the read position runs past the last usable pair.
        val available = ((buf.size - 1 - phase) / step).let {
            if (it <= 0) 0 else kotlin.math.ceil(it).toInt()
        }
        if (available <= 0) {
            carry = buf
            return EMPTY
        }

        val out = FloatArray(available)
        var p = phase
        var n = 0
        while (n < available && p + 1 < buf.size) {
            val i0 = p.toInt()
            val frac = (p - i0).toFloat()
            out[n++] = buf[i0] * (1f - frac) + buf[i0 + 1] * frac
            p += step
        }

        val consumed = p.toInt().coerceAtMost(buf.size)
        carry = buf.copyOfRange(consumed, buf.size)
        phase = p - consumed
        return if (n == available) out else out.copyOf(n)
    }

    /**
     * Emit the final sample once the input is exhausted, so a stream and a batch run agree on
     * length rather than differing by the one sample the interpolation never had a partner for.
     */
    fun flush(): FloatArray {
        if (isIdentity || carry.isEmpty()) return EMPTY
        val last = carry[carry.size - 1]
        carry = EMPTY
        phase = 0.0
        return floatArrayOf(last)
    }

    private companion object {
        val EMPTY = FloatArray(0)
    }
}
