package com.tricodestudio.voicekit

/**
 * A speech model, chosen on the axes a consuming developer actually cares about — which languages,
 * how fast, how much disk.
 *
 * The engine internals (Whisper vs Moonshine, int8 ONNX bundles, encoder/decoder/tokens file roles,
 * where the weights are mirrored) are deliberately absent. Absorbing that is the product; leaking it
 * would just be `AsrModelSpec` with a new name.
 *
 * Every entry here is backed by a real bundle in [ModelCatalog], and [approxSizeMb] and [languages]
 * are read from it rather than restated — a size that drifts from the files being downloaded is the
 * kind of small lie that costs trust the first time a developer measures it.
 */
public enum class VoiceModel {

    /** Fastest and smallest. English only. */
    ENGLISH_FAST,

    /**
     * English, tuned for low-RAM devices. Same job as [ENGLISH_FAST] with a different architecture
     * that behaves better under memory pressure — reach for it if you support cheap hardware.
     */
    ENGLISH_COMPACT,

    /** Multilingual with auto-detection. The default. */
    MULTILINGUAL,
    ;

    internal val spec: AsrModelSpec
        get() = when (this) {
            ENGLISH_FAST -> ModelCatalog.whisperTinyEn
            ENGLISH_COMPACT -> ModelCatalog.moonshineBaseEn
            MULTILINGUAL -> ModelCatalog.whisperBaseMultilingual
        }

    /** ISO 639-1 codes this model targets. Empty means multilingual with auto-detection. */
    public val languages: Set<String> get() = spec.languages.toSet()

    /**
     * On-disk size once installed, including the shared voice-activity-detection model.
     * Read from the actual download manifest, so it cannot drift from reality.
     */
    public val approxSizeMb: Int
        get() = ((spec.totalBytes + ModelCatalog.vadFile.sizeBytes) / 1_048_576L).toInt()

    /** True when this model auto-detects rather than targeting a fixed language list. */
    public val isMultilingual: Boolean get() = spec.languages.isEmpty()

    /** Whether this model can be asked for [languageCode]. */
    public fun supports(languageCode: String): Boolean =
        isMultilingual || languageCode.lowercase() in languages
}

/** Install state of a [VoiceModel]. Observe it via [ModelManager.state]. */
public sealed interface ModelState {

    /** Not on disk. [ModelManager.ensure] would download it. */
    public data object NotInstalled : ModelState

    /** Downloading. [progress] runs 0f..1f. */
    public data class Downloading(val progress: Float) : ModelState

    /** On disk and usable. */
    public data object Ready : ModelState

    /** The last attempt failed. [cause] says why, and whether retrying is worth it. */
    public data class Failed(val cause: VoiceKitException) : ModelState
}
