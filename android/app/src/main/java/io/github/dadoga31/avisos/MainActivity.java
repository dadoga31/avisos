package io.github.dadoga31.avisos;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewFeature;

import io.github.dadoga31.avisos.correo.Cuenta;
import io.github.dadoga31.avisos.correo.ServicioCorreo;

import java.io.File;

/**
 * Contenedor de la aplicación web. Los archivos van dentro del APK y se
 * sirven desde appassets.androidplatform.net, que es un origen seguro: así
 * IndexedDB y las fotos siguen funcionando igual que en el navegador, pero
 * sin necesitar red.
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String BASE = "https://" + HOST + "/assets/www/index.html";
    private static final int PEDIR_ARCHIVO = 1001;
    private static final int PEDIR_NOTIFICACIONES = 1002;

    private WebView web;
    private ValueCallback<Uri[]> callbackArchivos;
    private Uri uriFoto;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);

        final WebViewAssetLoader cargador = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        setContentView(web);

        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setDatabaseEnabled(true);
        ajustes.setAllowFileAccess(false);
        ajustes.setAllowContentAccess(true);
        ajustes.setSupportZoom(false);
        ajustes.setMediaPlaybackRequiresUserGesture(false);

        /* Que el tema oscuro del móvil llegue a prefers-color-scheme. */
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(ajustes, true);
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView vista, WebResourceRequest peticion) {
                return cargador.shouldInterceptRequest(peticion.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView vista, WebResourceRequest peticion) {
                Uri destino = peticion.getUrl();
                if (HOST.equals(destino.getHost())) return false;

                /* blob: y data: solo existen dentro de la página: ninguna app
                   del móvil sabe abrirlos, e intentarlo no hacía nada. Los
                   adjuntos van por el puente, no por aquí. */
                String esquema = destino.getScheme();
                if ("blob".equals(esquema) || "data".equals(esquema) || "about".equals(esquema)) {
                    return true;
                }

                abrirFuera(destino);   // tel:, whatsapp, mapas, Google Calendar…
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView vista, ValueCallback<Uri[]> callback,
                                             FileChooserParams parametros) {
                return elegirArchivo(callback, parametros);
            }
        });

        web.addJavascriptInterface(new PuenteNativo(this), "AvisosNativo");
        web.loadUrl(BASE + rutaDe(getIntent()));

        pedirNotificaciones();
        arrancarCorreoSiHayCuenta();
    }

    /** Ejecuta JavaScript en la página; lo usa el puente para contestar. */
    public void ejecutar(String javascript) {
        if (web != null) web.evaluateJavascript(javascript, null);
    }

    private void arrancarCorreoSiHayCuenta() {
        Cuenta cuenta = Cuenta.cargar(this);
        if (cuenta != null && cuenta.valida()) ServicioCorreo.arrancar(this);
    }

    /* Sin este permiso el servicio sigue funcionando, pero el usuario no ve
       ni su estado ni los correos nuevos. */
    private void pedirNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, PEDIR_NOTIFICACIONES);
    }

    private final ServicioCorreo.Oyente oyenteCorreo = new ServicioCorreo.Oyente() {
        @Override
        public void nuevoCorreo(int cuantos) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { ejecutar("window.Correo && Correo.alLlegar();"); }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        ServicioCorreo.escuchar(oyenteCorreo);
        ejecutar("window.App && App.recogerCorreo && App.recogerCorreo();");
    }

    @Override
    protected void onPause() {
        ServicioCorreo.dejarDeEscuchar(oyenteCorreo);
        super.onPause();
    }

    /**
     * El widget y los accesos directos abren la app en una pantalla concreta.
     * El widget la pasa como extra; los atajos del icono, en el URI, porque
     * los atajos estáticos no admiten extras en XML.
     */
    private String rutaDe(Intent intent) {
        if (intent == null) return "";

        String ruta = intent.getStringExtra("ruta");
        if (ruta != null && ruta.startsWith("#/")) return ruta;

        Uri datos = intent.getData();
        if (datos != null && "avisos".equals(datos.getScheme())) {
            String destino = datos.getHost();
            if ("nuevo".equals(destino)) return "#/nuevo";
            if ("agenda".equals(destino)) return "#/agenda";
        }
        return "";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String ruta = rutaDe(intent);
        if (!ruta.isEmpty() && web != null) {
            web.evaluateJavascript("location.hash=" + comillas(ruta) + ";", null);
        }
    }

    private static String comillas(String texto) {
        return "'" + texto.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private void abrirFuera(Uri destino) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, destino)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            // Sin app capaz de abrirlo: no hay nada que hacer.
        }
    }

    // ---------- fotos ----------

    private boolean elegirArchivo(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams parametros) {
        if (callbackArchivos != null) callbackArchivos.onReceiveValue(null);
        callbackArchivos = callback;
        uriFoto = null;

        Intent galeria = new Intent(Intent.ACTION_GET_CONTENT);
        galeria.setType("image/*");
        galeria.addCategory(Intent.CATEGORY_OPENABLE);
        if (parametros != null
                && parametros.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            galeria.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        Intent seleccion = Intent.createChooser(galeria, getString(R.string.anadir_foto));
        Intent camara = intentCamara();
        if (camara != null) {
            seleccion.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camara});
        }

        try {
            startActivityForResult(seleccion, PEDIR_ARCHIVO);
            return true;
        } catch (ActivityNotFoundException e) {
            callbackArchivos = null;
            return false;
        }
    }

    private Intent intentCamara() {
        Intent camara = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camara.resolveActivity(getPackageManager()) == null) return null;
        try {
            File carpeta = new File(getCacheDir(), "compartidos");
            if (!carpeta.exists() && !carpeta.mkdirs()) return null;
            File destino = new File(carpeta, "foto-" + System.currentTimeMillis() + ".jpg");
            uriFoto = FileProvider.getUriForFile(this, getPackageName() + ".archivos", destino);
            camara.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
            camara.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return camara;
        } catch (Exception e) {
            uriFoto = null;
            return null;
        }
    }

    @Override
    protected void onActivityResult(int peticion, int resultado, Intent datos) {
        if (peticion != PEDIR_ARCHIVO) {
            super.onActivityResult(peticion, resultado, datos);
            return;
        }
        if (callbackArchivos == null) return;

        Uri[] elegidos = null;
        if (resultado == RESULT_OK) {
            if (datos == null || (datos.getData() == null && datos.getClipData() == null)) {
                if (uriFoto != null) elegidos = new Uri[]{uriFoto};   // viene de la cámara
            } else if (datos.getClipData() != null) {
                int n = datos.getClipData().getItemCount();
                elegidos = new Uri[n];
                for (int i = 0; i < n; i++) elegidos[i] = datos.getClipData().getItemAt(i).getUri();
            } else {
                elegidos = new Uri[]{datos.getData()};
            }
        }
        callbackArchivos.onReceiveValue(elegidos);
        callbackArchivos = null;
        uriFoto = null;
    }

    // ---------- navegación ----------

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("AvisosNativo");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
