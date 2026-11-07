# Proguard rules for Bunde (Native Android)
# Keep Kotlin Serialization models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}

# Keep OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Compose models
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
