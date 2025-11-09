# --------------------------------------------
# ✅ Remove Android Log calls (for release)
# --------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# --------------------------------------------
# ✅ Keep class members used by Gson (if using Retrofit / Firestore)
# --------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --------------------------------------------
# ✅ Keep Activities, Fragments, ViewModels, etc.
# --------------------------------------------
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends android.app.Application { *; }

# --------------------------------------------
# ✅ If using Firebase
# --------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --------------------------------------------
# ✅ If using Jetpack Compose
# --------------------------------------------
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }

# --------------------------------------------
# ✅ Hide original source file names
# --------------------------------------------
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
