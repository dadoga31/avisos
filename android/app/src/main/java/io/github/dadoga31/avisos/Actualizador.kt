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
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Cómo va la actualización, para la Agenda y Ajustes. */
sealed interface EstadoActualizacion {
    data object Nada : EstadoActualizacion
    data class Disponible(val version: Actualizador.Version) : EstadoActualizacion
    data class Descargando(val version: Actualizador.Version, val progreso: Float) : EstadoActualizacion
    data class Lista(val version: Actualizador.Version, val apk: File) : EstadoActualizacion
    data class Fallo(val version: Actualizador.Version, val mensaje: String) : EstadoActualizacion
}

/**
 * Actualizar sin pasar por el navegador. El workflow `apk.yml` publica en la
 * release «apk», junto a la APK, un `version.json` con su versionCode. La app
 * lo mira al abrirse y dos veces al día; si hay una más nueva avisa (una
 * notificación por versión y una franja en la Agenda), la descarga y abre el
 * instalador del sistema. Android no deja instalar sin que el usuario lo
 * confirme: se ahorra el navegador y la carpeta de Descargas, no el toque en
 * «Instalar». La primera vez pide permiso para instalar apps de Avisos.
 */
object Actualizador {
    private const val RELEASE = "https://github.com/dadoga31/avisos/releases/download/apk/"
    const val URL_VERSION = RELEASE + "version.json"
    const val URL_APK = RELEASE + "avisos.apk"
    const val CANAL = "actualizaciones"
    const val EXTRA = "actualizar"
    private const val ID_NOTIFICACION = 3
    private const val PREFS = "actualizacion"

    data class Version(val codigo: Int, val nombre: String, val notas: String = "")

    /** `{"versionCode": 13, "versionName": "3.6.0"}`; null si no se entiende. */
    fun leer(json: String): Version? = runCatching {
        val o = JSONObject(json)
        Version(o.getInt("versionCode"), o.getString("versionName"), o.optString("notas"))
    }.getOrNull()?.takeIf { it.codigo > 0 && it.nombre.isNotBlank() }

    fun hayNueva(v: Version?, actual: Int = BuildConfig.VERSION_CODE) = v != null && v.codigo > actual

    private fun conexion(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "Avisos-Android/" + BuildConfig.VERSION_NAME)
        }

    /** La versión publicada, o null sin red o si aún no hay version.json. Hilo de fondo. */
    fun consultar(): Version? = runCatching {
        val c = conexion(URL_VERSION)
        try {
            if (c.responseCode != 200) return null
            leer(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    private fun carpeta(ctx: Context) = File(ctx.cacheDir, "actualizacion")

    /**
     * Baja la APK a la caché avisando del avance (0..1) y comprueba que es de
     * esta app y más nueva que la instalada. Hilo de fondo.
     */
    fun descargar(ctx: Context, alAvanzar: (Float) -> Unit): File {
        val dir = carpeta(ctx).apply { mkdirs() }
        val parcial = File(dir, "avisos.apk.part")
        val final = File(dir, "avisos.apk")
        val c = conexion(URL_APK)
        try {
            if (c.responseCode != 200) error("El servidor contestó ${c.responseCode}")
            val total = c.contentLengthLong
            var leidos = 0L
            var ultimo = -1
            c.inputStream.use { entrada ->
                parcial.outputStream().use { salida ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = entrada.read(buf)
                        if (n < 0) break
                        salida.write(buf, 0, n)
                        leidos += n
                        if (total > 0) {
                            val pct = (leidos * 100 / total).toInt()
                            if (pct != ultimo) { ultimo = pct; alAvanzar(pct / 100f) }
                        }
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        final.delete()
        if (!parcial.renameTo(final)) error("No se pudo guardar la descarga")
        val codigo = versionDeApk(ctx, final)
        if (codigo == null) { final.delete(); error("La descarga no es una APK de Avisos") }
        if (codigo <= BuildConfig.VERSION_CODE) { final.delete(); error("La APK publicada no es más nueva que la instalada") }
        return final
    }

    /** versionCode de una APK si es de esta app; null si no lo es o no se puede leer. */
    @Suppress("DEPRECATION")
    fun versionDeApk(ctx: Context, apk: File): Int? {
        val info = runCatching { ctx.packageManager.getPackageArchiveInfo(apk.path, 0) }.getOrNull() ?: return null
        if (info.packageName != ctx.packageName) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt() else info.versionCode
    }

    /** Lo que quedó de antes: descargas a medias o APK ya instaladas. */
    fun limpiar(ctx: Context) {
        val dir = carpeta(ctx)
        File(dir, "avisos.apk.part").delete()
        val apk = File(dir, "avisos.apk")
        if (apk.exists() && (versionDeApk(ctx, apk) ?: 0) <= BuildConfig.VERSION_CODE) apk.delete()
    }

    /** Android 8+ pide permiso para que Avisos instale APK. */
    fun puedeInstalar(ctx: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun pedirPermiso(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Abre el instalador del sistema con la APK descargada. */
    fun instalar(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".archivos", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---------------------------------------------------------------- en segundo plano

    private fun pendiente(ctx: Context, flags: Int) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, ReceptorActualizacion::class.java), flags or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Mira si hay versión nueva dos veces al día, sin hora exacta (no gasta
     * batería). Si ya está programado no se toca, para no ir retrasándolo.
     */
    fun programar(ctx: Context) {
        if (pendiente(ctx, PendingIntent.FLAG_NO_CREATE) != null) return
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
            AlarmManager.INTERVAL_HALF_DAY, pendiente(ctx, PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    /** Una notificación por versión: si ya se avisó de esta, no se repite. */
    fun notificar(ctx: Context, v: Version) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt("notificada", 0) >= v.codigo) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CANAL, "Actualizaciones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Cuando hay una versión nueva de la app"
                }
            )
        }
        val abrir = PendingIntent.getActivity(
            ctx, ID_NOTIFICACION,
            Intent(ctx, MainActivity::class.java).putExtra(EXTRA, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            NotificationManagerCompat.from(ctx).notify(
                ID_NOTIFICACION,
                NotificationCompat.Builder(ctx, CANAL)
                    .setSmallIcon(R.drawable.ic_notificacion)
                    .setColor(ctx.getColor(R.color.acento))
                    .setContentTitle("Avisos ${v.nombre} disponible")
                    .setContentText("Toca para actualizar desde la app")
                    .setContentIntent(abrir)
                    .addAction(0, "Actualizar", abrir)
                    .setAutoCancel(true)
                    .build()
            )
            prefs.edit().putInt("notificada", v.codigo).apply()
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones: queda la franja de la Agenda.
        }
    }

    /** Quita la notificación (al abrir la app ya se ve la franja). */
    fun quitarNotificacion(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(ID_NOTIFICACION)
}

/** La comprobación de dos veces al día. */
class ReceptorActualizacion : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val app = ctx.applicationContext
        val fin = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val v = Actualizador.consultar()
                if (v != null && Actualizador.hayNueva(v)) Actualizador.notificar(app, v)
            } finally {
                fin.finish()
            }
        }
    }
}
