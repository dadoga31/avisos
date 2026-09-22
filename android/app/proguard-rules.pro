# El puente con la web se llama por reflexión desde JavaScript.
-keepclassmembers class io.github.dadoga31.avisos.PuenteNativo {
    @android.webkit.JavascriptInterface <methods>;
}

# JavaMail carga proveedores y clases MIME por reflexión.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
