package io.github.dadoga31.avisos.correo

import android.content.Context
import android.util.Log
import io.github.dadoga31.avisos.AvisosApp
import io.github.dadoga31.avisos.Notificaciones
import io.github.dadoga31.avisos.datos.Adjunto
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Repositorio
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File

/**
 * Convierte en avisos los correos que el servicio deja en AlmacenCorreo.
 *
 * Lo llaman el servicio al llegar correo y la app al abrirse. Si dos pasadas
 * se solapan leen la misma lista y crean el mismo aviso dos veces (fallo ya
 * visto en la web): por eso solo hay una pasada a la vez, y cada correo se
 * mira contra los avisos ya guardados antes de crear nada.
 */
object ProcesadorCorreo {
    private const val TAG = "ProcesadorCorreo"
    private val cerrojo = Mutex()

    data class Resultado(val creados: List<Aviso>, val saltados: Int)

    suspend fun procesar(
        contexto: Context,
        repo: Repositorio,
        almacen: AlmacenCorreo,
        ignorados: Collection<String>
    ): Resultado = cerrojo.withLock {
        val mensajes = leer(almacen.leerPendientes())
        if (mensajes.isEmpty()) return@withLock Resultado(emptyList(), 0)
        val creados = mutableListOf<Aviso>()
        val tratados = mutableSetOf<String>()
        var saltados = 0
        for (m in mensajes) {
            try {
                if (ConversorCorreo.ignorado(m, ignorados) || repo.yaConvertido(m.claves)) {
                    saltados++
                    tratados += m.uid
                    continue
                }
                val id = Repositorio.nuevoId("av")
                val adjuntos = copiarAdjuntos(contexto.filesDir, almacen, id, m.adjuntos)
                creados += repo.guardar(ConversorCorreo.aAviso(m, id, repo.hoy()).copy(adjuntos = adjuntos))
                tratados += m.uid          // solo se da por hecho lo que se guardó
            } catch (e: Exception) {
                /* Sin marcarlo: se reintenta en la próxima pasada. El correo sigue en el servidor. */
                Log.e(TAG, "Correo no convertido: ${m.uid}", e)
            }
        }
        almacen.marcarProcesados(tratados.filter { it.isNotEmpty() }.toSet())
        Resultado(creados, saltados)
    }

    /** Desde el hilo del servicio: convierte y avisa. Devuelve los creados, o -1 si falló. */
    @JvmStatic
    fun procesarDesdeServicio(contexto: Context): Int = try {
        val app = contexto.applicationContext as AvisosApp
        val r = runBlocking { procesar(contexto, app.repositorio, AlmacenCorreo(contexto), app.ajustes.actual.correoIgnorados) }
        Notificaciones.avisosNuevos(contexto, r.creados)
        r.creados.size
    } catch (e: Exception) {
        Log.e(TAG, "No se pudieron convertir los correos", e)
        -1
    }

    fun leer(lista: JSONArray): List<MensajeCorreo> = (0 until lista.length()).mapNotNull { i ->
        val o = lista.optJSONObject(i) ?: return@mapNotNull null
        val adj = o.optJSONArray("adjuntos") ?: JSONArray()
        MensajeCorreo(
            uid = o.optString("uid"), messageId = o.optString("messageId"), de = o.optString("de"),
            deNombre = o.optString("deNombre"), asunto = o.optString("asunto"), fecha = o.optString("fecha"),
            texto = o.optString("texto"), html = o.optString("html"),
            adjuntos = (0 until adj.length()).mapNotNull { j ->
                adj.optJSONObject(j)?.let {
                    AdjuntoCorreo(it.optString("id"), it.optString("nombre"), it.optString("mime"), it.optLong("tam"))
                }
            }
        )
    }

    /** Copia los adjuntos a filesDir/adjuntos/<aviso>/, porque el almacén del correo los borra al procesar. */
    private fun copiarAdjuntos(base: File, almacen: AlmacenCorreo, avisoId: String, lista: List<AdjuntoCorreo>): List<Adjunto> {
        if (lista.isEmpty()) return emptyList()
        val carpeta = File(base, "adjuntos/$avisoId").apply { mkdirs() }
        return lista.mapIndexedNotNull { i, a ->
            val origen = almacen.ficheroAdjunto(a.id)
            if (origen == null || !origen.exists()) return@mapIndexedNotNull null
            val nombre = a.nombre.ifBlank { "adjunto" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val destino = File(carpeta, "$i-$nombre")
            origen.copyTo(destino, overwrite = true)
            Adjunto(
                id = Repositorio.nuevoId("f"), nombre = nombre,
                mime = a.mime.ifBlank { "application/octet-stream" }, tam = destino.length(),
                ruta = "adjuntos/$avisoId/${destino.name}"
            )
        }
    }
}
