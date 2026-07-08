plugins {
    alias(libs.plugins.voicenotes.android.feature)
}

android { namespace = "com.codingwithsalman.voicenotes.feature.onboarding" }

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.asr.api)
    implementation(libs.androidx.activity.compose)
}
