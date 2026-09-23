package io.github.dadoga31.avisos;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import io.github.dadoga31.avisos.correo.AlmacenCorreo;
import io.github.dadoga31.avisos.correo.ClienteCorreo;
import io.github.dadoga31.avisos.correo.Cuenta;
import io.github.dadoga31.avisos.correo.ServicioCorreo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Lo que la web no puede hacer dentro de un WebView: entregar archivos al
 * sistema (las descargas por blob: no funcionan) y dejar los contadores que
 * pinta el widget de la pantalla de inicio.
 *
 * Ojo: estos métodos NO corren en el hilo de interfaz.
 */
public class PuenteNativo {

    private static final String TAG = "AvisosPuente";

    private final Activity actividad;
    private final AlmacenCorreo almacen;

    private File archivo;
    private FileOutputStream salida;
    private String mime;

    PuenteNativo(Activity actividad) {
        this.actividad = actividad;
        this.almacen = new AlmacenCorreo(actividad);
    }

    // ---------- archivos por trozos ----------

    @JavascriptInterface
    public void iniciarArchivo(String nombre, String tipo) {
        cancelarArchivo();
        try {
            File carpeta = new File(actividad.getCacheDir(), "compartidos");
            if (!carpeta.exists() && !carpeta.mkdirs()) throw new Exception("sin carpeta");
            limpiar(carpeta);
            archivo = new File(carpeta, limpiarNombre(nombre));
            salida = new FileOutputStream(archivo);
            mime = (tipo == null || tipo.isEmpty()) ? "application/octet-stream" : tipo;
        } catch (Exception e) {
            Log.e(TAG, "No se pudo abrir el archivo", e);
            cancelarArchivo();
        }
    }

    @JavascriptInterface
    public void anexarTrozo(String base64) {
        if (salida == null) return;
        try {
            salida.write(Base64.decode(base64, Base64.DEFAULT));
        } catch (Exception e) {
            Log.e(TAG, "No se pudo escribir el archivo", e);
            cancelarArchivo();
        }
    }

    /** modo: "abrir" (el sistema elige con qué) o "compartir" (hoja de compartir). */
    @JavascriptInterface
    public void finalizarArchivo(final String modo) {
        if (salida == null || archivo == null) return;
        final File terminado = archivo;
        final String tipo = mime;
        try {
            salida.flush();
            salida.close();
        } catch (Exception e) {
            Log.e(TAG, "No se pudo cerrar el archivo", e);
        }
        salida = null;
        archivo = null;

        actividad.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                entregar(terminado, tipo, modo);
            }
        });
    }

    @JavascriptInterface
    public void cancelarArchivo() {
        if (salida != null) {
            try { salida.close(); } catch (Exception ignorada) { }
        }
        salida = null;
        archivo = null;
    }

    private void entregar(File fichero, String tipo, String modo) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    actividad, actividad.getPackageName() + ".archivos", fichero);

            Intent intento;
            if ("abrir".equals(modo)) {
                intento = new Intent(Intent.ACTION_VIEW);
                intento.setDataAndType(uri, tipo);
            } else {
                intento = new Intent(Intent.ACTION_SEND);
                intento.setType(tipo);
                intento.putExtra(Intent.EXTRA_STREAM, uri);
                intento.putExtra(Intent.EXTRA_SUBJECT, fichero.getName());
            }
            intento.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent seleccion = Intent.createChooser(intento, fichero.getName());
            seleccion.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            actividad.startActivity(seleccion);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo entregar el archivo", e);
            Toast.makeText(actividad, R.string.error_archivo, Toast.LENGTH_LONG).show();
        }
    }

    private static String limpiarNombre(String nombre) {
        String limpio = (nombre == null ? "" : nombre).replaceAll("[^A-Za-z0-9._-]", "-");
        return limpio.isEmpty() ? "archivo" : limpio;
    }

    /** Los temporales ya entregados no hacen falta: se borran al empezar otro. */
    private static void limpiar(File carpeta) {
        File[] viejos = carpeta.listFiles();
        if (viejos == null) return;
        long limite = System.currentTimeMillis() - 6L * 60 * 60 * 1000;
        for (File f : viejos) {
            if (f.lastModified() < limite) {
                // noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    // ---------- correo ----------

    @JavascriptInterface
    public String correoEstado() {
        return almacen.estado(Cuenta.cargar(actividad)).toString();
    }

    @JavascriptInterface
    public void correoGuardarCuenta(String json) {
        try {
            Cuenta c = Cuenta.deJSON(new JSONObject(json));
            Cuenta anterior = Cuenta.cargar(actividad);
            boolean cambiaBuzon = anterior == null
                    || !anterior.usuario.equals(c.usuario)
                    || !anterior.servidor.equals(c.servidor)
                    || !anterior.protocolo.equals(c.protocolo);

            Cuenta.guardar(actividad, c);
            /* Buzón distinto: se olvida por dónde íbamos, para no mezclar
               la numeración de mensajes de dos servidores. */
            if (cambiaBuzon) almacen.limpiarTodo();
            ServicioCorreo.arrancar(actividad);
        } catch (Exception e) {
            Log.e(TAG, "Cuenta no válida", e);
        }
    }

    @JavascriptInterface
    public void correoBorrarCuenta() {
        ServicioCorreo.parar(actividad);
        Cuenta.borrar(actividad);
        almacen.limpiarTodo();
    }

    @JavascriptInterface
    public void correoSincronizarAhora() {
        ServicioCorreo.sincronizar(actividad);
    }

    /** La prueba tarda, así que se contesta luego llamando a la web. */
    @JavascriptInterface
    public void correoProbar(final String json) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject respuesta = new JSONObject();
                try {
                    Cuenta c = Cuenta.deJSON(new JSONObject(json));
                    if (c.clave.isEmpty()) {
                        Cuenta guardada = Cuenta.cargar(actividad);
                        if (guardada != null) c.clave = guardada.clave;   // «sin cambios»
                    }
                    String error = new ClienteCorreo(actividad, almacen).probar(c);
                    respuesta.put("ok", error == null);
                    if (error != null) respuesta.put("mensaje", error);
                } catch (Exception e) {
                    try {
                        respuesta.put("ok", false);
                        respuesta.put("mensaje", ClienteCorreo.explicar(e));
                    } catch (Exception ignorada) { }
                }
                contestarWeb("window.Correo && Correo.alProbar(" + comillas(respuesta.toString()) + ");");
            }
        }, "probar-correo").start();
    }

    /** Vuelve a mirar los últimos correos del buzón. */
    @JavascriptInterface
    public void correoRevisarDeNuevo(int cuantos) {
        almacen.pedirRelectura(cuantos > 0 ? cuantos : 20);
        ServicioCorreo.sincronizar(actividad);
    }

    @JavascriptInterface
    public String correoPendientes() {
        return almacen.leerPendientes().toString();
    }

    @JavascriptInterface
    public void correoMarcarProcesados(String jsonUids) {
        try {
            JSONArray lista = new JSONArray(jsonUids);
            Set<String> uids = new HashSet<>();
            for (int i = 0; i < lista.length(); i++) uids.add(lista.optString(i));
            almacen.marcarProcesados(uids);
        } catch (Exception e) {
            Log.e(TAG, "Lista de procesados no válida", e);
        }
    }

    @JavascriptInterface
    public String correoAdjuntoTrozo(String id, long desde, int longitud) {
        return almacen.trozoAdjunto(id, desde, longitud);
    }

    private void contestarWeb(final String javascript) {
        actividad.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (actividad instanceof io.github.dadoga31.avisos.MainActivity) {
                    ((io.github.dadoga31.avisos.MainActivity) actividad).ejecutar(javascript);
                }
            }
        });
    }

    private static String comillas(String texto) {
        return "'" + texto.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\n", "\\n").replace("\r", "") + "'";
    }

    // ---------- datos del widget ----------

    @JavascriptInterface
    public void guardarResumen(String json) {
        try {
            JSONObject o = new JSONObject(json);
            actividad.getSharedPreferences(WidgetAvisos.PREFS, Activity.MODE_PRIVATE)
                    .edit()
                    .putString(WidgetAvisos.CLAVE_DIA, o.optString("dia", ""))
                    .putInt(WidgetAvisos.CLAVE_HOY, o.optInt("hoy", 0))
                    .putInt(WidgetAvisos.CLAVE_VENCIDOS, o.optInt("vencidos", 0))
                    .putInt(WidgetAvisos.CLAVE_ABIERTOS, o.optInt("abiertos", 0))
                    .putInt(WidgetAvisos.CLAVE_HECHOS, o.optInt("hechosHoy", 0))
                    .apply();
            WidgetAvisos.actualizarTodos(actividad);
        } catch (Exception e) {
            Log.e(TAG, "Resumen no válido", e);
        }
    }
}
