package com.tricodestudio.voicekit

/**
 * This device cannot run the requested model, so the load was refused before reaching native code.
 *
 * Raised instead of letting sherpa-onnx fail on its own terms: on a 32-bit device the failure mode
 * is `SIGBUS`, which is a signal rather than a throwable and kills the process outright. Callers
 * should treat this as a permanent, per-device condition — retrying will not help.
 */
@VoiceKitInternalApi
public class UnsupportedDeviceException(message: String) : IllegalStateException(message)
