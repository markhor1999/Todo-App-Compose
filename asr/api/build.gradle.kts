plugins {
    alias(libs.plugins.voicenotes.android.library)
}

android { namespace = "com.codingwithsalman.voicenotes.asr.api" }

dependencies {
    implementation(projects.core.model)
    // EngineState still surfaces AsrModelSpec, which now lives in the SDK.
    api(projects.voicekit)
    implementation(libs.kotlinx.coroutines.android)
}

// Dagger/KSP generates factories referencing VoiceKit engine internals, and generated sources
// cannot carry a @file:OptIn. Opt in for the whole module instead. Each module listed here is a
// site still bound to the engine rather than to the public VoiceKit API — the list shrinking to
// nothing is what "migration finished" looks like.
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=com.tricodestudio.voicekit.VoiceKitInternalApi")
    }
}
