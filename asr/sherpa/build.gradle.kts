plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.asr.sherpa" }

dependencies {
    api(projects.asr.api)
    // api, not implementation: ModelStore/SherpaTranscriptionEngine are constructor-injected
    // into workers, so Dagger's generated component in :app has to see the types.
    api(projects.voicekit)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.designsystem)
    implementation(projects.core.media)
    // Extraction runs at the end of a transcription job, so the worker arms reminders directly.
    implementation(projects.core.reminders)
    implementation(libs.sherpa.onnx.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)
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
