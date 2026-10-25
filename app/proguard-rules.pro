# ==============================================================================
# ProGuard / R8 Rules for ArAr TOZ (Production Ready)
# ==============================================================================

# Preserve line numbers and source file names for readable Crashlytics & stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Room Database ---
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Data Entities & Domain Models (Mencegah Obfuscation Kolom Database) ---
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.model.** { *; }

# --- Gson Serializer & BackupData (Mencegah Corrupt Key JSON saat Ekspor/Impor) ---
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.data.model.BackupData { *; }

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
