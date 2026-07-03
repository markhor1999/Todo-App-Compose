plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
    alias(libs.plugins.voicenotes.room)
}

android { namespace = "com.codingwithsalman.voicenotes.core.database" }

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.android)
}
