# Preserve file names and line numbers in stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ──────────────────────────────────────────────────────────────
# Kotlin Serialization  (used by Navigation Compose type-safe routes)
# The Kotlin Serialization plugin generates $serializer companion objects
# at compile time. R8 must not remove or rename them.
# ──────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses

-keep @kotlinx.serialization.Serializable class * { *; }

-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
    static ** $serializer;
}

# If the Serializable class has a Companion that provides serializer()
-if @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep the NavigationRoute sealed hierarchy (all @Serializable sub-types)
-keep class com.codingwithsalman.apps.todo.app.compose.jotjives.presentation.navigation.** { *; }

# ──────────────────────────────────────────────────────────────
# Koin  (4.x)
# singleOf / viewModelOf compile to direct constructor lambdas,
# so most references are resolved at compile time.
# Keep Koin internals that use reflection for module scanning.
# ──────────────────────────────────────────────────────────────
-keepnames class org.koin.** { *; }
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }

# ──────────────────────────────────────────────────────────────
# ViewModel
# Koin creates ViewModels via direct constructor references in lambdas,
# but the constructor keep guards against aggressive R8 inlining.
# ──────────────────────────────────────────────────────────────
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application, ...);
}

# ──────────────────────────────────────────────────────────────
# Glance App Widget
# Widget receiver class name is referenced in AndroidManifest.xml.
# ──────────────────────────────────────────────────────────────
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# ──────────────────────────────────────────────────────────────
# Coroutines
# ──────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ──────────────────────────────────────────────────────────────
# Google Fonts (Compose)
# ──────────────────────────────────────────────────────────────
-keep class androidx.compose.ui.text.googlefonts.** { *; }

# ──────────────────────────────────────────────────────────────
# DataStore Preferences
# ──────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
