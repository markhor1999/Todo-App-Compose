import java.util.Properties

plugins {
    alias(libs.plugins.voicenotes.android.application)
    alias(libs.plugins.voicenotes.hilt)
}

// Drop a keystore.properties (gitignored) at the repo root to sign releases:
//   storeFile=/absolute/path/upload.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Absent file -> unsigned release (CI/verification builds keep working).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        load(keystorePropsFile.inputStream())
    }
}

android {
    namespace = "com.codingwithsalman.voicenotes.app"

    defaultConfig {
        applicationId = "com.codingwithsalman.apps.todo.app.compose"
        versionCode = 15
        versionName = "2.4.0"

        ndk {
            // sherpa-onnx ships native libs; keep the APK/AAB to the ABIs real devices use.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(projects.feature.capture)
    implementation(projects.feature.library)
    implementation(projects.feature.note)
    implementation(projects.feature.settings)
    implementation(projects.feature.onboarding)
    implementation(projects.core.billing)

    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.media)
    implementation(projects.core.reminders)
    implementation(projects.asr.api)
    implementation(projects.asr.sherpa)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.play.review.ktx)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)
}
