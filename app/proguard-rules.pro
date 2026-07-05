# Keep WebView related classes
-keep class android.webkit.** { *; }
-keep class * extends android.webkit.WebViewClient
-keep class * extends android.webkit.WebChromeClient

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# WebView JS interface (if any)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
