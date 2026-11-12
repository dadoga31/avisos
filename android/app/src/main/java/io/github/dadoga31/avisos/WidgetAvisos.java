package io.github.dadoga31.avisos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Widget de la pantalla de inicio: lo de hoy de un vistazo (por hacer,
 * vencidos, hechos), el siguiente aviso (abre su ficha) y los botones
 * «Nuevo aviso» y «Agenda». Los datos los deja la app con
 * AvisosApp.publicarResumen tras cada cambio.
 */
public class WidgetAvisos extends AppWidgetProvider {

    static final String PREFS = "avisos";
    static final String CLAVE_DIA = "dia";
    static final String CLAVE_HOY = "hoy";
    static final String CLAVE_VENCIDOS = "vencidos";
    static final String CLAVE_ABIERTOS = "abiertos";
    static final String CLAVE_HECHOS = "hechos";
    static final String CLAVE_SIGUIENTE_ID = "siguienteId";
    static final String CLAVE_SIGUIENTE_CABECERA = "siguienteCabecera";
    static final String CLAVE_SIGUIENTE_TEXTO = "siguienteTexto";

    private static final Locale ES = new Locale("es", "ES");

    @Override
    public void onUpdate(final Context contexto, AppWidgetManager gestor, int[] ids) {
        RemoteViews vista = vistas(contexto);
        for (int id : ids) gestor.updateAppWidget(id, vista);
        /* Android lo refresca cada 30 minutos: los contadores pueden ser de
           ayer, así que la app los recalcula desde la base de datos. */
        final PendingResult pendiente = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AvisosApp.recalcular(contexto);
                } finally {
                    pendiente.finish();
                }
            }
        }, "widget").start();
    }

    static void actualizarTodos(Context contexto) {
        AppWidgetManager gestor = AppWidgetManager.getInstance(contexto);
        int[] ids = gestor.getAppWidgetIds(new ComponentName(contexto, WidgetAvisos.class));
        if (ids.length == 0) return;
        RemoteViews vista = vistas(contexto);
        for (int id : ids) gestor.updateAppWidget(id, vista);
    }

    /** El widget pintado con lo último que dejó la app (también lo usan las capturas). */
    static RemoteViews vistas(Context contexto) {
        RemoteViews v = new RemoteViews(contexto.getPackageName(), R.layout.widget_avisos);
        SharedPreferences p = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        Date ahora = new Date();
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(ahora);
        String dia = new SimpleDateFormat("EEE d", ES).format(ahora).replace(".", "");
        v.setTextViewText(R.id.widget_fecha, "Hoy · " + dia);

        if (!hoy.equals(p.getString(CLAVE_DIA, ""))) {
            /* Los contadores son de otro día: mejor decirlo que mentir. */
            v.setTextViewText(R.id.widget_numero, "–");
            v.setTextViewText(R.id.widget_etiqueta, contexto.getString(R.string.widget_sin_datos));
            v.setViewVisibility(R.id.widget_hechos, View.GONE);
            v.setViewVisibility(R.id.widget_vencidos, View.GONE);
            v.setViewVisibility(R.id.widget_siguiente, View.GONE);
        } else {
            int deHoy = p.getInt(CLAVE_HOY, 0);
            int vencidos = p.getInt(CLAVE_VENCIDOS, 0);
            int hechos = p.getInt(CLAVE_HECHOS, 0);
            v.setTextViewText(R.id.widget_numero, String.valueOf(deHoy));
            v.setTextViewText(R.id.widget_etiqueta, deHoy == 0 && hechos > 0 ? "todo hecho hoy" : "por hacer hoy");
            if (hechos > 0) {
                v.setTextViewText(R.id.widget_hechos, hechos == 1 ? "1 hecho" : hechos + " hechos");
                v.setViewVisibility(R.id.widget_hechos, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.widget_hechos, View.GONE);
            }
            if (vencidos > 0) {
                v.setTextViewText(R.id.widget_vencidos, vencidos == 1 ? "1 vencido" : vencidos + " vencidos");
                v.setViewVisibility(R.id.widget_vencidos, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.widget_vencidos, View.GONE);
            }
            String sigId = p.getString(CLAVE_SIGUIENTE_ID, "");
            if (!sigId.isEmpty()) {
                v.setTextViewText(R.id.widget_siguiente_cabecera, p.getString(CLAVE_SIGUIENTE_CABECERA, ""));
                v.setTextViewText(R.id.widget_siguiente_texto, p.getString(CLAVE_SIGUIENTE_TEXTO, ""));
                v.setViewVisibility(R.id.widget_siguiente, View.VISIBLE);
                v.setOnClickPendingIntent(R.id.widget_siguiente, intentRuta(contexto, "#/aviso/" + sigId, 3));
            } else {
                v.setViewVisibility(R.id.widget_siguiente, View.GONE);
            }
        }

        v.setOnClickPendingIntent(R.id.widget_nuevo, intentRuta(contexto, "#/nuevo", 1));
        v.setOnClickPendingIntent(R.id.widget_agenda, intentRuta(contexto, "#/agenda", 2));
        v.setOnClickPendingIntent(R.id.widget_cabecera, intentRuta(contexto, "#/agenda", 2));
        return v;
    }

    private static PendingIntent intentRuta(Context contexto, String ruta, int codigo) {
        Intent intento = new Intent(contexto, MainActivity.class);
        intento.setAction(Intent.ACTION_VIEW);
        intento.putExtra("ruta", ruta);
        intento.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(contexto, codigo, intento,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
