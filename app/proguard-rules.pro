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

# The @Serializable navigation routes (Navigation Compose type-safe routes) are already covered
# by the generic @kotlinx.serialization.Serializable keep above.

# ──────────────────────────────────────────────────────────────
# ViewModel
# Hilt creates ViewModels via generated factories; the constructor keep guards
# against aggressive R8 inlining. (Hilt/Dagger ship their own consumer rules.)
# ──────────────────────────────────────────────────────────────
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application, ...);
}

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

# ──────────────────────────────────────────────────────────────
# Google Play In-App Review (review-ktx)
# The Billing + Review AARs ship their own consumer keep rules; R8 only needs this
# Play Services compile-time annotation (absent at runtime) suppressed.
# ──────────────────────────────────────────────────────────────
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# sherpa-onnx: JNI resolves these classes/fields reflectively; the AAR ships no consumer rules.
-keep class com.k2fsa.sherpa.onnx.** { *; }
