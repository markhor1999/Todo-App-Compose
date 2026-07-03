plugins {
    alias(libs.plugins.voicenotes.android.library)
}

android { namespace = "com.codingwithsalman.voicenotes.asr.api" }

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.coroutines.android)
}
