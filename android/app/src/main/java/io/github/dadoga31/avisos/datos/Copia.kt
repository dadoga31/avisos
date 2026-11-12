package io.github.dadoga31.avisos.datos

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.Base64

/** Un fichero dentro de la copia (en la web se llamaban «fotos», aunque fueran PDF). */
class FicheroCopia(val id: String, val avisoId: String, val nombre: String, val mime: String, val ts: String, val datos: ByteArray)

/** Lo que trae una copia, ya pasado al modelo de la app. */
class ContenidoCopia(
    val avisos: List<Aviso>,
    val tecnicos: List<Tecnico>,
    val ficheros: List<FicheroCopia>,
    val prefijoRef: String?,
    val contadorRef: Int,
    val correoIgnorados: List<String>
)

/**
 * Copia de seguridad en el formato de la app web («avisos-backup»): un JSON
 * con avisos, técnicos, ajustes sin secretos y los ficheros en base64. La
 * versión 2 es la nativa; la 1 es la de la web y se lee igual, adaptando lo
 * que cambió (horas en lista, correo de origen, el técnico «Yo»).
 */
object CopiaSeguridad {
    const val FORMATO = "avisos-backup"

    /** Colores de técnico de la web, en el mismo orden que los índices de aquí. */
    private val COLORES_WEB = listOf("#c2410c", "#0e7490", "#15803d", "#6d28d9", "#b45309", "#be123c", "#0f766e", "#4338ca")

    fun exportar(
        avisos: List<Aviso>,
        tecnicos: List<Tecnico>,
        ajustes: Ajustes,
        leer: (Adjunto) -> ByteArray?,
        ahora: Instant = Instant.now()
    ): String {
        val fotos = JSONArray()
        val av = JSONArray()
        avisos.forEach { a ->
            av.put(avisoAJson(a))
            a.adjuntos.forEach { adj ->
                val bytes = leer(adj) ?: return@forEach
                fotos.put(
                    JSONObject().put("id", adj.id).put("avisoId", a.id).put("ts", a.actualizado)
                        .put("nombre", adj.nombre).put("mime", adj.mime).put("origen", "dispositivo")
                        .put("data", "data:${adj.mime};base64," + Base64.getEncoder().encodeToString(bytes))
                )
            }
        }
        return JSONObject()
            .put("formato", FORMATO).put("version", 2).put("exportado", ahora.toString())
            .put(
                "ajustes", JSONObject().put("prefijoRef", ajustes.prefijoRef).put("contadorRef", ajustes.contadorRef)
                    .put("recordatorio", ajustes.recordatorio).put("correoIgnorados", JSONArray(ajustes.correoIgnorados))
            )
            .put("tecnicos", JSONArray().apply {
                tecnicos.forEach {
                    put(JSONObject().put("id", it.id).put("nombre", it.nombre).put("telefono", it.telefono)
                        .put("color", COLORES_WEB[((it.color % 8) + 8) % 8]))
                }
            })
            .put("avisos", av)
            .put("fotos", fotos)
            .toString()
    }

    private fun avisoAJson(a: Aviso) = JSONObject()
        .put("id", a.id).put("ref", a.ref).put("titulo", a.titulo).put("tipo", a.tipo).put("sistema", a.sistema)
        .put("prioridad", a.prioridad).put("estado", a.estado).put("origen", a.origen).put("correoId", a.correoId)
        .put("cliente", JSONObject().put("nombre", a.cliente.nombre).put("direccion", a.cliente.direccion)
            .put("telefono", a.cliente.telefono).put("contacto", a.cliente.contacto))
        .put("fecha", a.fecha).put("hora", a.hora).put("duracion", a.duracion).put("asignadoA", a.asignadoA)
        .put("descripcion", a.descripcion)
        .put("notas", JSONArray().apply { a.notas.forEach { put(JSONObject().put("id", it.id).put("ts", it.ts).put("texto", it.texto)) } })
        .put("horas", a.horas).put("creado", a.creado).put("actualizado", a.actualizado)
        .put("empezado", a.empezado).put("cerrado", a.cerrado)

    /** Lee una copia (nativa o de la web). Lanza IllegalArgumentException si no lo es. */
    fun leer(texto: String): ContenidoCopia {
        val o = runCatching { JSONObject(texto) }.getOrNull()
        require(o != null && o.optString("formato") == FORMATO) { "El archivo no es una copia de seguridad de Avisos" }

        /* El «Yo» de la web tiene un id cualquiera: se junta con el «Yo» de aquí. */
        val renombrar = mutableMapOf<String, String>()
        val tecnicos = mutableListOf<Tecnico>()
        val lt = o.optJSONArray("tecnicos") ?: JSONArray()
        for (i in 0 until lt.length()) {
            val t = lt.optJSONObject(i) ?: continue
            val id = t.optString("id")
            val nombre = t.optString("nombre").trim()
            if (id.isEmpty() || nombre.isEmpty()) continue
            if (nombre.equals("yo", ignoreCase = true) || id == Tecnico.YO) {
                renombrar[id] = Tecnico.YO
                continue
            }
            val color = when (val c = t.opt("color")) {
                is Number -> c.toInt()
                is String -> COLORES_WEB.indexOf(c.lowercase()).takeIf { it >= 0 } ?: i
                else -> i
            }
            tecnicos += Tecnico(id, nombre, t.optString("telefono"), color)
        }

        val avisos = mutableListOf<Aviso>()
        val la = o.optJSONArray("avisos") ?: JSONArray()
        for (i in 0 until la.length()) {
            val a = la.optJSONObject(i) ?: continue
            val id = a.optString("id")
            if (id.isEmpty()) continue
            val c = a.optJSONObject("cliente") ?: JSONObject()
            val notas = a.optJSONArray("notas") ?: JSONArray()
            /* Horas: número en la copia nativa, lista de partes en la web. */
            val horas = when (val h = a.opt("horas")) {
                is Number -> h.toDouble()
                is JSONArray -> (0 until h.length()).sumOf { h.optJSONObject(it)?.optDouble("horas", 0.0)?.takeIf { x -> !x.isNaN() } ?: 0.0 }
                else -> 0.0
            }
            /* La web guardaba el correo de origen aparte: se pasa a la clave que evita duplicados. */
            val correo = a.optJSONObject("correo")
            val correoId = a.optString("correoId").ifEmpty {
                when {
                    correo == null -> ""
                    correo.optString("messageId").isNotEmpty() -> "m:" + correo.optString("messageId")
                    correo.optString("uid").isNotEmpty() -> "u:" + correo.optString("uid")
                    else -> ""
                }
            }
            val materiales = a.optJSONArray("materiales")
            val textoMateriales = if (materiales == null || materiales.length() == 0) "" else
                "\n\nMateriales:\n" + (0 until materiales.length()).mapNotNull { j ->
                    materiales.optJSONObject(j)?.let { m -> "· ${m.optString("desc")} × ${m.optInt("cantidad", 1)}" }
                }.joinToString("\n")
            val asignado = a.optString("asignadoA").let { renombrar[it] ?: it }
            avisos += Aviso(
                id = id, ref = a.optString("ref"), titulo = a.optString("titulo"),
                tipo = Tipo.de(a.optString("tipo")).id, sistema = Sistema.de(a.optString("sistema")).id,
                prioridad = Prioridad.de(a.optString("prioridad")).id, estado = Estado.de(a.optString("estado")).id,
                origen = a.optString("origen", "manual").ifEmpty { "manual" }, correoId = correoId,
                cliente = Cliente(c.optString("nombre"), c.optString("direccion"), c.optString("telefono"), c.optString("contacto")),
                fecha = a.optString("fecha"), hora = a.optString("hora"), duracion = a.optString("duracion"),
                asignadoA = asignado, descripcion = a.optString("descripcion") + textoMateriales,
                notas = (0 until notas.length()).mapNotNull { j ->
                    notas.optJSONObject(j)?.let { n -> Nota(n.optString("id"), n.optString("ts"), n.optString("texto")) }
                },
                horas = horas, creado = a.optString("creado"), actualizado = a.optString("actualizado"),
                empezado = a.optString("empezado"), cerrado = a.optString("cerrado")
            )
        }

        val ficheros = mutableListOf<FicheroCopia>()
        val lf = o.optJSONArray("fotos") ?: JSONArray()
        for (i in 0 until lf.length()) {
            val f = lf.optJSONObject(i) ?: continue
            val data = f.optString("data")
            val coma = data.indexOf(',')
            if (!data.startsWith("data:") || coma < 0) continue
            val bytes = runCatching { Base64.getDecoder().decode(data.substring(coma + 1)) }.getOrNull() ?: continue
            val mime = f.optString("mime").ifEmpty { data.substring(5, coma).substringBefore(';') }
            ficheros += FicheroCopia(
                f.optString("id").ifEmpty { "f$i" }, f.optString("avisoId"),
                f.optString("nombre").ifEmpty { if (mime.startsWith("image/")) "foto.jpg" else "adjunto" }, mime, f.optString("ts"), bytes
            )
        }

        val aj = o.optJSONObject("ajustes") ?: JSONObject()
        val ign = aj.optJSONArray("correoIgnorados") ?: JSONArray()
        return ContenidoCopia(
            avisos, tecnicos, ficheros,
            aj.optString("prefijoRef").ifEmpty { null }, aj.optInt("contadorRef", 0),
            (0 until ign.length()).map { ign.optString(it) }.filter { it.isNotBlank() }
        )
    }
}
