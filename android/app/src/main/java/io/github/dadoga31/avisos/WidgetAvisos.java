package io.github.dadoga31.avisos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Widget de la pantalla de inicio: un botón grande para crear un aviso sin
 * abrir nada más, y los contadores del día que deja la app al usarla.
 */
public class WidgetAvisos extends AppWidgetProvider {

    static final String PREFS = "avisos";
    static final String CLAVE_DIA = "dia";
    static final String CLAVE_HOY = "hoy";
    static final String CLAVE_VENCIDOS = "vencidos";
    static final String CLAVE_ABIERTOS = "abiertos";
    static final String CLAVE_HECHOS = "hechos";

    @Override
    public void onUpdate(Context contexto, AppWidgetManager gestor, int[] ids) {
        for (int id : ids) pintar(contexto, gestor, id);
    }

    static void actualizarTodos(Context contexto) {
        AppWidgetManager gestor = AppWidgetManager.getInstance(contexto);
        int[] ids = gestor.getAppWidgetIds(new ComponentName(contexto, WidgetAvisos.class));
        for (int id : ids) pintar(contexto, gestor, id);
    }

    private static void pintar(Context contexto, AppWidgetManager gestor, int id) {
        RemoteViews vista = new RemoteViews(contexto.getPackageName(), R.layout.widget_avisos);
        SharedPreferences p = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String dia = p.getString(CLAVE_DIA, "");

        if (!hoy.equals(dia)) {
            /* Los contadores son de otro día: mejor decirlo que mentir. */
            vista.setTextViewText(R.id.widget_resumen, contexto.getString(R.string.widget_sin_datos));
        } else {
            int deHoy = p.getInt(CLAVE_HOY, 0);
            int vencidos = p.getInt(CLAVE_VENCIDOS, 0);
            int hechos = p.getInt(CLAVE_HECHOS, 0);
            StringBuilder texto = new StringBuilder();
            texto.append(deHoy).append(" para hoy");
            if (vencidos > 0) texto.append(" · ").append(vencidos).append(vencidos == 1 ? " vencido" : " vencidos");
            if (hechos > 0) texto.append(" · ").append(hechos).append(" hecho").append(hechos == 1 ? "" : "s");
            vista.setTextViewText(R.id.widget_resumen, texto.toString());
        }

        vista.setOnClickPendingIntent(R.id.widget_nuevo, intentRuta(contexto, "#/nuevo", 1));
        vista.setOnClickPendingIntent(R.id.widget_cabecera, intentRuta(contexto, "#/agenda", 2));

        gestor.updateAppWidget(id, vista);
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
