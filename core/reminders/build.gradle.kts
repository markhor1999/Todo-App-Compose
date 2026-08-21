plugins {
    alias(libs.plugins.voicenotes.android.library)
    alias(libs.plugins.voicenotes.hilt)
}

android { namespace = "com.codingwithsalman.voicenotes.core.reminders" }

dependencies {
    // api, not implementation: ReminderScheduler is constructor-injected into workers and view
    // models in :app and :feature:note, so Dagger's generated component has to see the type.
    api(projects.core.database)
    implementation(projects.core.common)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)
}
