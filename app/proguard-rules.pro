# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/tim/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Add any project specific keep options here:

# Allow obfuscation of android.support.v7.internal.view.menu.**
# to avoid problem on Samsung 4.2.2 devices with appcompat v21
# see https://code.google.com/p/android/issues/detail?id=78377
-keep class !android.support.v7.internal.view.menu.**,android.support.** {*;}

# Custom Switch, referenced via menu action view class
-keep class com.simplecity.amp_library.ui.views.CustomSwitch { *; }

# Another Switch, referenced via menu action view class
-keep class com.afollestad.aesthetic.AestheticSwitchCompat { *; }

# ChromeCast options provider, only referenced via manifest.
-keep class com.simplecity.amp_library.cast.CastOptionsProvider { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keepnames class com.simplecity.amp_library.glide.utils.CustomGlideModule
-keep public class * implements com.bumptech.glide.module.GlideModule

# JAudioTagger
-dontwarn org.jaudiotagger.**
-keep class org.jaudiotagger.** { *; }

# OkHttp
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# Okio
-keep class sun.misc.Unsafe { *; }
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okio.**
-dontwarn javax.annotation.**

# Retrofit
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# GSON
-keepattributes Signature
# For using GSON @Expose annotation
-keepattributes *Annotation*
#--  Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }
# Application classes that will be serialized/deserialized over Gson
-keep class com.simplecity.amp_library.http.itunes.** { *; }
-keep class com.simplecity.amp_library.http.lastfm.** { *; }
# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# RXJava
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# Build is failing without the following.. Something to do with StreamSupport?
-dontwarn java8.**

# Hide an annoying compilation warning
# http://stackoverflow.com/questions/3308010/what-is-the-ignoring-innerclasses-attribute-warning-output-during-compilation
-keepattributes EnclosingMethod

# Retrolambda
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

# Keep MaterialDialogs ThemeSingleton, so we can access it via reflection, from Aesthetic
-keep class com.afollestad.materialdialogs.internal.** { *; }

# Custom Cast Media Button, only referenced via menu
-keep class com.simplecity.amp_library.ui.views.CustomMediaRouteActionProvider.CustomMediaRouteButton { *; }
-keep class com.simplecity.amp_library.ui.views.CustomMediaRouteActionProvider { *; }