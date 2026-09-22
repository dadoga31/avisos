package io.github.dadoga31.avisos.correo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fechas en ISO 8601 para pasárselas a la web. Sin java.time, que en
 * Android 7 (minSdk 24) no está disponible.
 */
final class Fechas {

    private Fechas() { }

    static String iso(Date fecha) {
        if (fecha == null) return "";
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formato.format(fecha);
    }

    static String iso(long milis) {
        return milis > 0 ? iso(new Date(milis)) : "";
    }
}
