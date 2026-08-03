package com.tricodestudio.voicekit

/**
 * A speech model, chosen on the axes a consuming developer actually cares about — which languages,
 * how fast, how much disk.
 *
 * The engine internals (Whisper vs Moonshine, int8 ONNX bundles, encoder/decoder/tokens file roles,
 * where the weights are mirrored) are deliberately absent from this type. Absorbing that complexity
 * is the product; leaking it would just be `AsrModelSpec` with a new name.
 */
public enum class VoiceModel(
    /** ISO 639-1 codes this model targets. Empty means multilingual. */
    public val languages: Set<String>,
    /** Rough on-disk size once installed. Budget for it in your onboarding copy. */
    public val approxSizeMb: Int,
) {
    /** Fastest and smallest. English only. */
    ENGLISH_FAST(setOf("en"), 40),

    /** The default. Multilingual, good accuracy, still comfortable on mid-range hardware. */
    MULTILINGUAL_FAST(emptySet(), 90),

    /** Highest accuracy, noticeably slower and larger. Prefer it for long-form audio. */
    MULTILINGUAL_ACCURATE(emptySet(), 220),
    ;

    /** True when this model has no fixed language list and will auto-detect. */
    public val isMultilingual: Boolean get() = languages.isEmpty()

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
