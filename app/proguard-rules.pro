# Media3 / ExoPlayer keep rules
-dontwarn androidx.media3.**

# Retrofit / OkHttp (if you plug in a real API later)
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin serialization models (add your @Serializable classes as needed)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
