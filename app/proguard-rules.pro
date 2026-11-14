# ProGuard / R8 Production Rules for TypeGenius Keyboard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# InputMethodService & Companion Entry Points
-keep public class com.example.service.TypeGeniusIME { *; }
-keep public class com.example.MainActivity { *; }
-keep public class androidx.core.content.FileProvider { *; }

# Room SQLite Database, Entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class com.example.data.local.** { *; }

# Moshi & JSON Models
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.example.data.ai.** { *; }
-keep class com.example.model.** { *; }

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# OkHttp3 & Retrofit2 (Optional direct Gemini HTTPS client)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers,allowshrinking class * {
    @retrofit2.http.* <methods>;
}

# Preserve Compose Runtime Semantics & AndroidX SavedState
-keepclassmembers class * implements androidx.savedstate.SavedStateRegistry$AutoRecreated {
    <init>(...);
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
