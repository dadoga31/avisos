package io.github.dadoga31.avisos.correo

import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Estado
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** Un correo ya descargado por el servicio (lo que guarda AlmacenCorreo). */
data class MensajeCorreo(
    val uid: String = "",
    val messageId: String = "",
    val de: String = "",
    val deNombre: String = "",
    val asunto: String = "",
    /** ISO 8601. */
    val fecha: String = "",
    val texto: String = "",
    val html: String = "",
    val adjuntos: List<AdjuntoCorreo> = emptyList(),
    /** Message-ID al que contesta (In-Reply-To). */
    val enRespuestaA: String = "",
    /** Message-ID de los anteriores del hilo (References). */
    val referencias: List<String> = emptyList(),
    /** Thread-Index de Outlook (sus primeros 22 bytes son la conversación). */
    val hiloOutlook: String = ""
) {
    /** Clave con la que se reconoce: el Message-ID si lo hay y, si no, el UID. */
    val clave: String get() = if (messageId.isNotBlank()) "m:$messageId" else if (uid.isNotBlank()) "u:$uid" else ""
    val claves: List<String> get() = listOfNotNull(
        messageId.takeIf { it.isNotBlank() }?.let { "m:$it" }, uid.takeIf { it.isNotBlank() }?.let { "u:$it" }
    )
}

data class AdjuntoCorreo(val id: String, val nombre: String, val mime: String, val tam: Long)

/**
 * Correo → aviso. Traducción de assets/js/correo.js: mismas reglas y mismo
 * orden, para que un correo dé el mismo aviso que daba la app web.
 */
object ConversorCorreo {

    // ---------- texto del correo ----------

    private val ENTIDADES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "aacute" to "á", "eacute" to "é", "iacute" to "í", "oacute" to "ó", "uacute" to "ú",
        "ntilde" to "ñ", "Ntilde" to "Ñ", "uuml" to "ü", "iexcl" to "¡", "iquest" to "¿",
        "Aacute" to "Á", "Eacute" to "É", "Iacute" to "Í", "Oacute" to "Ó", "Uacute" to "Ú",
        "euro" to "€", "hellip" to "…", "mdash" to "—", "ndash" to "–", "laquo" to "«", "raquo" to "»"
    )

    fun decodificar(texto: String): String =
        Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(texto) { m ->
            val cuerpo = m.groupValues[1]
            if (cuerpo.startsWith("#")) {
                val n = if (cuerpo.length > 1 && (cuerpo[1] == 'x' || cuerpo[1] == 'X')) cuerpo.drop(2).toIntOrNull(16)
                else cuerpo.drop(1).toIntOrNull()
                if (n != null && Character.isValidCodePoint(n)) String(Character.toChars(n)) else m.value
            } else ENTIDADES[cuerpo] ?: m.value
        }

    fun htmlATexto(html: String): String {
        var t = html
        t = Regex("<(script|style)[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE).replace(t, " ")
        t = Regex("<!--[\\s\\S]*?-->").replace(t, " ")
        t = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE).replace(t, "\n")
        t = Regex("</(p|div|tr|li|h[1-6]|table|blockquote)>", RegexOption.IGNORE_CASE).replace(t, "\n")
        t = Regex("<li[^>]*>", RegexOption.IGNORE_CASE).replace(t, "· ")
        t = Regex("<[^>]+>").replace(t, "")
        t = decodificar(t)
        t = Regex("\\r\\n?").replace(t, "\n")
        t = Regex("[ \\t\\u00a0]+").replace(t, " ")
        t = Regex(" *\\n *").replace(t, "\n")
        t = Regex("\\n{3,}").replace(t, "\n\n")
        return t.trim()
    }

    /** Corta la cadena de respuestas anteriores: solo interesa lo último que han escrito. */
    fun quitarCitas(texto: String): String {
        val cortes = listOf(
            Regex("^-{2,}\\s*Mensaje original\\s*-{2,}", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            Regex("^-{2,}\\s*Original Message\\s*-{2,}", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            Regex("^El .{0,60}escribi[óo]:\\s*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            Regex("^On .{0,80}wrote:\\s*$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            Regex("^De:\\s.+\\nEnviado:", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)),
            Regex("^From:\\s.+\\nSent:", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        )
        val fin = cortes.mapNotNull { it.find(texto)?.range?.first }.minOrNull() ?: texto.length
        return texto.substring(0, fin).trim().ifEmpty { texto.trim() }
    }

    fun normalizarTexto(t: String): String {
        var s = Regex("\\r\\n?").replace(t, "\n")
        s = Regex("[ \\t\\u00a0]+").replace(s, " ")
        s = Regex(" *\\n *").replace(s, "\n")
        s = Regex("\\n{3,}").replace(s, "\n\n")
        return s.trim()
    }

    /** Con texto plano se usa tal cual: el limpiador de HTML se comería cosas como «<5 grados». */
    fun cuerpoLegible(m: MensajeCorreo): String {
        val plano = m.texto.trim()
        return if (plano.isNotEmpty()) quitarCitas(normalizarTexto(plano)) else quitarCitas(htmlATexto(m.html))
    }

    // ---------- deducciones ----------

    fun sinAcentos(t: String): String =
        Normalizer.normalize(t.lowercase(), Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "")

    private val REGLAS_TIPO = listOf(
        "averia" to listOf("averia", "no funciona", "no va", "fallo", "falla", "error", "roto", "incidencia", "salta sola"),
        "mantenimiento" to listOf("mantenimiento", "revision anual", "preventivo"),
        "presupuesto" to listOf("presupuesto", "oferta", "precio", "cotizacion"),
        "instalacion" to listOf("instalacion", "instalar", "montaje", "ampliacion", "nueva"),
        "revision" to listOf("revision", "revisar", "comprobar", "verificar")
    )

    private val REGLAS_SISTEMA = listOf(
        "cctv" to listOf("camara", "camaras", "cctv", "videovigilancia", "grabador", "dvr", "nvr"),
        "incendios" to listOf("incendio", "incendios", "detector de humo", "bie", "extintor", "pci"),
        "accesos" to listOf("control de acceso", "lector", "tarjeta", "torno", "huella"),
        "interfono" to listOf("portero", "videoportero", "interfono", "telefonillo"),
        "alarma" to listOf("alarma", "central", "sirena", "volumetrico", "cra", "intrusion")
    )

    private fun deducir(reglas: List<Pair<String, List<String>>>, texto: String, porDefecto: String): String {
        val t = sinAcentos(texto)
        return reglas.firstOrNull { (_, palabras) -> palabras.any { t.contains(it) } }?.first ?: porDefecto
    }

    fun deducirTipo(texto: String) = deducir(REGLAS_TIPO, texto, "averia")
    fun deducirSistema(texto: String) = deducir(REGLAS_SISTEMA, texto, "otros")

    fun deducirPrioridad(texto: String): String {
        val t = sinAcentos(texto)
        if (Regex("\\burgent|\\burgencia|inmediat|cuanto antes|sin alarma|sin cobertura").containsMatchIn(t)) return "urgente"
        if (Regex("\\bprioritario|importante|hoy mismo").containsMatchIn(t)) return "alta"
        return "normal"
    }

    /** Teléfonos españoles: 9 cifras que empiezan por 6, 7, 8 o 9. */
    fun buscarTelefono(texto: String): String {
        val m = Regex("(?:\\+34[\\s.-]?)?([6789]\\d{2})[\\s.-]?(\\d{2})[\\s.-]?(\\d{2})[\\s.-]?(\\d{2})").find(texto) ?: return ""
        return m.groupValues.drop(1).joinToString("")
    }

    fun nombreDe(m: MensajeCorreo): String {
        if (m.deNombre.isNotBlank()) return m.deNombre.trim()
        val arroba = m.de.indexOf('@')
        return if (arroba > 0) m.de.substring(0, arroba) else m.de.ifEmpty { "Remitente desconocido" }
    }

    fun diaDe(iso: String, hoy: LocalDate, zona: ZoneId): LocalDate =
        runCatching { Instant.parse(iso).atZone(zona).toLocalDate() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(zona).toLocalDate() }.getOrNull()
            ?: hoy

    /** Remitentes que no crean avisos: dirección exacta o «@dominio» entero. */
    fun ignorado(m: MensajeCorreo, lista: Collection<String>): Boolean {
        val de = m.de.lowercase().trim()
        if (de.isEmpty()) return false
        return lista.any { patron ->
            val p = patron.lowercase().trim()
            when {
                p.isEmpty() -> false
                !p.startsWith("@") -> de == p
                else -> de.length > p.length && de.endsWith(p)
            }
        }
    }

    // ---------- conversión ----------

    fun aAviso(m: MensajeCorreo, id: String, hoy: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Aviso {
        val cuerpo = cuerpoLegible(m)
        val conjunto = m.asunto + "\n" + cuerpo
        return Aviso(
            id = id,
            titulo = m.asunto.trim().ifEmpty { "(correo sin asunto)" },
            descripcion = cuerpo,
            tipo = deducirTipo(conjunto),
            sistema = deducirSistema(conjunto),
            prioridad = deducirPrioridad(conjunto),
            estado = Estado.PENDIENTE.id,
            fecha = diaDe(m.fecha, hoy, zona).toString(),
            hora = "",
            asignadoA = "",
            origen = "correo",
            correoId = m.clave,
            cliente = Cliente(nombre = nombreDe(m), direccion = "", telefono = buscarTelefono(cuerpo), contacto = m.de)
        )
    }
}
