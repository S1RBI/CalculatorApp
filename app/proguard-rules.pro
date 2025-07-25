# Сохраняем модели данных
-keep class com.example.calculatorapp.models.** { *; }

# Сохраняем Gson модели
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Fragments
-keep public class * extends androidx.fragment.app.Fragment

# Keep BuildConfig fields (автоматически генерируется при сборке)
-keepclassmembers class **.BuildConfig {
    public static final *;
}

# Общие правила для Android
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
