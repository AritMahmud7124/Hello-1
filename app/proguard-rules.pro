# Keep annotations, generic signatures, and Firestore model classes for reflection serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all Firestore & domain data models (required for DocumentSnapshot.toObject & batch writes)
-keep class com.example.data.model.** { *; }

# Keep FCM service class
-keep class com.example.notifications.HelloFirebaseMessagingService { *; }

# Firebase Firestore & Google Play Services keep rules
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Preserve line numbers for production crash diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
