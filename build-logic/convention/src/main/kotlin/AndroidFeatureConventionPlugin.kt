import com.codingwithsalman.voicenotes.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("voicenotes.android.library")
                apply("voicenotes.hilt")
            }

            dependencies {
                "implementation"(project(":core:common"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:designsystem"))

                "implementation"(libs.findLibrary("androidx-hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation-compose").get())
                "implementation"(libs.findLibrary("androidx-compose-material-icons-extended").get())
            }
        }
    }
}
