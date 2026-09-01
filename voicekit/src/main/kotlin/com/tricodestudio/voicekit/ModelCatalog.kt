package com.tricodestudio.voicekit

/** Which sherpa model family a spec is — selects the recognizer config the engine builds.
 *  WHISPER/MOONSHINE are OFFLINE (the saved-transcript pass); STREAMING_ZIPFORMER is an ONLINE
 *  transducer used only for the live-preview-while-recording feature (v2.1 #2), never the offline pass. */
@VoiceKitInternalApi
enum class ModelFamily { WHISPER, MOONSHINE, STREAMING_ZIPFORMER }

/** One downloadable file of a model bundle. Size verified via HTTP HEAD. */
@VoiceKitInternalApi
data class ModelFileSpec(
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val role: ModelFileRole,
    /**
     * Expected SHA-256, lowercase hex, or null when unpinned.
     *
     * Null for everything served from public mirrors today — we have not verified those digests,
     * and asserting a hash we have not checked would be worse than asserting none. Populate this
     * as files move onto our own distribution, where the digest is known at upload time.
     * [ModelStore] enforces it whenever it is present.
     */
    val sha256: String? = null,
)

@VoiceKitInternalApi
enum class ModelFileRole {
    // Whisper + shared
    ENCODER, DECODER, TOKENS,
    // Moonshine-only
    PREPROCESSOR, UNCACHED_DECODER, CACHED_DECODER,
    // Streaming transducer (zipformer) — encoder/decoder reuse ENCODER/DECODER
    JOINER,
}

@VoiceKitInternalApi
data class AsrModelSpec(
    val id: String,
    val displayName: String,
    /** ISO 639-1 codes the model targets; empty = multilingual. */
    val languages: List<String>,
    /** Whisper `language` parameter; "" lets the model auto-detect (ignored by Moonshine). */
    val languageParam: String,
    val files: List<ModelFileSpec>,
    val isPro: Boolean,
    val family: ModelFamily = ModelFamily.WHISPER,
) {
    val totalBytes: Long get() = files.sumOf(ModelFileSpec::sizeBytes)
    val approxSizeMb: Int get() = (totalBytes / 1_048_576L).toInt()

    /** Look a file up by its role — order-independent, so families with different file sets fit. */
    fun file(role: ModelFileRole): ModelFileSpec = files.first { it.role == role }
}

/**
 * Int8 ONNX bundles from the sherpa-onnx author's HuggingFace mirrors (raw files — no archive
 * extraction on device). Whisper URLs/sizes verified 2026-07-03; Moonshine 2026-07-05.
 */
@VoiceKitInternalApi
public object ModelCatalog {

    private const val HF = "https://huggingface.co/csukuangfj"

    val whisperTinyEn = AsrModelSpec(
        id = "whisper-tiny-en-int8",
        displayName = "English · Fast",
        languages = listOf("en"),
        languageParam = "en",
        files = listOf(
            ModelFileSpec(
                "tiny.en-encoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-tiny.en/resolve/main/tiny.en-encoder.int8.onnx",
                12_937_772L,
                ModelFileRole.ENCODER,
            ),
            ModelFileSpec(
                "tiny.en-decoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-tiny.en/resolve/main/tiny.en-decoder.int8.onnx",
                89_853_865L,
                ModelFileRole.DECODER,
            ),
            ModelFileSpec(
                "tiny.en-tokens.txt",
                "$HF/sherpa-onnx-whisper-tiny.en/resolve/main/tiny.en-tokens.txt",
                835_554L,
                ModelFileRole.TOKENS,
            ),
        ),
        isPro = false,
    )

    val whisperBaseMultilingual = AsrModelSpec(
        id = "whisper-base-int8",
        displayName = "All languages",
        languages = emptyList(),
        languageParam = "",
        files = listOf(
            ModelFileSpec(
                "base-encoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-base/resolve/main/base-encoder.int8.onnx",
                29_120_534L,
                ModelFileRole.ENCODER,
            ),
            ModelFileSpec(
                "base-decoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-base/resolve/main/base-decoder.int8.onnx",
                130_672_026L,
                ModelFileRole.DECODER,
            ),
            ModelFileSpec(
                "base-tokens.txt",
                "$HF/sherpa-onnx-whisper-base/resolve/main/base-tokens.txt",
                816_730L,
                ModelFileRole.TOKENS,
            ),
        ),
        isPro = false,
    )

    /**
     * Multilingual Whisper **tiny**, int8 (~99 MB) — the low-RAM multilingual path (MUR-17).
     *
     * Added 2026-09-01. Before this, the only multilingual option was [whisperBaseMultilingual] at
     * ~160 MB, which [DeviceCapabilities] excludes on 2-3 GB hardware — so a low-RAM user who needed
     * a language other than English had no working path at all. This bundle is within a megabyte of
     * [whisperTinyEn] in size, so it clears the same ceiling while keeping auto-detect.
     *
     * Quality is below `base`: it is offered *instead of* base only where base cannot load.
     * Sizes verified via HTTP HEAD 2026-09-01.
     */
    val whisperTinyMultilingual = AsrModelSpec(
        id = "whisper-tiny-int8",
        displayName = "All languages · Compact",
        languages = emptyList(),
        languageParam = "",
        files = listOf(
            ModelFileSpec(
                "tiny-encoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-tiny/resolve/main/tiny-encoder.int8.onnx",
                12_937_772L,
                ModelFileRole.ENCODER,
            ),
            ModelFileSpec(
                "tiny-decoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-tiny/resolve/main/tiny-decoder.int8.onnx",
                89_855_401L,
                ModelFileRole.DECODER,
            ),
            ModelFileSpec(
                "tiny-tokens.txt",
                "$HF/sherpa-onnx-whisper-tiny/resolve/main/tiny-tokens.txt",
                816_730L,
                ModelFileRole.TOKENS,
            ),
        ),
        isPro = false,
    )

    /**
     * SCAFFOLD (v2.1) — Moonshine base-en int8. Moonshine is lighter at inference than Whisper on
     * mid-range CPUs, so it's the ready-to-flip English fallback if the owed mid-range F2 device
     * spike shows Whisper is too slow (see brain/apps/voicenotes/m1-spike-protocol.md).
     * Deliberately NOT in [all] (not offered in the picker) and NOT the [default] — flipping it on
     * is a one-line change in [defaultFor] gated on that device validation. Engine path in
     * SherpaTranscriptionEngine handles [ModelFamily.MOONSHINE]; download uses these verified sizes.
     */
    val moonshineBaseEn = AsrModelSpec(
        id = "moonshine-base-en-int8",
        displayName = "English · Fast (low-RAM)",
        languages = listOf("en"),
        languageParam = "en",
        family = ModelFamily.MOONSHINE,
        files = listOf(
            ModelFileSpec(
                "preprocess.onnx",
                "$HF/sherpa-onnx-moonshine-base-en-int8/resolve/main/preprocess.onnx",
                14_077_290L,
                ModelFileRole.PREPROCESSOR,
            ),
            ModelFileSpec(
                "encode.int8.onnx",
                "$HF/sherpa-onnx-moonshine-base-en-int8/resolve/main/encode.int8.onnx",
                50_311_494L,
                ModelFileRole.ENCODER,
            ),
            ModelFileSpec(
                "uncached_decode.int8.onnx",
                "$HF/sherpa-onnx-moonshine-base-en-int8/resolve/main/uncached_decode.int8.onnx",
                122_120_451L,
                ModelFileRole.UNCACHED_DECODER,
            ),
            ModelFileSpec(
                "cached_decode.int8.onnx",
                "$HF/sherpa-onnx-moonshine-base-en-int8/resolve/main/cached_decode.int8.onnx",
                99_983_837L,
                ModelFileRole.CACHED_DECODER,
            ),
            ModelFileSpec(
                "tokens.txt",
                "$HF/sherpa-onnx-moonshine-base-en-int8/resolve/main/tokens.txt",
                436_688L,
                ModelFileRole.TOKENS,
            ),
        ),
        isPro = false,
    )

    /**
     * LIVE PREVIEW (v2.1 #2) — English streaming Zipformer transducer, int8 (~41 MB). Powers the
     * words-as-you-speak preview while recording; the SAVED transcript still comes from the offline
     * Whisper pass (so notes stay multilingual). Streaming models are language-specific and there is
     * no viable ur/hi/ar streaming model yet, so live preview is English-only in v2.1 — a real
     * limitation vs the app's multilingual pitch, deliberately scoped and shipped OFF BY DEFAULT
     * (SettingsRepository.liveTranscriptionEnabled) until validated on the Play internal track.
     * Not in [all]; resolvable by id via [hidden]; downloaded on-demand when the toggle is enabled.
     * Sizes verified via HTTP HEAD 2026-07-06.
     */
    val liveEnStreaming = AsrModelSpec(
        id = "streaming-zipformer-en-20m-int8",
        displayName = "English · Live",
        languages = listOf("en"),
        languageParam = "en",
        family = ModelFamily.STREAMING_ZIPFORMER,
        files = listOf(
            ModelFileSpec(
                "encoder-epoch-99-avg-1.int8.onnx",
                "$HF/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/resolve/main/encoder-epoch-99-avg-1.int8.onnx",
                42_845_182L,
                ModelFileRole.ENCODER,
            ),
            ModelFileSpec(
                "decoder-epoch-99-avg-1.int8.onnx",
                "$HF/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/resolve/main/decoder-epoch-99-avg-1.int8.onnx",
                539_499L,
                ModelFileRole.DECODER,
            ),
            ModelFileSpec(
                "joiner-epoch-99-avg-1.int8.onnx",
                "$HF/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/resolve/main/joiner-epoch-99-avg-1.int8.onnx",
                259_572L,
                ModelFileRole.JOINER,
            ),
            ModelFileSpec(
                "tokens.txt",
                "$HF/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17/resolve/main/tokens.txt",
                5_048L,
                ModelFileRole.TOKENS,
            ),
        ),
        isPro = false,
    )

    /** silero VAD — tiny, shared by every model; segments long audio for the recognizer's window. */
    val vadFile = ModelFileSpec(
        fileName = "silero_vad.onnx",
        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
        sizeBytes = 643_854L,
        role = ModelFileRole.ENCODER, // role is unused for the standalone VAD file
    )

    /** Models offered in the picker. Moonshine is intentionally absent until device-validated. */
    val all: List<AsrModelSpec> =
        listOf(whisperTinyEn, whisperTinyMultilingual, whisperBaseMultilingual)

    /** Specs resolvable by id but not shown in the offline model picker: the dormant Moonshine
     *  fallback and the live-preview streaming model (downloaded via its own settings toggle). */
    private val hidden: List<AsrModelSpec> = listOf(moonshineBaseEn, liveEnStreaming)

    val default: AsrModelSpec = whisperTinyEn

    /**
     * The low-RAM default: English tiny, which is also the global [default].
     *
     * ⚠️ This used to name [moonshineBaseEn], which is **~287 MB — larger than the
     * [whisperBaseMultilingual] bundle that low-RAM devices are refused for being too big.** That
     * label was actively misleading, so it is corrected here (2026-09-01); Moonshine stays dormant
     * and is still resolvable by id, but it is not a low-RAM anything.
     */
    val lowRamDefault: AsrModelSpec = whisperTinyEn

    /** Multilingual choice for a device that cannot hold [whisperBaseMultilingual]. */
    val lowRamMultilingual: AsrModelSpec = whisperTinyMultilingual

    /**
     * Default spec for a device. Low-RAM devices get the tiny bundle; everything else keeps
     * [default]. Callers that need a *multilingual* low-RAM model want [lowRamMultilingual].
     */
    fun defaultFor(lowRam: Boolean): AsrModelSpec = if (lowRam) lowRamDefault else default

    fun byId(id: String?): AsrModelSpec = (all + hidden).firstOrNull { it.id == id } ?: default
}
