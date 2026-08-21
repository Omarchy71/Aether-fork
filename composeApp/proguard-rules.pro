# Essential rules for Compose Desktop
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep your model classes for serialization using annotations
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Prevent shrinking of resources and JNI
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Ignore warnings from common libraries if necessary
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn kotlinx.serialization.**
