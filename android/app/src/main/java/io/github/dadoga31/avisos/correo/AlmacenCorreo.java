package io.github.dadoga31.avisos.correo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Guarda en el móvil lo descargado del buzón hasta que la parte web lo
 * convierte en avisos. Nada de esto sale del dispositivo.
 */
public class AlmacenCorreo {

    private static final String TAG = "AvisosAlmacen";
    private static final String PREFS = "correo_estado";
    private static final int MAX_VISTOS = 2000;

    private final Context contexto;
    private final File carpeta;
    private final File adjuntos;
    private final File pendientes;

    public AlmacenCorreo(Context contexto) {
        this.contexto = contexto.getApplicationContext();
        this.carpeta = new File(this.contexto.getFilesDir(), "correo");
        this.adjuntos = new File(carpeta, "adj");
        this.pendientes = new File(carpeta, "pendientes.json");
        // noinspection ResultOfMethodCallIgnored
        adjuntos.mkdirs();
    }

    private SharedPreferences prefs() {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- mensajes pendientes ----------

    public synchronized JSONArray leerPendientes() {
        try {
            if (!pendientes.exists()) return new JSONArray();
            return new JSONArray(leerTexto(pendientes));
        } catch (Exception e) {
            Log.e(TAG, "Pendientes ilegibles", e);
            return new JSONArray();
        }
    }

    public synchronized void anadirPendientes(List<Mensaje> nuevos) {
        if (nuevos.isEmpty()) return;
        JSONArray lista = leerPendientes();

        /* Segunda línea de defensa contra avisos repetidos: lo que ya está
           esperando no se vuelve a encolar. */
        Set<String> yaEstan = new HashSet<>();
        for (int i = 0; i < lista.length(); i++) {
            JSONObject o = lista.optJSONObject(i);
            if (o == null) continue;
            String uid = o.optString("uid", "");
            String id = o.optString("messageId", "");
            if (!uid.isEmpty()) yaEstan.add("u:" + uid);
            if (!id.isEmpty()) yaEstan.add("m:" + id);
        }

        int anadidos = 0;
        for (Mensaje m : nuevos) {
            boolean repetido = (!m.uid.isEmpty() && yaEstan.contains("u:" + m.uid))
                    || (!m.messageId.isEmpty() && yaEstan.contains("m:" + m.messageId));
            if (repetido) {
                Log.i(TAG, "Mensaje ya en cola, no se duplica: " + m.uid);
                continue;
            }
            if (!m.uid.isEmpty()) yaEstan.add("u:" + m.uid);
            if (!m.messageId.isEmpty()) yaEstan.add("m:" + m.messageId);
            lista.put(m.aJSON());
            anadidos++;
        }

        if (anadidos > 0) escribirPendientes(lista);
    }

    /** Quita los que la web ya convirtió y borra sus adjuntos del disco. */
    public synchronized void marcarProcesados(Set<String> uids) {
        if (uids.isEmpty()) return;
        JSONArray lista = leerPendientes();
        JSONArray quedan = new JSONArray();
        for (int i = 0; i < lista.length(); i++) {
            JSONObject o = lista.optJSONObject(i);
            if (o == null) continue;
            if (uids.contains(o.optString("uid"))) borrarAdjuntosDe(o);
            else quedan.put(o);
        }
        escribirPendientes(quedan);
    }

    private void borrarAdjuntosDe(JSONObject mensaje) {
        JSONArray lista = mensaje.optJSONArray("adjuntos");
        if (lista == null) return;
        for (int i = 0; i < lista.length(); i++) {
            JSONObject a = lista.optJSONObject(i);
            if (a == null) continue;
            File f = ficheroAdjunto(a.optString("id"));
            if (f != null && f.exists()) {
                // noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }

    private void escribirPendientes(JSONArray lista) {
        try (FileOutputStream salida = new FileOutputStream(pendientes)) {
            salida.write(lista.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            Log.e(TAG, "No se pudo guardar pendientes", e);
        }
    }

    public synchronized int cuantosPendientes() {
        return leerPendientes().length();
    }

    // ---------- adjuntos ----------

    /** El id es el propio nombre del fichero; se sanea para no salir de la carpeta. */
    public File ficheroAdjunto(String id) {
        String limpio = String.valueOf(id).replaceAll("[^A-Za-z0-9._-]", "");
        if (limpio.isEmpty()) return null;
        return new File(adjuntos, limpio);
    }

    public long guardarAdjunto(String id, InputStream entrada) throws IOException {
        File destino = ficheroAdjunto(id);
        if (destino == null) throw new IOException("Identificador de adjunto no válido");
        long total = 0;
        try (FileOutputStream salida = new FileOutputStream(destino)) {
            byte[] buffer = new byte[16 * 1024];
            int leidos;
            while ((leidos = entrada.read(buffer)) > 0) {
                salida.write(buffer, 0, leidos);
                total += leidos;
            }
        }
        return total;
    }

    /** Devuelve un trozo del adjunto en base64, para pasarlo a la web. */
    public String trozoAdjunto(String id, long desde, int longitud) {
        File f = ficheroAdjunto(id);
        if (f == null || !f.exists() || desde >= f.length()) return "";
        int cuanto = (int) Math.min(longitud, f.length() - desde);
        byte[] buffer = new byte[cuanto];
        try (RandomAccessFile lector = new RandomAccessFile(f, "r")) {
            lector.seek(desde);
            lector.readFully(buffer);
        } catch (Exception e) {
            Log.e(TAG, "Adjunto ilegible", e);
            return "";
        }
        return Base64.encodeToString(buffer, Base64.NO_WRAP);
    }

    // ---------- control de lo ya visto ----------

    public synchronized Set<String> vistos() {
        return new HashSet<>(prefs().getStringSet("vistos", new HashSet<String>()));
    }

    public synchronized void anadirVistos(Set<String> uids) {
        Set<String> todos = new LinkedHashSet<>(prefs().getStringSet("vistos", new HashSet<String>()));
        todos.addAll(uids);
        /* No hace falta recordar para siempre: con los últimos basta para
           no volver a descargar lo mismo. */
        while (todos.size() > MAX_VISTOS) {
            String primero = todos.iterator().next();
            todos.remove(primero);
        }
        prefs().edit().putStringSet("vistos", todos).apply();
    }

    /** Pide que la próxima consulta vuelva a traer los últimos correos. */
    public void pedirRelectura(int cuantos) {
        prefs().edit().putInt("relectura", cuantos).apply();
    }

    public int relecturaPedida() {
        return prefs().getInt("relectura", 0);
    }

    public void relecturaHecha() {
        prefs().edit().remove("relectura").apply();
    }

    public synchronized void olvidarVistos() {
        prefs().edit().remove("vistos").apply();
    }

    // ---------- estado visible en la app ----------

    public void anotarSync(String error) {
        prefs().edit()
                .putLong("ultimaSync", System.currentTimeMillis())
                .putString("error", error == null ? "" : error)
                .putInt("caidas", 0)
                .apply();
    }

    /** Una conexión que se cae y se recupera no es una avería: solo se cuenta. */
    public int anotarCaida() {
        int n = prefs().getInt("caidas", 0) + 1;
        prefs().edit().putInt("caidas", n).apply();
        return n;
    }

    public void anotarActivo(boolean activo) {
        prefs().edit().putBoolean("activo", activo).apply();
    }

    public JSONObject estado(Cuenta cuenta) {
        JSONObject o = new JSONObject();
        try {
            if (cuenta != null) {
                o.put("usuario", cuenta.usuario);
                o.put("servidor", cuenta.servidor);
                o.put("protocolo", cuenta.protocolo);
                o.put("seguridad", cuenta.seguridad);
                o.put("puerto", cuenta.puerto);
            }
            long sync = prefs().getLong("ultimaSync", 0);
            if (sync > 0) o.put("ultimaSync", Fechas.iso(sync));
            o.put("error", prefs().getString("error", ""));
            o.put("activo", prefs().getBoolean("activo", false));
            o.put("caidas", prefs().getInt("caidas", 0));
            o.put("pendientes", cuantosPendientes());
        } catch (Exception ignorada) { }
        return o;
    }

    public synchronized void limpiarTodo() {
        JSONArray lista = leerPendientes();
        for (int i = 0; i < lista.length(); i++) {
            JSONObject o = lista.optJSONObject(i);
            if (o != null) borrarAdjuntosDe(o);
        }
        escribirPendientes(new JSONArray());
        prefs().edit().clear().apply();
    }

    private static String leerTexto(File f) throws IOException {
        try (FileInputStream entrada = new FileInputStream(f)) {
            byte[] datos = new byte[(int) f.length()];
            int leidos = 0;
            while (leidos < datos.length) {
                int n = entrada.read(datos, leidos, datos.length - leidos);
                if (n < 0) break;
                leidos += n;
            }
            return new String(datos, 0, leidos, "UTF-8");
        }
    }
}
