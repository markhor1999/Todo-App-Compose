pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Official sherpa-onnx AAR is distributed via GitHub releases, not Maven Central.
        // Populated by tools/fetch-sherpa.sh (run once after cloning).
        maven {
            url = uri("third_party/m2")
            content { includeGroup("com.k2fsa.sherpa.onnx") }
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "VoiceNotes"
include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:media")
include(":core:billing")
include(":asr:api")
include(":asr:sherpa")
include(":feature:capture")
include(":feature:library")
include(":feature:note")
include(":feature:settings")
include(":feature:onboarding")
