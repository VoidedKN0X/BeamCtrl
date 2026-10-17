# kotlinx.serialization models (library ships its own rules; these keep our saved remotes and routes intact).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.vknxworks.beamctrl.**$$serializer { *; }
-keepclassmembers class com.vknxworks.beamctrl.** {
    *** Companion;
}
-keepclasseswithmembers class com.vknxworks.beamctrl.** {
    kotlinx.serialization.KSerializer serializer(...);
}
