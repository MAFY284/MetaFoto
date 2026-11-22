# metadata-extractor uses directory/reader implementations that may be resolved reflectively.
-keep class com.drew.** { *; }
-dontwarn com.drew.**
-keep class com.adobe.internal.xmp.** { *; }
-dontwarn com.adobe.internal.xmp.**

# osmdroid loads tiles, overlays and marker assets by name.
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep metadata model names readable in crash reports without exposing user data.
-keepnames class com.metafoto.app.domain.model.** { *; }
