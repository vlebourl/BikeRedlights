# BikeRedlights ProGuard Rules
# Purpose: Optimize release builds while preserving required classes
# Constitution: Minification MUST be enabled per v1.3.0 requirements

# Preserve line numbers for crash debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===========================
# BikeRedlights Specific Rules
# ===========================

# Keep application class
-keep class com.example.bikeredlights.BikeRedlightsApplication { *; }

# Keep all model classes (data classes used with Room/Retrofit)
# These are serialized/deserialized and reflection is used
-keep class com.example.bikeredlights.domain.model.** { *; }
-keep class com.example.bikeredlights.data.model.** { *; }

# ===========================
# Android Architecture Components
# ===========================

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Lifecycle components
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ===========================
# Kotlin & Coroutines
# ===========================

# Kotlin metadata
-keepattributes *Annotation*

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.flow.**internal**

# ===========================
# Dependency Injection (Hilt)
# ===========================

# Hilt - when added
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# ===========================
# Networking (Retrofit, OkHttp)
# ===========================

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Keep generic signature of Call, Response (R8 full mode)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ===========================
# JSON Serialization (Gson/Moshi)
# ===========================

# Gson - if using
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Moshi - if using instead
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# ===========================
# Location Services (Critical for BikeRedlights)
# ===========================

# Google Play Services - Location
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Fused Location Provider
-keep class com.google.android.gms.location.FusedLocationProviderClient { *; }
-keep class com.google.android.gms.location.LocationRequest { *; }
-keep class com.google.android.gms.location.LocationCallback { *; }
-keep class com.google.android.gms.location.LocationResult { *; }

# ===========================
# Jetpack Compose
# ===========================

# Compose Runtime
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Composable functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===========================
# DataStore (Preferences)
# ===========================

-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.core.Serializer {
    *;
}

# ===========================
# WorkManager
# ===========================

-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class androidx.work.** { *; }

# ===========================
# General Android Rules
# ===========================

# View binding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===========================
# Debugging & Crash Reporting
# ===========================

# Keep stack traces readable
-keepattributes SourceFile,LineNumberTable

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# ===========================
# Remove Logging in Release
# ===========================

# Remove Log.d, Log.v, but keep Log.e and Log.w for crash reports
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# ===========================
# Optimization Settings
# ===========================

# Enable optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ===========================
# Warnings to Suppress
# ===========================

# Suppress warnings for missing classes (optional dependencies)
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**