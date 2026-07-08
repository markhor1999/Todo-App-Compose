plugins {
    alias(libs.plugins.voicenotes.android.library)
}

android { namespace = "com.codingwithsalman.voicenotes.core.designsystem" }

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material.icons.extended)
}
