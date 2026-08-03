package com.tricodestudio.voicekit

/**
 * Every failure this SDK can produce.
 *
 * Each one names what to do about it. That is not politeness — a generic "something went wrong" in
 * a transcription path teaches users to retry the identical input, and on a metered engine every
 * blind retry costs someone money. (Observed directly in another app on this account: one user
 * burned 13 paid generations re-running a prompt the model had already declined, because the error
 * said nothing.) At 10-40 B2B customers, support load is the cost that actually hurts.
 */
public sealed class VoiceKitException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** [VoiceKit.initialize] was never called, or it threw and was not retried. */
    public class NotInitialized : VoiceKitException(
        "VoiceKit.initialize(context, licenseKey) must be called before any other call — " +
            "Application.onCreate is the right place."
    )

    /** The licence key was rejected. [reason] distinguishes a typo from an expiry. */
    public class InvalidLicense(
        public val reason: Reason,
    ) : VoiceKitException("License rejected: ${reason.explanation}") {

        public enum class Reason(internal val explanation: String) {
            MALFORMED("the key is not a VoiceKit key — check for a truncated or wrapped string"),
            UNKNOWN_KEY("the key is not recognised — check you are using the live key, not a placeholder"),
            EXPIRED("the subscription has lapsed"),
            APP_MISMATCH(
                "the key is bound to a different applicationId or signing certificate — " +
                    "keys are per-app, so debug and release builds signed differently need the right key"
            ),
        }
    }

    /** A transcription was requested for a model that is not on disk. Call [ModelManager.ensure]. */
    public class ModelNotInstalled(
        public val model: VoiceModel,
    ) : VoiceKitException(
        "Model $model is not installed (~${model.approxSizeMb} MB). " +
            "Call VoiceKit.models.ensure($model) and await ModelState.Ready first."
    )

    /** The download could not complete. Usually transient — surface a retry. */
    public class DownloadFailed(
        public val model: VoiceModel,
        cause: Throwable? = null,
    ) : VoiceKitException("Could not download model $model. Check connectivity and free disk space.", cause)

    /** The audio could not be read or decoded. [detail] says what was wrong with it. */
    public class UnsupportedAudio(
        public val detail: String,
    ) : VoiceKitException(
        "Audio could not be decoded: $detail. Supply a file the platform can decode " +
            "(wav, m4a, mp3, ogg); 16 kHz mono is decoded fastest and needs no resampling."
    )

    /** Inference itself failed. Rare, and not something the caller can fix by retrying the same input. */
    public class InferenceFailed(
        cause: Throwable? = null,
    ) : VoiceKitException("Transcription failed inside the engine.", cause)

    /** No live-transcription path is available right now — see the message for which precondition failed. */
    public class LiveUnavailable(
        public val detail: String,
    ) : VoiceKitException("Live transcription unavailable: $detail")
}
