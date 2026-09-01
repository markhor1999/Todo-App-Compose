package com.tricodestudio.voicekit

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * What this device can actually run sherpa-onnx on.
 *
 * Added 2026-09-01 after Play vitals showed **19 crash issues on 2.3.1 hitting 22.2% of distinct
 * users**, which turned out to be one cause with two faces
 * (`brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md`):
 *
 * - **32-bit devices → `SIGBUS`** inside `Java_com_k2fsa_sherpa_onnx_OfflineRecognizer_newFromFile`,
 *   in the `armeabi_v7a` split. A SIGBUS is a native signal: **no `try`/`catch` can contain it**, and
 *   it takes the whole process down. The only defence is to never attempt the load.
 * - **64-bit low-RAM devices → `OutOfMemoryError`** at whatever allocated next, which is why those
 *   15 issues pointed at innocent bystanders (Compose, Room, coroutines, `ArrayList.grow`). One
 *   Whisper recognizer costs roughly a gigabyte of native heap — see the `transcribeMutex` note in
 *   [SherpaTranscriptionEngine] — and Murmur's install base is 2–3 GB hardware (Redmi 9A, Galaxy
 *   A02s, Infinix).
 *
 * This class is the single place that answers "should we even try?". Construct it from a [Context]
 * in production; use [UNRESTRICTED] in tests and on the JVM.
 */
@VoiceKitInternalApi
public class DeviceCapabilities(
    /** False when the device has no 64-bit ABI, i.e. it runs the `armeabi-v7a` split. */
    public val supportsNativeAsr: Boolean,
    /** True for `ActivityManager.isLowRamDevice()` or anything under [LOW_RAM_LIMIT_BYTES]. */
    public val isLowRam: Boolean,
    /** Physical RAM as the platform reports it, or 0 when unknown. */
    public val totalMemBytes: Long,
) {

    /**
     * Inference threads this device should use. ONNX Runtime allocates a per-thread arena, so on a
     * low-RAM phone the thread count is a direct multiplier on peak native memory. Capping at 2
     * roughly halves it on the 8-core budget SoCs that dominate the install base.
     */
    public val asrThreadCount: Int
        get() {
            val cores = Runtime.getRuntime().availableProcessors()
            return if (isLowRam) cores.coerceIn(1, 2) else cores.coerceIn(2, 4)
        }

    /**
     * Largest offline model this device should load, in megabytes of on-disk weights.
     *
     * Deliberately a *cap*, not a substitution: see [fits]. On a low-RAM device this admits
     * `whisper-tiny-en` (~104 MB) and excludes `whisper-base` multilingual (~160 MB).
     */
    public val maxOfflineModelMb: Int
        get() = if (isLowRam) LOW_RAM_MODEL_LIMIT_MB else Int.MAX_VALUE

    /**
     * Whether [spec] may be loaded here at all.
     *
     * Note what this deliberately does **not** do: it never silently swaps a multilingual model for
     * an English-only one. A user on a 2 GB phone who picked "All languages" is better served by a
     * note that fails visibly than by transcripts that quietly stop understanding their language.
     * The picker filters on this so the situation is normally unreachable; the engine re-checks so
     * a stale selection from an older install cannot crash the process.
     */
    public fun fits(spec: AsrModelSpec): Boolean = refusalReason(spec) == null

    /**
     * A short sentence for the person holding the phone, or null when [spec] is fine here.
     *
     * `SettingsScreen` renders `EngineState.DownloadFailed.message` verbatim, so this has to read
     * as English prose rather than as a log line — [refusalReason] keeps the diagnostic detail.
     */
    public fun refusalMessage(spec: AsrModelSpec): String? = when {
        !supportsNativeAsr ->
            "This phone can't run on-device transcription. Recording still works."
        spec.approxSizeMb > maxOfflineModelMb ->
            "This phone doesn't have enough memory for the “All languages” model. " +
                "Choose the smaller English model instead."
        else -> null
    }

    /** Why [fits] said no, in a form a log line or a bug report can carry. */
    public fun refusalReason(spec: AsrModelSpec): String? = when {
        !supportsNativeAsr ->
            "device has no 64-bit ABI; on-device ASR is not supported here"
        spec.approxSizeMb > maxOfflineModelMb ->
            "model ${spec.id} (~${spec.approxSizeMb} MB) exceeds this device's " +
                "$maxOfflineModelMb MB limit (RAM $totalMemBytes)"
        else -> null
    }

    public companion object {
        /** Below this, treat the device as low-RAM even if the platform does not flag it. */
        public const val LOW_RAM_LIMIT_BYTES: Long = 3L * 1000L * 1000L * 1000L

        /** Offline model weight ceiling on a low-RAM device. */
        public const val LOW_RAM_MODEL_LIMIT_MB: Int = 120

        /** Every path enabled — for unit tests and non-Android callers. */
        public val UNRESTRICTED: DeviceCapabilities = DeviceCapabilities(
            supportsNativeAsr = true,
            isLowRam = false,
            totalMemBytes = 0L,
        )

        public fun from(context: Context): DeviceCapabilities {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val totalMem = am?.let {
                ActivityManager.MemoryInfo().also(it::getMemoryInfo).totalMem
            } ?: 0L
            // SUPPORTED_64_BIT_ABIS describes the device, and Play only serves the armeabi-v7a
            // split to devices that have no 64-bit ABI — so an empty list is exactly the population
            // that received the crashing native library.
            val has64Bit = Build.SUPPORTED_64_BIT_ABIS?.isNotEmpty() == true
            val lowRam = am?.isLowRamDevice == true ||
                (totalMem in 1 until LOW_RAM_LIMIT_BYTES)
            return DeviceCapabilities(
                supportsNativeAsr = has64Bit,
                isLowRam = lowRam,
                totalMemBytes = totalMem,
            )
        }
    }
}
