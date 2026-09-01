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
        val chunks = ArrayList<FloatArray>(256)
        var total = 0
        decodeStreamingMono16k(file, onProgress) { chunk ->
            chunks += chunk
            total += chunk.size
        }
        if (chunks.size == 1) return chunks[0]
        val joined = FloatArray(total)
        var at = 0
        for (c in chunks) {
            c.copyInto(joined, at)
            at += c.size
        }
        return joined
    }

    /**
     * Same decode, delivered in pieces: every chunk is 16 kHz mono float PCM, already resampled,
     * and is handed to [onChunk] as soon as the codec produces it. Returns the total number of
     * samples emitted.
     *
     * Prefer this over [decodeToMono16k] for anything that processes audio sequentially. The batch
     * form has to hold the whole recording *and* a second copy of it while joining, which on a
     * one-hour note is comfortably over a gigabyte before the recogniser allocates anything — the
     * memory half of the 2026-09-01 crash cluster. Streaming keeps the footprint at one codec
     * buffer regardless of how long the recording is.
     *
     * [onChunk] must not retain the array it is given.
     */
    public fun decodeStreamingMono16k(
        file: File,
        onProgress: (Float) -> Unit = {},
        onChunk: (FloatArray) -> Unit,
    ): Long {
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

        var totalSamples = 0L
        // Built lazily: the real sample rate can arrive with INFO_OUTPUT_FORMAT_CHANGED, which the
        // codec reports before the first output buffer.
        var resampler: StreamingResampler? = null
        fun emit(mono: FloatArray) {
            if (mono.isEmpty()) return
            val r = resampler ?: StreamingResampler(sampleRate, TARGET_SAMPLE_RATE)
                .also { resampler = it }
            val out = r.resample(mono)
            if (out.isNotEmpty()) {
                totalSamples += out.size
                onChunk(out)
            }
        }
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
                            emit(mono)
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

        resampler?.flush()?.let { tail ->
            if (tail.isNotEmpty()) {
                totalSamples += tail.size
                onChunk(tail)
            }
        }
        onProgress(1f)
        return totalSamples
    }


    public companion object {
        /** Sample rate every model in [VoiceModel] expects. */
        public const val TARGET_SAMPLE_RATE: Int = 16_000
    }
}
