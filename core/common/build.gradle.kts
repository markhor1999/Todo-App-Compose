plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.core.common" }

dependencies {
    implementation(libs.kotlinx.coroutines.android)

    // Deadline/task extraction is pure logic and the one place in the app where being subtly wrong
    // fires a notification at the user, so it carries real tests.
    testImplementation(libs.junit)
}
