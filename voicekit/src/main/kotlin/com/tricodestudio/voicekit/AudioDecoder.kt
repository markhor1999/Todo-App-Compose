package com.tricodestudio.voicekit

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder

/**
 * Decodes any platform-supported audio file (m4a/AAC, wav, mp3, ogg…) to **16 kHz mono float PCM**,
 * resampling if needed.
 *
 * This is public API rather than an internal helper, and deliberately so: [LiveSession.accept] takes
 * 16 kHz mono normalized floats, which is a real burden to produce correctly — channel downmixing
 * and resampling are exactly the kind of thing that silently half-works and degrades accuracy with
 * no error. Shipping the decoder is part of absorbing the integration week rather than leaving it
 * on the caller.
 *
 * No DI annotations. A library that forces Hilt (or any container) on its consumers is one nobody
 * can adopt; construct it directly, or provide it from your own graph — Murmur does the latter, in
 * `core:media`'s `MediaModule`.
 *
 * CPU-bound and blocking. Call it from a background dispatcher.
 */
public class AudioDecoder {

    /**
     * @param onProgress 0f..1f, driven by presentation timestamps. Only meaningful when the
     *   container declares a duration.
     * @throws VoiceKitException.UnsupportedAudio if the file has no audio track or cannot be decoded.
     */
    public fun decodeToMono16k(file: File, onProgress: (Float) -> Unit = {}): FloatArray {
        if (!file.exists()) {
            throw VoiceKitException.UnsupportedAudio("no file at ${file.absolutePath}")
        }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
        } catch (e: Exception) {
            extractor.release()
            throw VoiceKitException.UnsupportedAudio("${file.name} could not be opened (${e.message})")
        }

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i
                format = f
                break
            }
        }
        val trackFormat = format ?: run {
            extractor.release()
            // Was `error(...)`, i.e. an IllegalStateException with no guidance. An SDK failure
            // should say what is wrong with the input, because the caller is the one who can fix it.
            throw VoiceKitException.UnsupportedAudio("no audio track in ${file.name}")
        }
        extractor.selectTrack(trackIndex)

        val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
        val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
            trackFormat.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        val codec = try {
            MediaCodec.createDecoderByType(mime).also {
                it.configure(trackFormat, null, null, 0)
                it.start()
            }
        } catch (e: Exception) {
            extractor.release()
            throw VoiceKitException.UnsupportedAudio("no decoder for $mime on this device (${e.message})")
        }

        // Output format can change after start (and often carries the real values).
        var sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val pcm = ArrayList<FloatArray>(256)
        var totalSamples = 0L
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inIndex, 0, sampleSize, extractor.sampleTime, 0
                            )
                            if (durationUs > 0) {
                                onProgress(
                                    (extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f)
                                )
                            }
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIndex >= 0 -> {
                        if (bufferInfo.size > 0) {
                            val outBuf = codec.getOutputBuffer(outIndex)!!
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shorts = outBuf.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val mono = FloatArray(shorts.remaining() / channels)
                            var s = 0
                            while (shorts.remaining() >= channels) {
                                var acc = 0f
                                repeat(channels) { acc += shorts.get() / 32768f }
                                mono[s++] = acc / channels
                            }
                            pcm.add(mono)
                            totalSamples += mono.size
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
        } catch (e: VoiceKitException) {
            throw e
        } catch (e: Exception) {
            throw VoiceKitException.UnsupportedAudio("decoding ${file.name} failed (${e.message})")
        } finally {
            runCatching { codec.stop() }
            codec.release()
            extractor.release()
        }

        val joined = FloatArray(totalSamples.toInt())
        var offset = 0
        for (chunk in pcm) {
            chunk.copyInto(joined, offset)
            offset += chunk.size
        }

        return if (sampleRate == TARGET_SAMPLE_RATE) joined
        else resampleLinear(joined, sampleRate, TARGET_SAMPLE_RATE)
    }

    private fun resampleLinear(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (input.isEmpty()) return input
        val outLength = (input.size.toLong() * toRate / fromRate).toInt().coerceAtLeast(1)
        val output = FloatArray(outLength)
        val ratio = (input.size - 1).toDouble() / (outLength - 1).coerceAtLeast(1)
        for (i in output.indices) {
            val pos = i * ratio
            val i0 = pos.toInt()
            val i1 = (i0 + 1).coerceAtMost(input.size - 1)
            val frac = (pos - i0).toFloat()
            output[i] = input[i0] * (1f - frac) + input[i1] * frac
        }
        return output
    }

    public companion object {
        /** Sample rate every model in [VoiceModel] expects. */
        public const val TARGET_SAMPLE_RATE: Int = 16_000
    }
}
