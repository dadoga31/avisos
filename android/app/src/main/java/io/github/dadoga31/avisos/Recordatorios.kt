package io.github.dadoga31.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Recordatorio antes de cada cita: una alarma exacta por aviso (mío o sin
 * asignar, con fecha y hora). Se reprograma todo tras cada cambio de datos,
 * al arrancar el móvil, al cambiar la hora y al actualizar la app.
 *
 * Exacta porque «en 20 min» tiene que llegar a su hora: la app declara
 * USE_EXACT_ALARM (se concede al instalar; no está en Google Play). Si aun
 * así no se puede, va inexacta y Android la puede retrasar unos minutos.
 */
object Recordatorios {
    const val CANAL = "recordatorios"
    const val RECORDAR = "io.github.dadoga31.avisos.RECORDAR"
    const val EMPEZAR = "io.github.dadoga31.avisos.EMPEZAR"
    const val MAS_TARDE = "io.github.dadoga31.avisos.MAS_TARDE"
    /** «Avisar en 15 min». */
    const val MINUTOS_MAS_TARDE = 15L
    private const val PREFS = "recordatorios"

    fun idNotificacion(avisoId: String) = 3_000_000 + (avisoId.hashCode() and 0x7fffffff) % 1_000_000

    /**
     * Una alarma por aviso: el Uri las distingue (los extras no cuentan para
     * Android). La de «Avisar en 15 min» va aparte: si compartiera Uri con la
     * normal, el siguiente reprogramar la borraría por estar ya pasada.
     */
    private fun intento(ctx: Context, accion: String, avisoId: String, tipo: String = "recordatorio") =
        Intent(ctx, ReceptorRecordatorio::class.java).setAction(accion).setData(Uri.parse("avisos://$tipo/$avisoId"))
            .putExtra("id", avisoId)

    /** Programa las alarmas que tocan y quita las que ya no. */
    @Synchronized
    fun reprogramar(ctx: Context, avisos: List<Aviso>, minutos: Int, ahora: Instant = Instant.now()) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val antes = prefs.getStringSet("ids", emptySet()) ?: emptySet()
        val pendientes = Reglas.recordatoriosPendientes(avisos, minutos, ahora)
        val ahoraIds = pendientes.map { it.first.id }.toSet()
        (antes - ahoraIds).forEach { id -> cancelar(ctx, am, id) }
        pendientes.forEach { (a, cuando) -> programar(ctx, am, a.id, cuando, posponer = false) }
        prefs.edit().putStringSet("ids", ahoraIds).apply()
    }

    private fun cancelar(ctx: Context, am: AlarmManager, id: String) {
        PendingIntent.getBroadcast(ctx, 0, intento(ctx, RECORDAR, id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            am.cancel(it)
            it.cancel()
        }
    }

    fun programar(ctx: Context, am: AlarmManager, id: String, cuando: Instant, posponer: Boolean) {
        val i = intento(ctx, RECORDAR, id, if (posponer) "posponer" else "recordatorio")
            .putExtra("cuando", cuando.toEpochMilli()).putExtra("posponer", posponer)
        val pi = PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exacta) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando.toEpochMilli(), pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando.toEpochMilli(), pi)
    }

    private fun crearCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CANAL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Antes de cada cita, con Ir y Empezar"
            }
        )
    }

    fun notificar(ctx: Context, a: Aviso, ahora: Instant = Instant.now()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        crearCanal(ctx)
        val n = idNotificacion(a.id)
        val (titulo, texto) = Reglas.textoRecordatorio(a, ahora)
        val detalle = listOf(texto, a.cliente.direccion).filter { it.isNotBlank() }.joinToString("\n")
        fun accion(accion: String, codigo: Int) = PendingIntent.getBroadcast(
            ctx, codigo, intento(ctx, accion, a.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ctx.getColor(R.color.acento))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(Notificaciones.abrir(ctx, "#/aviso/${a.id}", n))
        if (a.cliente.direccion.isNotBlank()) {
            val ir = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a.cliente.direccion)))
            b.addAction(0, "Ir", PendingIntent.getActivity(ctx, n, ir, PendingIntent.FLAG_IMMUTABLE))
        }
        b.addAction(0, "Empezar", accion(EMPEZAR, 1))
        b.addAction(0, "Avisar en $MINUTOS_MAS_TARDE min", accion(MAS_TARDE, 2))
        try {
            NotificationManagerCompat.from(ctx).notify(n, b.build())
        } catch (_: SecurityException) {
        }
    }
}

/** La alarma del recordatorio y los botones de su notificación. */
class ReceptorRecordatorio : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val app = ctx.applicationContext as AvisosApp
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nm = NotificationManagerCompat.from(ctx)
                when (intent.action) {
                    Recordatorios.RECORDAR -> {
                        val a = app.repositorio.porId(id) ?: return@launch
                        /* La alarma puede ser vieja: solo avisa si el aviso sigue
                           pidiendo recordatorio a esa hora (o si se pospuso a mano). */
                        val esperado = Reglas.momentoRecordatorio(a, app.ajustes.actual.recordatorio)?.toEpochMilli()
                        val posponer = intent.getBooleanExtra("posponer", false)
                        val sigue = a.abierto && a.estadoE != Estado.EN_CURSO
                        if (sigue && (posponer || esperado == intent.getLongExtra("cuando", -1))) Recordatorios.notificar(ctx, a)
                    }
                    Recordatorios.EMPEZAR -> {
                        app.repositorio.cambiarEstado(id, Estado.EN_CURSO)
                        nm.cancel(Recordatorios.idNotificacion(id))
                    }
                    Recordatorios.MAS_TARDE -> {
                        val am = ctx.getSystemService(android.app.AlarmManager::class.java)
                        if (am != null) {
                            Recordatorios.programar(ctx, am, id, Instant.now().plusSeconds(Recordatorios.MINUTOS_MAS_TARDE * 60), posponer = true)
                        }
                        nm.cancel(Recordatorios.idNotificacion(id))
                    }
                }
            } finally {
                pendiente.finish()
            }
        }
    }
}

/** Al arrancar el móvil, al cambiar la hora o la zona y al actualizar la app, las alarmas se pierden: se vuelven a poner. */
class ReceptorHora : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext as AvisosApp
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repositorio.avisarCambio()
            } finally {
                pendiente.finish()
            }
        }
    }
}
