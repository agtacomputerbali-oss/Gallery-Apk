# ============================================================
# OP Gallery — ProGuard / R8 Rules
# ============================================================

# --- Jetpack Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Hilt / Dagger ---
-keep class com.gallery.app.di.** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
}

# --- Coil 3 ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- Model & Data Classes ---
-keep class com.gallery.app.domain.** { *; }
-keep class com.gallery.app.data.** { *; }
-keepclassmembers class com.gallery.app.domain.model.** { *; }

# --- Security Crypto & DataStore ---
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# --- Biometric ---
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# --- Kotlin Coroutines ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- General: Jangan strip kelas dengan @Keep ---
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
