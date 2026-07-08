package com.codingwithsalman.voicenotes.core.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Raw-PCM recorder for the live-transcription path (v2.1 #2). Unlike [AudioRecorderController]
 * (MediaRecorder, no PCM access), this reads 16 kHz mono PCM from [AudioRecord] so it can feed the
 * streaming recognizer AND still produce a normal AAC `.m4a` (encoded here via MediaCodec +
 * MediaMuxer) so playback / import / waveform / the offline pass all consume the same file format.
 *
 * Used ONLY when live transcription is enabled + the streaming model is installed; everyone else
 * stays on the proven MediaRecorder path. One recording at a time; a single capture thread reads,
 * meters (RMS), forwards PCM to [onPcm], and encodes. Pause drops audio (excluded from file, ASR,
 * and timeline) so the saved duration counts active time only.
 */
class PcmAudioRecorder @Inject constructor(
    @Suppress("unused") @ApplicationContext private val appContext: android.content.Context,
) {
    private var audioRecord: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var captureThread: Thread? = null
    private var outputFile: File? = null

    @Volatile private var running = false
    @Volatile private var lastAmplitude = 0f
    /** Encoded (active) sample count → drives presentationTimeUs and the reported duration. */
    @Volatile private var encodedSamples = 0L

    var isPaused: Boolean = false
        private set

    /** Normalized 0..1 level from the last read buffer (sqrt curve to match the MediaRecorder meter). */
    fun amplitude(): Float = lastAmplitude

    /**
     * Start capturing into [output] (an `.m4a`). [onPcm] receives 16 kHz mono normalized-float
     * chunks on the capture thread — keep it non-blocking (the live session queues internally).
     */
    @SuppressLint("MissingPermission") // caller ensures RECORD_AUDIO before starting
    fun start(output: File, onPcm: (FloatArray) -> Unit) {
        check(!running) { "Recording already in progress" }
        outputFile = output
        encodedSamples = 0L
        isPaused = false
        muxerStarted = false
        trackIndex = -1

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val recordBufBytes = maxOf(minBuf, SAMPLE_RATE * 2) // ≥ ~1 s of headroom
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            recordBufBytes,
        )

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_BYTES)
        }
        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
        muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        running = true
        audioRecord!!.startRecording()
        captureThread = Thread({ captureLoop(onPcm) }, "pcm-capture").apply { start() }
    }

    fun pause() { if (running) isPaused = true }
    fun resume() { if (running) isPaused = false }

    /** Stops, finalizes the file, returns its info — or null if nothing was captured. */
    fun stop(): RecordingInfo? {
        if (!running) return null
        running = false
        captureThread?.join(3_000) // let the loop flush end-of-stream into the muxer
        captureThread = null
        val file = outputFile
        releaseNative()
        outputFile = null
        val durationMs = encodedSamples * 1_000L / SAMPLE_RATE
        return file?.let { RecordingInfo(file = it, durationMs = durationMs, sizeBytes = it.length()) }
    }

    /** Stops and deletes the file — the user discarded the take. */
    fun cancel() {
        if (!running) return
        running = false
        captureThread?.join(3_000)
        captureThread = null
        releaseNative()
        outputFile?.delete()
        outputFile = null
    }

    private fun captureLoop(onPcm: (FloatArray) -> Unit) {
        val ar = audioRecord ?: return
        val buf = ShortArray(SAMPLE_RATE / 10) // 100 ms chunks
        val info = MediaCodec.BufferInfo()
        try {
            while (running) {
                val n = ar.read(buf, 0, buf.size)
                if (n <= 0) continue
                if (isPaused) continue // keep draining the mic, but exclude from file/ASR/timeline
                lastAmplitude = rms(buf, n)
                onPcm(FloatArray(n) { buf[it] / 32768f })
                encode(buf, n, info, endOfStream = false)
            }
            encode(buf, 0, info, endOfStream = true) // flush
        } catch (t: Throwable) {
            Log.e(TAG, "capture loop failed", t)
        }
    }

    /** Queue one PCM chunk into the encoder and drain whatever output is ready into the muxer. */
    private fun encode(pcm: ShortArray, count: Int, info: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val c = codec ?: return
        val inIndex = c.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
        if (inIndex >= 0) {
            val inBuf = c.getInputBuffer(inIndex)!!
            inBuf.clear()
            val ptsUs = encodedSamples * 1_000_000L / SAMPLE_RATE
            if (count > 0) {
                val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until count) bytes.putShort(pcm[i])
                bytes.flip()
                inBuf.put(bytes)
                c.queueInputBuffer(inIndex, 0, count * 2, ptsUs, 0)
                encodedSamples += count
            } else {
                val flags = if (endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                c.queueInputBuffer(inIndex, 0, 0, ptsUs, flags)
            }
        }
        drain(info, endOfStream)
    }

    private fun drain(info: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val c = codec ?: return
        val m = muxer ?: return
        while (true) {
            val outIndex = c.dequeueOutputBuffer(info, if (endOfStream) DEQUEUE_TIMEOUT_US else 0L)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return // nothing ready; pick it up on the next chunk
                }
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        trackIndex = m.addTrack(c.outputFormat)
                        m.start()
                        muxerStarted = true
                    }
                }
                outIndex >= 0 -> {
                    val outBuf = c.getOutputBuffer(outIndex)!!
                    // The codec-config buffer is consumed by addTrack, not written as a sample.
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        m.writeSampleData(trackIndex, outBuf, info)
                    }
                    c.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    private fun releaseNative() {
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        runCatching { if (muxerStarted) muxer?.stop() }
        runCatching { muxer?.release() }
        muxer = null
        muxerStarted = false
    }

    private fun rms(buf: ShortArray, n: Int): Float {
        if (n <= 0) return 0f
        var sum = 0.0
        for (i in 0 until n) {
            val s = buf[i].toDouble()
            sum += s * s
        }
        val normalized = (sqrt(sum / n) / 32768.0).toFloat().coerceIn(0f, 1f)
        return sqrt(normalized) // same perceptual curve as AudioRecorderController.amplitude()
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val BIT_RATE = 64_000
        const val MAX_INPUT_BYTES = 16_384
        const val DEQUEUE_TIMEOUT_US = 10_000L
        const val TAG = "VnPcmRecorder"
    }
}
