# kotlinx.serialization models (library ships its own rules; these keep our saved remotes and routes intact).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.vknxworks.beamctrl.**$$serializer { *; }
-keepclassmembers class com.vknxworks.beamctrl.** {
    *** Companion;
}
-keepclasseswithmembers class com.vknxworks.beamctrl.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google-free: Guava's listenablefuture jar is excluded from the build (see app/build.gradle.kts).
# AndroidX's concurrent-futures still names its interface in classes BeamCtrl never uses.
-dontwarn com.google.common.util.concurrent.ListenableFuture

# No debug, verbose or info logging in release builds (BeamCtrl's own code doesn't log; this also
# silences libraries).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
