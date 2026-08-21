plugins {
    alias(libs.plugins.voicenotes.android.feature)
}

android { namespace = "com.codingwithsalman.voicenotes.feature.library" }

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.reminders)
    implementation(projects.core.media)
    implementation(projects.asr.api)
    implementation(libs.androidx.activity.compose)
}
