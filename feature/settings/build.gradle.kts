plugins {
    alias(libs.plugins.voicenotes.android.feature)
}

android { namespace = "com.codingwithsalman.voicenotes.feature.settings" }

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.asr.api)
    implementation(projects.core.billing)
    implementation(libs.androidx.activity.compose)
}
