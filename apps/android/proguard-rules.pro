# Gson reads and writes the OpenAI request and response models by reflection.
# Keep their annotated fields so release builds serialise the same JSON as debug.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn com.google.errorprone.annotations.**
