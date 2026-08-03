package com.tricodestudio.voicekit

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Configuration for [VoiceKit.initialize]. Everything the engine needs from the host app arrives
 * here rather than through a shared module, which is what keeps the SDK free of the consumer's
 * architecture.
 */
public data class VoiceKitConfig(
    /** Model used when a call does not name one. */
    val defaultModel: VoiceModel = VoiceModel.MULTILINGUAL,
    /**
     * Allow model downloads over a metered connection. Off by default — these are 40-220 MB, and
     * spending a user's mobile data without asking is the sort of thing that earns one-star reviews.
     *
     * Not yet enforced: [ModelManager.ensure] downloads regardless. Gate the call yourself until
     * this is wired, and check connectivity before you call it.
     */
    val allowMeteredDownload: Boolean = false,
    /** Where model files live. Null uses the app's internal storage, which is the safe default. */
    val modelDirectory: File? = null,
)

/**
 * Manages the on-device model files.
 *
 * Downloads land via a `.part` temp file and a rename, so a killed download never leaves a
 * half-written file that passes the size check and then fails at inference time.
 */
public interface ModelManager {

    /** Install state of [model]. Cold-safe: reflects what is on disk the first time it is read. */
    public fun state(model: VoiceModel): StateFlow<ModelState>

    /**
     * Downloads [model] if it is not already present, suspending until it is [ModelState.Ready].
     * No-op when already installed. Progress is observable through [state].
     *
     * @throws VoiceKitException.DownloadFailed if the download cannot complete.
     */
    public suspend fun ensure(model: VoiceModel)

    /** Deletes [model] from disk. Frees roughly [VoiceModel.approxSizeMb]. */
    public suspend fun remove(model: VoiceModel)

    /** Models currently on disk and usable. */
    public fun installed(): Set<VoiceModel>
}

/**
 * A live transcription session for one recording.
 *
 * The recognizer decodes on its own thread, so [accept] does not block and is safe to call straight
 * from an audio-capture callback.
 */
public interface LiveSession : AutoCloseable {

    /** Running preview: committed segments plus the current partial hypothesis. */
    public val text: StateFlow<String>

    /** Feed a chunk of 16 kHz mono normalized-float PCM. Non-blocking. */
    public fun accept(samples: FloatArray)

    /** Release the recognizer. Idempotent; safe to call from `use {}`. */
    override fun close()
}

/**
 * On-device speech recognition. Audio never leaves the device.
 *
 * ```
 * VoiceKit.initialize(context, licenseKey = BuildConfig.VOICEKIT_KEY)
 * VoiceKit.models.ensure(VoiceModel.MULTILINGUAL)   // once, ~90 MB
 * val result = VoiceKit.transcribe(audioFile)
 * println(result.text)
 * ```
 *
 * A singleton rather than an injectable class on purpose: the engine holds native recognizers and
 * model files that must not exist twice in a process, and a developer should not have to wire a
 * dependency graph to transcribe a file.
 */
public object VoiceKit {

    private class Session(
        val config: VoiceKitConfig,
        val store: ModelStore,
        val engine: SherpaTranscriptionEngine,
        val models: ModelManager,
    )

    @Volatile
    private var session: Session? = null

    private fun require(): Session = session ?: throw VoiceKitException.NotInitialized()

    /**
     * Validates the licence, prepares the engine and returns immediately. Call once, from
     * `Application.onCreate`.
     *
     * **Transcription never requires connectivity.** Offline operation is the entire product, so
     * gating inference on a licence call would destroy the thing being sold. The key is checked for
     * shape here; the server round-trip (once it exists) caches its answer and fails *open* on
     * inference, logging rather than throwing.
     *
     * Calling this twice is safe and replaces the previous configuration.
     *
     * @throws VoiceKitException.InvalidLicense if the key is obviously not a key.
     */
    @JvmStatic
    @JvmOverloads
    public fun initialize(
        context: Context,
        licenseKey: String,
        config: VoiceKitConfig = VoiceKitConfig(),
    ) {
        validateLicenseShape(licenseKey)
        val app = context.applicationContext
        val store = ModelStore(context = app, rootOverride = config.modelDirectory)
        val engine = SherpaTranscriptionEngine(modelStore = store, audioDecoder = AudioDecoder())
        session = Session(
            config = config,
            store = store,
            engine = engine,
            models = ModelManagerImpl(store),
        )
    }

    /** True once [initialize] has completed successfully. */
    @JvmStatic
    public val isInitialized: Boolean
        get() = session != null

    /** Model download and disk management. */
    @JvmStatic
    public val models: ModelManager
        get() = require().models

    /**
     * Transcribes [file] entirely on device.
     *
     * @param model defaults to [VoiceKitConfig.defaultModel].
     * @param language ISO 639-1 hint, validated against [model]. Multilingual models auto-detect, so
     *   this is currently advisory — it rejects an impossible request rather than steering the
     *   decoder. Per-call language forcing is not wired yet.
     * @param onProgress 0f..1f, useful for anything longer than a voice memo.
     *
     * @throws VoiceKitException.ModelNotInstalled if [model] is not on disk — call
     *   [ModelManager.ensure] first.
     * @throws VoiceKitException.UnsupportedAudio if the file cannot be decoded.
     * @throws VoiceKitException.InferenceFailed on an engine-level failure.
     */
    @JvmStatic
    @JvmOverloads
    public suspend fun transcribe(
        file: File,
        model: VoiceModel? = null,
        language: String? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): TranscriptionResult {
        val s = require()
        val chosen = model ?: s.config.defaultModel

        if (language != null && !chosen.supports(language)) {
            throw VoiceKitException.UnsupportedAudio(
                "$chosen does not support '$language' (supports ${chosen.languages}). " +
                    "Use VoiceModel.MULTILINGUAL for auto-detection."
            )
        }
        if (!s.engine.isReady(chosen.spec)) {
            throw VoiceKitException.ModelNotInstalled(chosen)
        }

        return try {
            s.engine.transcribe(
                audioFile = file,
                spec = chosen.spec,
                onProgress = onProgress ?: {},
            )
        } catch (e: VoiceKitException) {
            throw e
        } catch (e: Exception) {
            // Anything escaping the native layer becomes a typed failure. A raw JNI exception
            // crossing an SDK boundary is unactionable for the caller.
            throw VoiceKitException.InferenceFailed(e)
        }
    }

    /**
     * Opens a live-preview session for one recording.
     *
     * Suspends (a model may need loading) and throws rather than returning null: a silent null turns
     * a precondition failure into a support ticket instead of a stack trace.
     *
     * @throws VoiceKitException.LiveUnavailable when no live path can run.
     */
    @JvmStatic
    @JvmOverloads
    public suspend fun startLiveSession(model: VoiceModel? = null): LiveSession {
        require()
        throw VoiceKitException.LiveUnavailable(
            "streaming is not in this build yet — use transcribe() on a finished recording"
        )
    }

    /**
     * Shape-only check, kept separate from [initialize] so it is testable without an Android
     * Context. This is not the entitlement check — that is a server round-trip which caches its
     * answer and never gates inference.
     */
    internal fun validateLicenseShape(licenseKey: String) {
        if (licenseKey.isBlank()) {
            throw VoiceKitException.InvalidLicense(VoiceKitException.InvalidLicense.Reason.MALFORMED)
        }
    }

    /** Test seam: drops the initialized state. */
    internal fun resetForTesting() {
        session = null
    }
}

/** [ModelManager] over [ModelStore]. Holds one state flow per model, created on first observation. */
private class ModelManagerImpl(private val store: ModelStore) : ModelManager {

    private val flows = ConcurrentHashMap<VoiceModel, MutableStateFlow<ModelState>>()

    private fun flowFor(model: VoiceModel): MutableStateFlow<ModelState> =
        flows.computeIfAbsent(model) {
            // Seed from disk, so a model installed in a previous process reads as Ready immediately
            // rather than briefly claiming NotInstalled and re-triggering a download.
            MutableStateFlow(if (store.isInstalled(model.spec)) ModelState.Ready else ModelState.NotInstalled)
        }

    override fun state(model: VoiceModel): StateFlow<ModelState> = flowFor(model).asStateFlow()

    override suspend fun ensure(model: VoiceModel) {
        val flow = flowFor(model)
        if (store.isInstalled(model.spec)) {
            flow.value = ModelState.Ready
            return
        }
        flow.value = ModelState.Downloading(0f)
        try {
            store.download(model.spec).collect { progress ->
                flow.value = ModelState.Downloading(progress.coerceIn(0f, 1f))
            }
        } catch (e: Exception) {
            val failure = VoiceKitException.DownloadFailed(model, e)
            flow.value = ModelState.Failed(failure)
            throw failure
        }
        if (!store.isInstalled(model.spec)) {
            // The flow completed but the files do not verify — a truncated or corrupted download.
            val failure = VoiceKitException.DownloadFailed(model)
            flow.value = ModelState.Failed(failure)
            throw failure
        }
        flow.value = ModelState.Ready
    }

    override suspend fun remove(model: VoiceModel) {
        store.remove(model.spec)
        flowFor(model).value = ModelState.NotInstalled
    }

    override fun installed(): Set<VoiceModel> =
        VoiceModel.entries.filterTo(mutableSetOf()) { store.isInstalled(it.spec) }
}
