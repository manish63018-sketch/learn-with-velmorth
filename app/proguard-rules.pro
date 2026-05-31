# Velmorth — ProGuard / R8 Rules
# Keep rules required for Firebase, Gson, and Kotlin serialization

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Firebase Firestore data model classes ─────────────────────────────────────
# Keep all Kotlin data classes used with Firestore .toObject()
-keep class com.velmorth.app.data.model.** { *; }

# ── Gson (JSON parsing for lesson assets) ─────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Prevent stripping generic type info needed by Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Kotlin serialization ──────────────────────────────────────────────────────
-keepattributes InnerClasses
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# ── Jetpack Compose ───────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── WorkManager ───────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── AdMob ─────────────────────────────────────────────────────────────────────
-keep public class com.google.android.gms.ads.** { public *; }

# ── Prevent removing Enum values ──────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
