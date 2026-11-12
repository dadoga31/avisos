package io.github.dadoga31.avisos

import android.Manifest
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
import io.github.dadoga31.avisos.datos.Prioridad
import io.github.dadoga31.avisos.datos.Tecnico
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Una notificación por aviso nuevo, con la silueta del icono en naranja y
 * botones para no tener que abrir la app: Me lo quedo, Llamar, Ver.
 */
object Notificaciones {
    const val CANAL = "avisos_nuevos"
    private const val GRUPO = "avisos"
    private const val ID_RESUMEN = 2

    fun idDe(avisoId: String) = 1000 + (avisoId.hashCode() and 0x7fffffff) % 1_000_000

    private fun crearCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CANAL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Avisos nuevos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cuando un correo se convierte en aviso"
            }
        )
    }

    private fun permitido(ctx: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Abre la app en una ruta («#/aviso/<id>»). */
    fun abrir(ctx: Context, ruta: String, codigo: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra("ruta", ruta)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(ctx, codigo, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun avisosNuevos(ctx: Context, avisos: List<Aviso>) {
        if (avisos.isEmpty() || !permitido(ctx)) return
        crearCanal(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        try {
            avisos.take(8).forEach { nm.notify(idDe(it.id), construir(ctx, it)) }
            if (avisos.size > 1) {
                nm.notify(
                    ID_RESUMEN,
                    NotificationCompat.Builder(ctx, CANAL)
                        .setSmallIcon(R.drawable.ic_notificacion)
                        .setColor(ctx.getColor(R.color.acento))
                        .setContentTitle("${avisos.size} avisos nuevos")
                        .setContentText("Desde el correo de la empresa")
                        .setGroup(GRUPO).setGroupSummary(true).setAutoCancel(true)
                        .setContentIntent(abrir(ctx, "#/agenda", ID_RESUMEN))
                        .build()
                )
            }
        } catch (_: SecurityException) {
            // El usuario quitó el permiso entre medias: no pasa nada, el aviso ya está en la Agenda.
        }
    }

    fun construir(ctx: Context, a: Aviso): android.app.Notification {
        val id = idDe(a.id)
        val urgente = a.prioridadE == Prioridad.URGENTE
        val titulo = (if (urgente) "Nuevo aviso urgente" else "Nuevo aviso") +
            if (a.cliente.nombre.isNotBlank()) " · ${a.cliente.nombre}" else ""
        val detalle = listOf(a.titulo, a.descripcion.take(240)).filter { it.isNotBlank() }.joinToString("\n")
        val ver = abrir(ctx, "#/aviso/${a.id}", id)
        val b = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ctx.getColor(R.color.acento))
            .setContentTitle(titulo)
            .setContentText(a.titulo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detalle))
            .setPriority(if (urgente) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setGroup(GRUPO)
            .setAutoCancel(true)
            .setContentIntent(ver)
        if (a.asignadoA.isEmpty()) {
            val quedar = Intent(ctx, ReceptorAcciones::class.java).setAction(ReceptorAcciones.ME_LO_QUEDO).putExtra("id", a.id)
            b.addAction(0, "Me lo quedo", PendingIntent.getBroadcast(ctx, id, quedar, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        if (a.cliente.telefono.isNotBlank()) {
            val llamar = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + a.cliente.telefono))
            b.addAction(0, "Llamar", PendingIntent.getActivity(ctx, id + 1, llamar, PendingIntent.FLAG_IMMUTABLE))
        }
        b.addAction(0, "Ver", ver)
        return b.build()
    }
}

/** Botones de las notificaciones que no abren la app. */
class ReceptorAcciones : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != ME_LO_QUEDO) return
        val id = intent.getStringExtra("id") ?: return
        val pendiente = goAsync()
        val repo = (ctx.applicationContext as AvisosApp).repositorio
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.porId(id)?.let { a ->
                    /* Con fecha y técnico ya está programado. */
                    val estado = if (a.estadoE == Estado.PENDIENTE && a.fecha.isNotEmpty()) Estado.PROGRAMADO.id else a.estado
                    repo.guardar(a.copy(asignadoA = Tecnico.YO, estado = estado))
                }
                NotificationManagerCompat.from(ctx).cancel(Notificaciones.idDe(id))
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ME_LO_QUEDO = "io.github.dadoga31.avisos.ME_LO_QUEDO"
    }
}
