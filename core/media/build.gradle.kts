plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.core.media" }

dependencies {
    implementation(projects.core.model)
    // AudioDecoder ships in the SDK now; Murmur consumes it like any integrator would.
    api(projects.voicekit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
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
