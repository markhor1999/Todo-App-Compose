package com.tricodestudio.voicekit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The streaming resampler replaced a batch one that needed the whole recording in memory. The risk
 * in that trade is the chunk boundary: a linear interpolation reads two neighbouring samples, and
 * that pair can straddle two chunks. A seam bug would not throw — it would quietly inject clicks
 * into the audio and degrade transcription, so these tests exist to make it loud.
 */
class StreamingResamplerTest {

    private fun ramp(n: Int) = FloatArray(n) { it.toFloat() }

    private fun runChunked(input: FloatArray, chunk: Int, from: Int, to: Int): FloatArray {
        val r = StreamingResampler(from, to)
        val out = ArrayList<Float>()
        var i = 0
        while (i < input.size) {
            val end = minOf(i + chunk, input.size)
            out += r.resample(input.copyOfRange(i, end)).toList()
            i = end
        }
        out += r.flush().toList()
        return out.toFloatArray()
    }

    @Test
    fun `chunk size does not change the output`() {
        // The real decoder hands over whatever the codec produced, so the same audio arrives in
        // different-sized pieces run to run. The result must not depend on that.
        val input = ramp(10_000)
        val reference = runChunked(input, chunk = 10_000, from = 44_100, to = 16_000)
        for (chunk in listOf(1, 2, 7, 512, 1024, 4096)) {
            val got = runChunked(input, chunk, from = 44_100, to = 16_000)
            assertEquals("chunk=$chunk changed the length", reference.size, got.size)
            for (i in reference.indices) {
                assertTrue(
                    "chunk=$chunk diverged at $i: ${reference[i]} vs ${got[i]}",
                    abs(reference[i] - got[i]) < 1e-3f,
                )
            }
        }
    }

    @Test
    fun `a linear ramp stays linear across seams`() {
        // Resampling a straight line must produce a straight line. Any discontinuity at a chunk
        // boundary shows up as a step in the first difference, which is exactly the click a seam
        // bug would put into the audio.
        val out = runChunked(ramp(20_000), chunk = 333, from = 48_000, to = 16_000)
        assertTrue(out.size > 100)
        val step = out[1] - out[0]
        for (i in 1 until out.size - 1) {
            val d = out[i + 1] - out[i]
            assertTrue("discontinuity at $i: expected ~$step, got $d", abs(d - step) < 1e-2f)
        }
    }

    @Test
    fun `output length tracks the rate ratio`() {
        val out = runChunked(ramp(44_100), chunk = 1024, from = 44_100, to = 16_000)
        // One second in, one second out — allow a couple of samples of edge handling.
        assertTrue("expected ~16000 samples, got ${out.size}", abs(out.size - 16_000) <= 3)
    }

    @Test
    fun `matching rates pass through untouched`() {
        val r = StreamingResampler(16_000, 16_000)
        assertTrue(r.isIdentity)
        val input = ramp(100)
        assertTrue(r.resample(input).contentEquals(input))
        assertEquals(0, r.flush().size)
    }

    @Test
    fun `upsampling works too`() {
        val out = runChunked(ramp(8_000), chunk = 256, from = 8_000, to = 16_000)
        assertTrue("expected ~16000 samples, got ${out.size}", abs(out.size - 16_000) <= 3)
    }

    @Test
    fun `empty and single-sample input do not blow up`() {
        val r = StreamingResampler(44_100, 16_000)
        assertEquals(0, r.resample(FloatArray(0)).size)
        // One sample has no partner to interpolate with; it must be held, not dropped or crashed on.
        assertEquals(0, r.resample(floatArrayOf(1f)).size)
        assertEquals(1, r.flush().size)
    }

    @Test
    fun `values stay within the input range`() {
        // Linear interpolation cannot overshoot; if it does, the phase bookkeeping is wrong.
        val input = FloatArray(5_000) { if (it % 2 == 0) -0.8f else 0.8f }
        val out = runChunked(input, chunk = 97, from = 44_100, to = 16_000)
        for (v in out) assertTrue("out of range: $v", v in -0.8001f..0.8001f)
    }
}
