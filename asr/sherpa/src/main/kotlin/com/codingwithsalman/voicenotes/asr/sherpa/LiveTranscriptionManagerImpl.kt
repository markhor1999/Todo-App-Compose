package com.codingwithsalman.voicenotes.asr.sherpa

import android.util.Log
import com.codingwithsalman.voicenotes.asr.api.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.LiveModelState
import com.codingwithsalman.voicenotes.asr.api.LiveSession
import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.codingwithsalman.voicenotes.asr.api.ModelCatalog
import com.codingwithsalman.voicenotes.asr.api.ModelFileRole
import com.codingwithsalman.voicenotes.core.datastore.SettingsRepository
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val SAMPLE_RATE = 16_000
private const val VAD_WINDOW = 512
private const val TAG = "VnLiveAsr"

/**
 * Live preview routing (v2.1 #2). Two engines behind one [LiveSession] contract:
 *
 * - **Word-level EN** — streaming Zipformer transducer ([ModelCatalog.liveEnStreaming], ~41 MB,
 *   optional download): words appear as you speak. English-only (streaming models are
 *   language-specific; verified 2026-07-06 there is NO ur/hi streaming model in existence).
 * - **Phrase-level, any language** — VAD-chunked decoding on the user's *installed offline model*
 *   (Whisper): each spoken phrase is decoded at its natural pause (~1–2 s later). Multilingual with
 *   per-phrase auto-detect (handles ur/en code-switching), zero extra download.
 *
 * Routing: EN-only model selected + zipformer installed → word-level; otherwise the installed
 * offline model → chunked. Every session try-locks the engine's process-wide inference slot, so a
 * live session never runs concurrently with a background transcription job (RAM invariant).
 */
@Singleton
class LiveTranscriptionManagerImpl @Inject constructor(
    private val modelStore: ModelStore,
    private val engine: SherpaTranscriptionEngine,
    settings: SettingsRepository,
) : LiveTranscriptionManager {

    private val zipSpec: AsrModelSpec = ModelCatalog.liveEnStreaming
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Selected offline model, cached so [newSession]/[isAvailable] stay synchronous. */
    @Volatile private var offlineSpec: AsrModelSpec = ModelCatalog.default

    init {
        scope.launch {
            settings.modelId.map(ModelCatalog::byId).collect { offlineSpec = it }
        }
    }

    private val _modelState = MutableStateFlow<LiveModelState>(
        if (modelStore.isInstalled(zipSpec)) LiveModelState.Ready else LiveModelState.NotInstalled
    )
    override val modelState: StateFlow<LiveModelState> = _modelState.asStateFlow()

    /** Some live path is usable: the chunked path (installed offline model) or the EN zipformer. */
    override fun isAvailable(): Boolean =
        modelStore.isInstalled(offlineSpec) || modelStore.isInstalled(zipSpec)

    private var downloadJob: Job? = null

    override fun ensureModelDownloaded() {
        if (_modelState.value is LiveModelState.Ready || downloadJob?.isActive == true) return
        // Opt-in, small model downloaded while the user watches the Settings toggle — a plain
        // scoped coroutine (not the WorkManager path the offline model uses) is enough here; a
        // process death just leaves it NotInstalled and the user re-taps.
        downloadJob = scope.launch {
            _modelState.value = LiveModelState.Downloading(0f)
            runCatching {
                modelStore.download(zipSpec).collect { p -> _modelState.value = LiveModelState.Downloading(p) }
            }.onSuccess {
                _modelState.value = if (modelStore.isInstalled(zipSpec)) LiveModelState.Ready
                else LiveModelState.Failed("Files incomplete — try again")
            }.onFailure { e ->
                Log.e(TAG, "live model download failed", e)
                _modelState.value = LiveModelState.Failed(e.message ?: "Download failed")
            }
        }
    }

    override fun newSession(): LiveSession? {
        val offline = offlineSpec
        val wordLevel = offline.languages == listOf("en") && modelStore.isInstalled(zipSpec)
        val chunked = modelStore.isInstalled(offline)
        if (!wordLevel && !chunked && !modelStore.isInstalled(zipSpec)) return null
        // One native inference at a time, process-wide: if a background transcription job is
        // mid-file, skip live preview for this take (the caller falls back to plain recording).
        if (!engine.tryLockForLiveSession()) {
            Log.i(TAG, "live preview skipped: a transcription job holds the engine")
            return null
        }
        val unlockOnce = AtomicBoolean(false)
        val onClosed = { if (unlockOnce.compareAndSet(false, true)) engine.unlockLiveSession() }
        return when {
            wordLevel -> WordLevelLiveSession(zipSpec, modelStore, onClosed)
            chunked -> ChunkedLiveSession(offline, engine, onClosed)
            else -> WordLevelLiveSession(zipSpec, modelStore, onClosed) // zipformer-only edge
        }
    }
}

/**
 * Word-by-word English session — streaming Zipformer transducer. Owns a daemon thread that builds
 * the [OnlineRecognizer] (heavy — kept off the caller's thread) and then consumes PCM chunks from a
 * blocking queue, decoding and publishing running text. A poison pill from [release] stops the loop.
 */
private class WordLevelLiveSession(
    private val spec: AsrModelSpec,
    private val modelStore: ModelStore,
    private val onClosed: () -> Unit,
) : LiveSession {

    private val queue = LinkedBlockingQueue<FloatArray>()
    private val poison = FloatArray(0)

    private val _text = MutableStateFlow("")
    override val text: StateFlow<String> = _text.asStateFlow()

    @Volatile private var running = true

    init {
        Thread({ run() }, "live-asr-word").apply { isDaemon = true; start() }
    }

    private fun run() {
        val recognizer = try {
            buildOnlineRecognizer(spec, modelStore)
        } catch (t: Throwable) {
            Log.e(TAG, "live recognizer load failed", t)
            onClosed()
            return
        }
        val stream = recognizer.createStream()
        val committed = StringBuilder()
        try {
            while (true) {
                val samples = queue.take()
                if (samples === poison) break
                stream.acceptWaveform(samples, SAMPLE_RATE)
                while (recognizer.isReady(stream)) recognizer.decode(stream)
                val partial = recognizer.getResult(stream).text
                if (recognizer.isEndpoint(stream)) {
                    if (partial.isNotBlank()) committed.append(partial.trim()).append(' ')
                    recognizer.reset(stream)
                }
                _text.value = (committed.toString() + partial).trim()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "live decode loop failed", t)
        } finally {
            runCatching { stream.release() }
            runCatching { recognizer.release() }
            onClosed()
        }
    }

    override fun accept(samples: FloatArray) {
        if (running) queue.offer(samples)
    }

    override fun release() {
        if (!running) return
        running = false
        queue.offer(poison)
    }
}

/**
 * Phrase-level multilingual session — silero VAD segments the mic feed; each closed speech segment
 * is decoded by the user's installed OFFLINE model (Whisper, auto-detect per phrase). Text commits
 * at natural pauses (or every ~8 s of continuous speech) rather than word-by-word — the trade that
 * buys every language with no extra download. An "…" tail shows while speech is being captured.
 */
private class ChunkedLiveSession(
    private val spec: AsrModelSpec,
    private val engine: SherpaTranscriptionEngine,
    private val onClosed: () -> Unit,
) : LiveSession {

    private val queue = LinkedBlockingQueue<FloatArray>()
    private val poison = FloatArray(0)

    private val _text = MutableStateFlow("")
    override val text: StateFlow<String> = _text.asStateFlow()

    @Volatile private var running = true

    init {
        Thread({ run() }, "live-asr-chunk").apply { isDaemon = true; start() }
    }

    private fun run() {
        val recognizer = try {
            engine.newRecognizer(spec)
        } catch (t: Throwable) {
            Log.e(TAG, "chunked live recognizer load failed", t)
            onClosed()
            return
        }
        val vad = try {
            engine.newLiveVad()
        } catch (t: Throwable) {
            Log.e(TAG, "live VAD load failed", t)
            runCatching { recognizer.release() }
            onClosed()
            return
        }
        val committed = StringBuilder()
        var carry = FloatArray(0)

        fun decodePending() {
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

        try {
            while (true) {
                val samples = queue.take()
                if (samples === poison) break
                // VAD consumes fixed 512-sample windows; slice with carry-over between chunks.
                val data = if (carry.isEmpty()) samples else carry + samples
                var off = 0
                while (off + VAD_WINDOW <= data.size) {
                    vad.acceptWaveform(data.copyOfRange(off, off + VAD_WINDOW))
                    off += VAD_WINDOW
                }
                carry = data.copyOfRange(off, data.size)
                decodePending()
                val tail = if (vad.isSpeechDetected()) ELLIPSIS else ""
                _text.value = (committed.toString().trim() + tail).trim()
            }
            // Final flush so the last phrase (no trailing silence) still previews.
            vad.flush()
            decodePending()
            _text.value = committed.toString().trim()
        } catch (t: Throwable) {
            Log.e(TAG, "chunked live loop failed", t)
        } finally {
            runCatching { vad.release() }
            runCatching { recognizer.release() }
            onClosed()
        }
    }

    override fun accept(samples: FloatArray) {
        if (running) queue.offer(samples)
    }

    override fun release() {
        if (!running) return
        running = false
        queue.offer(poison)
    }

    private companion object {
        const val ELLIPSIS = " …"
    }
}

/** Build a streaming Zipformer transducer recognizer from the spec's on-disk int8 files. */
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
