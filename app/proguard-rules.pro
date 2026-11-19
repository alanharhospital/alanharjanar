# Keep rules for Al-Anhar Scientific Journal (Firestore, Moshi, Retrofit, WorkManager)
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Preserve Firestore @Keep models and DocumentId / ServerTimestamp fields
-keep class com.example.domain.model.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Preserve Moshi & Retrofit interfaces
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepattributes Exceptions
-dontwarn okio.**
-dontwarn retrofit2.**
