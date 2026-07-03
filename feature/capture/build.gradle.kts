plugins {
    alias(libs.plugins.voicenotes.android.feature)
}

android { namespace = "com.codingwithsalman.voicenotes.feature.capture" }

dependencies {
    implementation(projects.core.media)
    implementation(projects.core.database)
    implementation(projects.asr.api)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
