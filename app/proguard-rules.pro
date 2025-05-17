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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Ink Api Proguard Rules

# Keep the attributes that contain annotations.
-keepattributes RuntimeVisible*Annotation*

# @UsedByNative should be used to annotate things referenced from name by JNI.
# This includes external methods in the Kotlin code and classes whose type
# is referenced by name in JNI C++ code, as well as any method that is looked
# up by name.
-if class androidx.ink.nativeloader.NativeLoader
-keep class androidx.ink.nativeloader.UsedByNative

# Keep annotated class names.
-if class androidx.ink.nativeloader.NativeLoader
-keepnames @androidx.ink.nativeloader.UsedByNative class * {
  <init>();
}

# Keep annotated class members if the class is kept. This is preserved not only
# from renaming but also from pruning, since some of the annotated methods may
# only be used as callbacks from native code.
-if class androidx.ink.nativeloader.NativeLoader
-keepclassmembers class * {
    @androidx.ink.nativeloader.UsedByNative *;
}

# End Ink Api Proguard Rules

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
 -keep class com.nicos.ink_api_compose.data.database.entities.** { <fields>; }
 -keep class com.nicos.ink_api_compose.data.stroke_converter.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation class com.google.gson.reflect.TypeToken
-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------