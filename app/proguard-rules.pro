# Claude's structured output schema and Jackson persistence read these classes reflectively.
-keep class com.nbradbury.snaptosite.ai.** { *; }
-keep class com.nbradbury.snaptosite.site.PublishedState { *; }
-keep class com.nbradbury.snaptosite.site.PublishedPage { *; }
-keep class com.nbradbury.snaptosite.site.UploadedMedia { *; }
-keep class com.nbradbury.snaptosite.wp.Credentials { *; }
-keep class com.nbradbury.snaptosite.PendingLogin { *; }
-keep class kotlin.Metadata { *; }

# Only reached by the SDK's class-to-schema derivation, which the app doesn't use on-device
# (see SiteAi.schemaFor), and by OpenTelemetry's AutoValue annotations.
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn java.lang.reflect.AnnotatedParameterizedType
-dontwarn com.google.auto.value.AutoValue
