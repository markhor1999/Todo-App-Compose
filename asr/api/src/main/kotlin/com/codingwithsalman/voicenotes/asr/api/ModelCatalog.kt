package com.codingwithsalman.voicenotes.asr.api

/** One downloadable file of a model bundle. Size verified via HTTP HEAD 2026-07-03. */
data class ModelFileSpec(
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
)

data class AsrModelSpec(
    val id: String,
    val displayName: String,
    /** ISO 639-1 codes the model targets; empty = multilingual. */
    val languages: List<String>,
    /** Whisper `language` parameter; "" lets the model auto-detect. */
    val languageParam: String,
    val files: List<ModelFileSpec>,
    val isPro: Boolean,
) {
    val totalBytes: Long get() = files.sumOf(ModelFileSpec::sizeBytes)
    val approxSizeMb: Int get() = (totalBytes / 1_048_576L).toInt()

    fun file(role: ModelFileRole): ModelFileSpec = when (role) {
        ModelFileRole.ENCODER -> files[0]
        ModelFileRole.DECODER -> files[1]
        ModelFileRole.TOKENS -> files[2]
    }
}

enum class ModelFileRole { ENCODER, DECODER, TOKENS }

/**
 * Whisper int8 ONNX bundles from the sherpa-onnx author's HuggingFace mirrors
 * (raw files — no archive extraction needed on device). URLs verified 2026-07-03.
 */
object ModelCatalog {

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
            ),
            ModelFileSpec(
                "tiny.en-decoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-tiny.en/resolve/main/tiny.en-decoder.int8.onnx",
                89_853_865L,
            ),
            ModelFileSpec(
                "tiny.en-tokens.txt",
                "$HF/sherpa-onnx-whisper-tiny.en/resolve/main/tiny.en-tokens.txt",
                835_554L,
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
            ),
            ModelFileSpec(
                "base-decoder.int8.onnx",
                "$HF/sherpa-onnx-whisper-base/resolve/main/base-decoder.int8.onnx",
                130_672_026L,
            ),
            ModelFileSpec(
                "base-tokens.txt",
                "$HF/sherpa-onnx-whisper-base/resolve/main/base-tokens.txt",
                816_730L,
            ),
        ),
        isPro = false,
    )

    /** silero VAD — tiny, shared by every model; segments long audio for Whisper's 30 s window. */
    val vadFile = ModelFileSpec(
        fileName = "silero_vad.onnx",
        url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx",
        sizeBytes = 643_854L,
    )

    val all: List<AsrModelSpec> = listOf(whisperTinyEn, whisperBaseMultilingual)

    val default: AsrModelSpec = whisperTinyEn

    fun byId(id: String?): AsrModelSpec = all.firstOrNull { it.id == id } ?: default
}
