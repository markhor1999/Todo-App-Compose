plugins {
    alias(libs.plugins.voicenotes.android.library)
}

android { namespace = "com.tricodestudio.voicekit" }

// No Hilt, and no `projects.core.*`. Both are deliberate: this module ships to third-party apps,
// and a library that drags in a DI framework or a host app's Room/design-system modules is one that
// nobody can adopt. Anything the engine needs from the host arrives through VoiceKitConfig or a
// constructor parameter — never through a shared module.
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    // The recognizer itself now lives here. This is the SDK's only heavyweight dependency.
    api(libs.sherpa.onnx.android)

    testImplementation(libs.junit)
}
