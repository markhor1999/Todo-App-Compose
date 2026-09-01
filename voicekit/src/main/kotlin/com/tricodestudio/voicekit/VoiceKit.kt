package com.tricodestudio.voicekit

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * Allow model downloads over a metered connection. Off by default — these run 41-274 MB, and
     * spending a user's mobile data without asking is the sort of thing that earns one-star reviews.
     *
     * Not yet enforced: [ModelManager.ensure] downloads regardless. Gate the call yourself until
     * this is wired, and check connectivity before you call it.
     */
    val allowMeteredDownload: Boolean = false,
    /** Where model files live. Null uses the app's internal storage, which is the safe default. */
    val modelDirectory: File? = null,
    /**
     * Base URL of the VoiceKit distribution endpoint, or null to fetch models from the public
     * mirrors named in the catalog.
     *
     * When set, model downloads are authenticated with your licence key. This is the control that
     * makes a stripped licence check not worth the trouble — see `LICENSING.md`. Leave it null and
     * nothing breaks; the models are public and always were.
     */
    val distributionEndpoint: String? = null,
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
 * VoiceKit.models.ensure(VoiceModel.MULTILINGUAL)   // once, 153 MB
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
        val license: LicenseStatus,
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
        val app = context.applicationContext
        val status = LicenseVerifier.verify(licenseKey, AppIdentity.of(app))
        reportLicense(status)

        // A refused licence must not be discoverable only at download time, so the gated source is
        // wired regardless of status — an Unlicensed/Lapsed key simply gets refused by the endpoint,
        // which is the correct place for a commercial decision to be enforced.
        val source = config.distributionEndpoint
            ?.let { GatedModelSource(licenseKey = licenseKey, endpoint = it.trimEnd('/')) }
            ?: DirectModelSource
        val store = ModelStore(
            context = app,
            rootOverride = config.modelDirectory,
            source = source,
        )
        val engine = SherpaTranscriptionEngine(modelStore = store, audioDecoder = AudioDecoder())
        session = Session(
            config = config,
            store = store,
            engine = engine,
            models = ModelManagerImpl(store),
            license = status,
        )
    }

    /**
     * The licence state established by [initialize].
     *
     * Read it and report it to your own crash reporter if you like — a lapsed key never stops the
     * SDK working, so this is the only way you would find out.
     */
    @JvmStatic
    public val license: LicenseStatus
        get() = require().license

    /** Logs, loudly for a lapse, and never throws — nothing here may take down a host app's start. */
    private fun reportLicense(status: LicenseStatus) {
        when (status) {
            is LicenseStatus.Active -> {
                if (status.entitlement.tier == LicenseTier.TRIAL) {
                    Log.i(TAG, "VoiceKit running on a TRIAL licence for ${status.entitlement.applicationId}.")
                }
                if (status.entitlement.isTest) {
                    Log.w(
                        TAG,
                        "VoiceKit is using a vk_test_ key. It is not bound to your signing " +
                            "certificate — do not ship a release build with it.",
                    )
                }
            }
            is LicenseStatus.Lapsed -> Log.w(
                TAG,
                "VoiceKit licence for ${status.entitlement.applicationId} lapsed on " +
                    "${status.sinceMillis}. The SDK keeps working — your users are not affected — " +
                    "but the subscription needs renewing.",
            )
            is LicenseStatus.Unlicensed -> Log.w(TAG, "VoiceKit is unlicensed: ${status.reason}")
        }
        if (LicenseVerifier.isPreRevenueBuild) {
            Log.i(TAG, "VoiceKit build has no issuer key compiled in: licences are structural only.")
        }
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

        if (chosen.isStreaming) {
            // Caught here rather than deep in the engine: a streaming model has no offline decoder,
            // so the native failure would surface as an unrelated-looking load error.
            throw VoiceKitException.UnsupportedAudio(
                "$chosen decodes a live stream, not a file. Use startLiveSession($chosen), or " +
                    "transcribe() with VoiceModel.MULTILINGUAL."
            )
        }
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
     * Opens a live transcription session, producing a running transcript as you feed it audio.
     *
     * ```
     * VoiceKit.startLiveSession(VoiceModel.ENGLISH_STREAMING).use { session ->
     *     lifecycleScope.launch { session.text.collect { caption.text = it } }
     *     while (recording) session.accept(readPcm())
     * }
     * ```
     *
     * The choice of [model] decides how the text behaves, and nothing else about your code changes:
     *
     * - [VoiceModel.ENGLISH_STREAMING] → **word-by-word**, caption style, English only.
     * - Any other model → **phrase-by-phrase**, committing at natural pauses ~1–2s after they are
     *   spoken, in every language that model supports, with **no additional download**.
     *
     * Feed it 16 kHz mono normalized-float PCM via [LiveSession.accept]; that call is non-blocking
     * and safe from an audio-capture callback. Always [LiveSession.close] the session — `use {}`
     * does it for you — or the native recognizer stays resident.
     *
     * Suspends while the recognizer loads, and throws rather than returning null: a silent null
     * turns a precondition failure into a support ticket instead of a stack trace.
     *
     * @param model defaults to [VoiceKitConfig.defaultModel].
     * @throws VoiceKitException.ModelNotInstalled if [model] is not on disk — call
     *   [ModelManager.ensure] first.
     * @throws VoiceKitException.LiveUnavailable if a [transcribe] call currently holds the engine,
     *   or the recognizer could not be loaded.
     */
    @JvmStatic
    @JvmOverloads
    public suspend fun startLiveSession(model: VoiceModel? = null): LiveSession {
        val s = require()
        val chosen = model ?: s.config.defaultModel
        // Loading a recognizer is heavy and blocking; keep it off whatever dispatcher the caller
        // happened to be on rather than making every caller remember to wrap this.
        return withContext(Dispatchers.IO) {
            openLiveSession(model = chosen, store = s.store, engine = s.engine)
        }
    }

    /**
     * Shape-only check, separate from [initialize] so it is testable without an Android Context.
     *
     * This is not the entitlement check — [LicenseVerifier.verify] does that, offline, against the
     * signed payload carried inside the key itself.
     */
    internal fun validateLicenseShape(licenseKey: String) {
        LicenseVerifier.parseShape(licenseKey).getOrElse { throw it }
    }

    internal const val TAG: String = "VoiceKit"

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
