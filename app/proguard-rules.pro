# ML Kit keep rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.android.gms.**

# Keep models if needed
-keep class com.google.mlkit.vision.text.latin.** { *; }

# ViewBinding / reflection safety
-keep class **ViewBinding { *; }