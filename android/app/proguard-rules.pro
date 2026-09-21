# El puente con la web se llama por reflexión desde JavaScript.
-keepclassmembers class io.github.dadoga31.avisos.PuenteNativo {
    @android.webkit.JavascriptInterface <methods>;
}
