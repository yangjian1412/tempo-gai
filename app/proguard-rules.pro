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

-keepattributes SourceFile, LineNumberTable, Signature, *Annotation*

-keep public class * extends java.lang.Exception

# Retrofit
-keep class retrofit2.** { *; }

# Guava TypeToken - must keep generic signatures for anonymous subclasses
-keep,allowshrinking class * extends com.google.common.reflect.TypeToken
-keep class com.google.common.reflect.TypeToken { *; }
-keepclassmembers,allowshrinking class * extends com.google.common.reflect.TypeToken {
    <init>(...);
}

# Gson TypeToken
-keep,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keepclassmembers,allowshrinking class * extends com.google.gson.reflect.TypeToken {
    <init>(...);
}

# Gson - keep all serialization/deserialization info
-keep class com.google.gson.** { *; }

# Retrofit response types (parameterized SubsonicResponse<List<T>> etc.)
-keep class com.cappielloantonio.tempo.subsonic.models.** { *; }
-keep class com.cappielloantonio.tempo.subsonic.base.** { *; }
-keep class com.cappielloantonio.tempo.subsonic.** { *; }

# OpenSubsonic extension detection
-keep class com.cappielloantonio.tempo.util.OpenSubsonicExtensionsUtil { *; }

# Preferences (stores serialized extension data)
-keep class com.cappielloantonio.tempo.util.Preferences { *; }

# Keep all ViewModels that use TypeToken
-keep class com.cappielloantonio.tempo.viewmodel.HomeViewModel { *; }
-keep class com.cappielloantonio.tempo.viewmodel.HomeRearrangementViewModel { *; }

# All repositories
-keep class com.cappielloantonio.tempo.repository.** { *; }
