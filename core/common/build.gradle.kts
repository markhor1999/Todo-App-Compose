plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.core.common" }

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
