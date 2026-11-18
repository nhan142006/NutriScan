# Reflection contracts for Retrofit, Gson and Room JSON columns.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# Dynamic Retrofit proxies; permit name obfuscation but retain the annotated endpoints.
-keep,allowoptimization,allowobfuscation interface vn.edu.vlu.milestone2.data.remote.**Api { *; }

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Unannotated domain field names form the on-disk JSON schema.
-keep,allowoptimization,allowobfuscation class vn.edu.vlu.milestone2.data.model.**
-keepclassmembers class vn.edu.vlu.milestone2.data.model.** { <fields>; }
-keepclassmembers enum vn.edu.vlu.milestone2.data.local.entity.** { <fields>; }
-keepclassmembers enum vn.edu.vlu.milestone2.data.model.** { <fields>; }
