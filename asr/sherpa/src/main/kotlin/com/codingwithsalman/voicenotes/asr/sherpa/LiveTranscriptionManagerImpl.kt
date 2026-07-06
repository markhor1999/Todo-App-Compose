package com.codingwithsalman.voicenotes.asr.sherpa

import android.util.Log
import com.codingwithsalman.voicenotes.asr.api.AsrModelSpec
import com.codingwithsalman.voicenotes.asr.api.LiveModelState
import com.codingwithsalman.voicenotes.asr.api.LiveSession
import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.codingwithsalman.voicenotes.asr.api.ModelCatalog
import com.codingwithsalman.voicenotes.asr.api.ModelFileRole
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
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

private const val SAMPLE_RATE = 16_000
private const val TAG = "VnLiveAsr"

@Singleton
class LiveTranscriptionManagerImpl @Inject constructor(
    private val modelStore: ModelStore,
) : LiveTranscriptionManager {

    private val spec: AsrModelSpec = ModelCatalog.liveEnStreaming
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _modelState = MutableStateFlow<LiveModelState>(
        if (modelStore.isInstalled(spec)) LiveModelState.Ready else LiveModelState.NotInstalled
    )
    override val modelState: StateFlow<LiveModelState> = _modelState.asStateFlow()

    override fun isModelInstalled(): Boolean = modelStore.isInstalled(spec)

    private var downloadJob: Job? = null

    override fun ensureModelDownloaded() {
        if (_modelState.value is LiveModelState.Ready || downloadJob?.isActive == true) return
        // Opt-in, small model downloaded while the user watches the Settings toggle — a plain
        // scoped coroutine (not the WorkManager path the offline model uses) is enough here; a
        // process death just leaves it NotInstalled and the user re-taps.
        downloadJob = scope.launch {
            _modelState.value = LiveModelState.Downloading(0f)
            runCatching {
                modelStore.download(spec).collect { p -> _modelState.value = LiveModelState.Downloading(p) }
            }.onSuccess {
                _modelState.value = if (modelStore.isInstalled(spec)) LiveModelState.Ready
                else LiveModelState.Failed("Files incomplete — try again")
            }.onFailure { e ->
                Log.e(TAG, "live model download failed", e)
                _modelState.value = LiveModelState.Failed(e.message ?: "Download failed")
            }
        }
    }

    override fun newSession(): LiveSession? {
        if (!modelStore.isInstalled(spec)) return null
        return LiveSessionImpl(spec, modelStore)
    }
}

/**
 * One recording's streaming session. Owns a daemon thread that builds the [OnlineRecognizer] (heavy
 * — kept off the caller's thread) and then consumes PCM chunks from a blocking queue, decoding and
 * publishing running text. A poison pill from [release] stops the loop and frees native resources.
 */
private class LiveSessionImpl(
    private val spec: AsrModelSpec,
    private val modelStore: ModelStore,
) : LiveSession {

    private val queue = LinkedBlockingQueue<FloatArray>()
    private val poison = FloatArray(0)

    private val _text = MutableStateFlow("")
    override val text: StateFlow<String> = _text.asStateFlow()

    @Volatile private var running = true

    private val thread = Thread({ run() }, "live-asr").apply { isDaemon = true; start() }

    private fun run() {
        val recognizer = try {
            buildRecognizer(spec, modelStore)
        } catch (t: Throwable) {
            Log.e(TAG, "live recognizer load failed", t)
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

/** Build a streaming Zipformer transducer recognizer from the spec's on-disk int8 files. */
private fun buildRecognizer(spec: AsrModelSpec, modelStore: ModelStore): OnlineRecognizer {
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
