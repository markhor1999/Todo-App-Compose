package com.tricodestudio.voicekit

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Configuration for [VoiceKit.initialize]. Everything the engine needs from the host app arrives
 * here rather than through a shared module, which is what keeps the SDK free of the consumer's
 * architecture.
 */
public data class VoiceKitConfig(
    /** Model used when a call does not name one. */
    val defaultModel: VoiceModel = VoiceModel.MULTILINGUAL_FAST,
    /** Allow model downloads over a metered connection. Off by default — these are 40-220 MB. */
    val allowMeteredDownload: Boolean = false,
    /** Where model files live. Null uses the app's internal storage, which is the safe default. */
    val modelDirectory: File? = null,
)

/**
 * Manages the on-device model files.
 *
 * Downloads are resumable and verified; [state] survives process death, so a download that started
 * during onboarding is still observable after the user backgrounds the app.
 */
public interface ModelManager {

    /** Install state of [model]. */
    public fun state(model: VoiceModel): StateFlow<ModelState>

    /**
     * Downloads [model] if it is not already present, suspending until it is [ModelState.Ready].
     * No-op when already installed. Progress is observable through [state].
     *
     * @throws VoiceKitException.DownloadFailed if the download cannot complete.
     */
    public suspend fun ensure(model: VoiceModel)

    /** Deletes [model] from disk. Frees [VoiceModel.approxSizeMb]. */
    public suspend fun remove(model: VoiceModel)

    /** Models currently on disk. */
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
 * val result = VoiceKit.transcribe(audioFile)
 * println(result.text)
 * ```
 *
 * A singleton rather than an injectable class on purpose: the engine holds native recognizers and
 * model files that must not be instantiated twice, and consumers should not have to wire a graph to
 * transcribe a file. Everything hangs off this one entry point.
 */
public object VoiceKit {

    /**
     * Validates the licence, caches the entitlement and returns immediately. Call once, from
     * `Application.onCreate`.
     *
     * The licence is checked over the network at most once, then cached with a long grace window and
     * re-checked only when a network happens to be available. **Transcription never requires
     * connectivity** — offline operation is the entire product, so gating inference on a licence
     * call would destroy the thing being sold. If the licence cannot be reached, inference continues
     * and a warning is logged.
     */
    @JvmStatic
    @JvmOverloads
    public fun initialize(
        context: Context,
        licenseKey: String,
        config: VoiceKitConfig = VoiceKitConfig(),
    ) {
        throw NotImplementedError("VoiceKit.initialize: pending extraction from :asr:sherpa")
    }

    /** True once [initialize] has completed successfully. */
    @JvmStatic
    public val isInitialized: Boolean
        get() = false

    /** Model download and disk management. */
    @JvmStatic
    public val models: ModelManager
        get() = throw VoiceKitException.NotInitialized()

    /**
     * Transcribes [file] entirely on device.
     *
     * @param model defaults to [VoiceKitConfig.defaultModel].
     * @param language ISO 639-1 hint. Null lets a multilingual model auto-detect.
     * @param onProgress optional 0f..1f callback for long files.
     *
     * @throws VoiceKitException.ModelNotInstalled if [model] is not on disk.
     * @throws VoiceKitException.UnsupportedAudio if the file cannot be decoded.
     * @throws VoiceKitException.InferenceFailed on an engine-level failure.
     */
    @JvmStatic
    public suspend fun transcribe(
        file: File,
        model: VoiceModel? = null,
        language: String? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): TranscriptionResult {
        throw NotImplementedError("VoiceKit.transcribe: pending extraction from SherpaTranscriptionEngine")
    }

    /**
     * Opens a live-preview session for one recording.
     *
     * Suspends (a model may need loading) and throws rather than returning null: a silent null is
     * the wrong ergonomic for a paid SDK, because it turns a precondition failure into a support
     * ticket instead of a stack trace.
     *
     * @throws VoiceKitException.LiveUnavailable when no live path can run.
     */
    @JvmStatic
    public suspend fun startLiveSession(model: VoiceModel? = null): LiveSession {
        throw NotImplementedError("VoiceKit.startLiveSession: pending extraction from LiveTranscriptionManagerImpl")
    }
}
