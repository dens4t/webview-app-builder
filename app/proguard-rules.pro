# Keep Android WebView and JavaScript interfaces.
-keep class android.webkit.** { *; }
-keep class com.webviewbuilder.app.** { *; }

# Start.io (StartApp) SDK - rules from the official integration guide.
-keep class com.startapp.** { *; }
-keep class com.truenet.** { *; }
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn com.startapp.**
-dontwarn org.jetbrains.annotations.**
