@file:OptIn(VoiceKitInternalApi::class)

package com.tricodestudio.voicekit

import android.util.Log
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private const val SAMPLE_RATE = 16_000

/** The VAD consumes fixed-size windows; anything left over is carried into the next chunk. */
private const val VAD_WINDOW = 512

private const val TAG = "VoiceKit"

/**
 * Shared machinery for both live paths.
 *
 * Both own a daemon thread that builds a recognizer (expensive — deliberately off the caller's
 * thread) and then drains PCM from a blocking queue. [accept] therefore never blocks and is safe to
 * call straight from an `AudioRecord` callback, which is the only call site that matters.
 *
 * Loading happens on that thread, so a failure has nowhere to be thrown to. [awaitStart] exists for
 * that reason: [VoiceKit.startLiveSession] waits on it and converts a load failure into a thrown
 * [VoiceKitException] on the caller's coroutine, rather than handing back a session that silently
 * never produces text.
 */
private abstract class PumpedLiveSession(
    private val onClosed: () -> Unit,
) : LiveSession {

    protected val queue = LinkedBlockingQueue<FloatArray>()
    protected val poison = FloatArray(0)

    protected val _text = MutableStateFlow("")
    final override val text: StateFlow<String> = _text.asStateFlow()

    @Volatile
    private var running = true
    private val closedOnce = AtomicBoolean(false)

    private val started = CountDownLatch(1)

    @Volatile
    private var startFailure: Throwable? = null

    protected fun launch(threadName: String) {
        Thread({ pump() }, threadName).apply { isDaemon = true; start() }
    }

    /**
     * Build the native resources. Runs on the session thread. Throwing here is the correct way to
     * report a load failure — [awaitStart] turns it into the caller's exception.
     */
    protected abstract fun openResources(): AutoCloseable

    /** Consume one chunk of PCM. Called only between [openResources] and the resource's close. */
    protected abstract fun onSamples(samples: FloatArray)

    /** Last chance to flush buffered audio into [_text] before resources are released. */
    protected open fun onDrained() = Unit

    private fun pump() {
        val resources = try {
            openResources()
        } catch (t: Throwable) {
            Log.e(TAG, "live session failed to start", t)
            startFailure = t
            started.countDown()
            release()
            return
        }
        started.countDown()
        try {
            while (true) {
                val samples = queue.take()
                if (samples === poison) break
                onSamples(samples)
            }
            onDrained()
        } catch (t: Throwable) {
            // A decode failure mid-session must not take the host app's process with it. The text
            // published so far stays readable; the session simply stops advancing.
            Log.e(TAG, "live decode loop failed", t)
        } finally {
            runCatching { resources.close() }
            release()
        }
    }

    /** Blocks until the recognizer is up, then rethrows whatever stopped it. */
    fun awaitStart() {
        started.await()
        startFailure?.let { throw it }
    }

    final override fun accept(samples: FloatArray) {
        if (running) queue.offer(samples)
    }

    final override fun close() {
        release()
    }

    private fun release() {
        running = false
        queue.offer(poison)
        // The pump also calls this from its finally block, so the unlock must happen exactly once
        // or the engine's inference slot gets released twice and a later session runs concurrently.
        if (closedOnce.compareAndSet(false, true)) onClosed()
    }
}

/**
 * Word-by-word English, via a streaming Zipformer transducer.
 *
 * The recognizer reports its own endpoints, so committed text accumulates at phrase boundaries while
 * the current hypothesis keeps updating in place — which is what makes output look like captions
 * rather than text appearing in blocks.
 */
private class WordLevelLiveSession(
    private val spec: AsrModelSpec,
    private val modelStore: ModelStore,
    onClosed: () -> Unit,
) : PumpedLiveSession(onClosed) {

    private lateinit var recognizer: OnlineRecognizer
    private lateinit var stream: com.k2fsa.sherpa.onnx.OnlineStream
    private val committed = StringBuilder()

    fun start() = launch("voicekit-live-word")

    override fun openResources(): AutoCloseable {
        recognizer = buildOnlineRecognizer(spec, modelStore)
        stream = recognizer.createStream()
        return AutoCloseable {
            runCatching { stream.release() }
            runCatching { recognizer.release() }
        }
    }

    override fun onSamples(samples: FloatArray) {
        stream.acceptWaveform(samples, SAMPLE_RATE)
        while (recognizer.isReady(stream)) recognizer.decode(stream)
        val partial = recognizer.getResult(stream).text
        if (recognizer.isEndpoint(stream)) {
            if (partial.isNotBlank()) committed.append(partial.trim()).append(' ')
            recognizer.reset(stream)
        }
        _text.value = (committed.toString() + partial).trim()
    }
}

/**
 * Phrase-level, any language — voice-activity detection segments the feed and each closed speech
 * segment is decoded by an ordinary offline model.
 *
 * The trade against word-level: text lands ~1–2s late, at natural pauses, in exchange for working in
 * every language the offline model supports with **no extra download**. An ellipsis tail marks
 * speech that is captured but not yet decoded, so the UI never looks frozen mid-sentence.
 */
private class ChunkedLiveSession(
    private val spec: AsrModelSpec,
    private val engine: SherpaTranscriptionEngine,
    onClosed: () -> Unit,
) : PumpedLiveSession(onClosed) {

    private lateinit var recognizer: com.k2fsa.sherpa.onnx.OfflineRecognizer
    private lateinit var vad: com.k2fsa.sherpa.onnx.Vad
    private val committed = StringBuilder()
    private var carry = FloatArray(0)

    fun start() = launch("voicekit-live-chunk")

    override fun openResources(): AutoCloseable {
        recognizer = engine.newRecognizer(spec)
        vad = try {
            engine.newLiveVad()
        } catch (t: Throwable) {
            runCatching { recognizer.release() }
            throw t
        }
        return AutoCloseable {
            runCatching { vad.release() }
            runCatching { recognizer.release() }
        }
    }

    override fun onSamples(samples: FloatArray) {
        val data = if (carry.isEmpty()) samples else carry + samples
        var offset = 0
        while (offset + VAD_WINDOW <= data.size) {
            vad.acceptWaveform(data.copyOfRange(offset, offset + VAD_WINDOW))
            offset += VAD_WINDOW
        }
        carry = data.copyOfRange(offset, data.size)
        decodePending()
        val tail = if (vad.isSpeechDetected()) ELLIPSIS else ""
        _text.value = (committed.toString().trim() + tail).trim()
    }

    override fun onDrained() {
        // Without this the final phrase is lost whenever a recording ends without trailing silence,
        // which is the common case — people stop the recorder as they stop talking.
        vad.flush()
        decodePending()
        _text.value = committed.toString().trim()
    }

    private fun decodePending() {
        while (!vad.empty()) {
            val speech = vad.front()
            vad.pop()
            val stream = recognizer.createStream()
            try {
                stream.acceptWaveform(speech.samples, SAMPLE_RATE)
                recognizer.decode(stream)
                val phrase = recognizer.getResult(stream).text.trim()
                if (phrase.isNotEmpty()) committed.append(phrase).append(' ')
            } finally {
                stream.release()
            }
        }
    }

    private companion object {
        const val ELLIPSIS = " …"
    }
}

/**
 * Chooses a live path for [model] and starts it, or throws explaining what is missing.
 *
 * Routing is deliberately not a user-facing choice: a streaming model gives word-level output, any
 * other model gives phrase-level output on the same [LiveSession] contract, and the caller's code
 * is identical either way.
 */
internal fun openLiveSession(
    model: VoiceModel,
    store: ModelStore,
    engine: SherpaTranscriptionEngine,
): LiveSession {
    if (!store.isInstalled(model.spec)) throw VoiceKitException.ModelNotInstalled(model)

    // One native inference at a time, process-wide. A live session running alongside a batch
    // transcribe() would double peak RAM, which is what actually kills the host app on a cheap
    // device — so the batch job wins and the caller is told to retry rather than being crashed.
    if (!engine.tryLockForLiveSession()) {
        throw VoiceKitException.LiveUnavailable(
            "a transcribe() call is using the engine — start the live session after it finishes"
        )
    }
    val unlocked = AtomicBoolean(false)
    val onClosed = { if (unlocked.compareAndSet(false, true)) engine.unlockLiveSession() }

    val session = try {
        if (model.isStreaming) {
            WordLevelLiveSession(model.spec, store, onClosed).also { it.start() }
        } else {
            ChunkedLiveSession(model.spec, engine, onClosed).also { it.start() }
        }
    } catch (t: Throwable) {
        onClosed()
        throw t
    }

    return try {
        session.awaitStart()
        session
    } catch (t: Throwable) {
        // awaitStart already released the engine slot via the pump's finally block.
        throw VoiceKitException.LiveUnavailable(
            "the ${model.name} recognizer could not be loaded: ${t.message ?: t::class.simpleName}"
        )
    }
}

/** Streaming Zipformer transducer over the spec's on-disk int8 files. */
private fun buildOnlineRecognizer(spec: AsrModelSpec, modelStore: ModelStore): OnlineRecognizer {
    fun path(role: ModelFileRole) = modelStore.localFile(spec, spec.file(role)).absolutePath
    val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
    val config = OnlineRecognizerConfig(
        featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
        modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = path(ModelFileRole.ENCODER),
                decoder = path(ModelFileRole.DECODER),
                joiner = path(ModelFileRole.JOINER),
            ),
            tokens = path(ModelFileRole.TOKENS),
            numThreads = threads,
            modelType = "zipformer",
            provider = "cpu",
        ),
        endpointConfig = EndpointConfig(),
        enableEndpoint = true,
        decodingMethod = "greedy_search",
    )
    return OnlineRecognizer(assetManager = null, config = config)
}
