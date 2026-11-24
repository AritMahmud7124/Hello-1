# =========================================================================
# Enterprise R8 / ProGuard Obfuscation & Hardening Rules
# =========================================================================

# Aggressive class/method obfuscation & repackaging
-repackageclasses 'o'
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

# Keep minimal attributes needed for runtime annotations & generic signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Strip verbose, debug, and info logs in release APKs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int w(...);
}

# Preserve anonymized line numbers while hiding source file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all Firestore & domain data models (required for DocumentSnapshot.toObject & batch writes)
-keep class com.example.data.model.** { *; }

# Keep FCM service class
-keep class com.example.notifications.HelloFirebaseMessagingService { *; }

# Keep AndroidX Biometric, Security Crypto & Google Tink internals
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.shaded.protobuf.** { *; }
-keep class com.google.crypto.tink.integration.android.** { *; }
-dontwarn com.google.api.client.http.**
-dontwarn org.joda.time.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# OkHttp CertificatePinner & TLS rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Firebase Firestore & Google Play Services keep rules
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
