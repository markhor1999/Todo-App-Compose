plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.core.billing" }

dependencies {
    implementation(projects.core.datastore)
    implementation(libs.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
