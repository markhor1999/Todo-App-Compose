plugins {
    alias(libs.plugins.voicenotes.android.feature)
}

android { namespace = "com.codingwithsalman.voicenotes.feature.note" }

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.media)
    implementation(projects.asr.api)
    implementation(projects.core.datastore)
    implementation(projects.core.billing)
    implementation(libs.androidx.activity.compose)
}
