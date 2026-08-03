package com.tricodestudio.voicekit

/**
 * Marks declarations that ship in the artifact but are **not** the supported API.
 *
 * These are the engine internals — model specs, file roles, the download store, the sherpa
 * recognizer. Murmur still touches them because it grew up around them, and hiding them outright
 * would mean rewriting the app and the SDK in one commit. Opting in is the honest middle: third-party
 * consumers get a hard compile error rather than a footgun, and every remaining opt-in site in this
 * repo is a to-do list for finishing the migration behind [VoiceKit].
 *
 * Anything annotated here may change or disappear without a major version bump.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "VoiceKit engine internal — not covered by API stability. Use the VoiceKit object instead.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class VoiceKitInternalApi
