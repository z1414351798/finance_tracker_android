# ── Stack traces ───────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ─────────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Kotlin Coroutines ──────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ── Retrofit ───────────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod

# Keep all Retrofit service interfaces
-keep interface com.z.financetracker.api.** { *; }

# ── OkHttp ─────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn javax.annotation.**

# ── Gson ───────────────────────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ── All entity / model data classes ────────────────────────────────────────────
# Keep field names so Gson serialization matches JSON keys exactly
-keep class com.z.financetracker.entity.** { *; }
-keep class com.z.financetracker.util.Budget { *; }
-keep class com.z.financetracker.enums.** { *; }

# Keep request/response bodies used inside API services
-keepclassmembers class com.z.financetracker.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── ConsentService request body ────────────────────────────────────────────────
-keep class com.z.financetracker.api.ConsentRequest { *; }

# ── Google Identity / Credentials ─────────────────────────────────────────────
-dontwarn com.google.android.libraries.identity.**
-keep class com.google.android.libraries.identity.** { *; }
-keep class androidx.credentials.** { *; }

# ── Coil image loader ──────────────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ── Vico charts ───────────────────────────────────────────────────────────────
-dontwarn com.patrykandpatrick.vico.**

# ── AndroidX / Compose ────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
