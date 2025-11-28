# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# KEEP YOUR APPLICATION CLASSES
-keep class com.yourcompany.melon_hydroponic.** { *; }
-keep class com.example.melon_monitoring_and_automation.** { *; }

# KEEP - Room database
-keep class * extends androidx.room.RoomDatabase { *; }

# KEEP - Hilt dependencies
#-keep class * extends dagger.hilt.android.internal.legacy.AggregatedElement { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManagerHolder { *; }
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories** { *; }

# KEEP - Supabase
-keep class io.github.janbarari.** { *; }
#-keep class io.github.jan-tennert.supabase.** { *; }

# KEEP - Compose
-keep class androidx.compose.runtime.** { *; }

# KEEP - Serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }

# KEEP - Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# KEEP - Coroutines
-keep class kotlinx.coroutines.** { *; }

# KEEP - Reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# KEEP - Data classes (if using reflection)
-keepclassmembers class * {
    public <init>(...);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimizations
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify