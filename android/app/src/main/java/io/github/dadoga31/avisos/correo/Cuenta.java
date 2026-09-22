package io.github.dadoga31.avisos.correo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

/**
 * Datos de la cuenta de correo. La contraseña vive en el almacén cifrado
 * de Android, no en las preferencias normales.
 */
public class Cuenta {

    private static final String TAG = "AvisosCuenta";
    private static final String FICHERO = "correo_seguro";

    public String usuario = "";
    public String clave = "";
    public String servidor = "";
    public String protocolo = "imap";    // imap | pop3
    public String seguridad = "ssl";     // ssl | starttls
    public int puerto = 993;

    public boolean valida() {
        return !usuario.isEmpty() && !servidor.isEmpty() && !clave.isEmpty() && puerto > 0;
    }

    public boolean esImap() { return !"pop3".equalsIgnoreCase(protocolo); }
    public boolean esSSL() { return !"starttls".equalsIgnoreCase(seguridad); }

    public static Cuenta deJSON(JSONObject o) {
        Cuenta c = new Cuenta();
        c.usuario = o.optString("usuario", "").trim();
        c.clave = o.optString("clave", "");
        c.servidor = o.optString("servidor", "").trim();
        c.protocolo = o.optString("protocolo", "imap");
        c.seguridad = o.optString("seguridad", "ssl");
        c.puerto = o.optInt("puerto", c.esImap() ? 993 : 995);
        return c;
    }

    private static SharedPreferences prefs(Context contexto) {
        try {
            MasterKey clave = new MasterKey.Builder(contexto)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    contexto, FICHERO, clave,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.e(TAG, "Sin almacén cifrado", e);
            return null;
        }
    }

    public static void guardar(Context contexto, Cuenta c) {
        SharedPreferences p = prefs(contexto);
        if (p == null) return;
        SharedPreferences.Editor ed = p.edit()
                .putString("usuario", c.usuario)
                .putString("servidor", c.servidor)
                .putString("protocolo", c.protocolo)
                .putString("seguridad", c.seguridad)
                .putInt("puerto", c.puerto);
        /* Si el usuario deja la contraseña en blanco al editar, se conserva
           la que ya había. */
        if (!c.clave.isEmpty()) ed.putString("clave", c.clave);
        ed.apply();
    }

    public static Cuenta cargar(Context contexto) {
        SharedPreferences p = prefs(contexto);
        if (p == null) return null;
        String usuario = p.getString("usuario", "");
        if (usuario.isEmpty()) return null;

        Cuenta c = new Cuenta();
        c.usuario = usuario;
        c.clave = p.getString("clave", "");
        c.servidor = p.getString("servidor", "");
        c.protocolo = p.getString("protocolo", "imap");
        c.seguridad = p.getString("seguridad", "ssl");
        c.puerto = p.getInt("puerto", 993);
        return c;
    }

    public static void borrar(Context contexto) {
        SharedPreferences p = prefs(contexto);
        if (p != null) p.edit().clear().apply();
    }
}
