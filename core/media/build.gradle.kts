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
