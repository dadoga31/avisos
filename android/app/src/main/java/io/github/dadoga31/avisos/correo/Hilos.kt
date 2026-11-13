package io.github.dadoga31.avisos.correo

import io.github.dadoga31.avisos.datos.Aviso
import java.time.Duration
import java.time.Instant

/**
 * Hilos de correo (3.8.0). Una respuesta a un correo que ya es aviso (aunque
 * la mande otro y uno solo vaya en copia) no crea un aviso nuevo: se añade
 * al que ya hay. Se reconoce por las cabeceras que ponen todos los programas
 * de correo al responder (In-Reply-To y References, con el Message-ID de los
 * anteriores) y por la conversación de Outlook (Thread-Index). Solo si un
 * programa no pone nada de eso, se mira el asunto, con cuidado de no juntar
 * dos avisos distintos.
 */
object Hilos {
    private val PREFIJO = Regex("^\\s*((re|rv|fw|fwd|reenv|r)\\s*(\\[\\d+])?\\s*:\\s*)+", RegexOption.IGNORE_CASE)

    /** Los Message-ID a los que contesta este correo. */
    fun anteriores(m: MensajeCorreo): List<String> =
        (listOf(m.enRespuestaA) + m.referencias).map { it.trim() }.filter { it.isNotBlank() }.distinct()

    /**
     * La conversación de Outlook: los primeros 22 bytes del Thread-Index son
     * los mismos en todo el hilo; el resto crece con cada respuesta.
     */
    fun conversacion(threadIndex: String): String {
        val t = threadIndex.trim()
        if (t.isEmpty()) return ""
        val bytes = runCatching { java.util.Base64.getMimeDecoder().decode(t) }.getOrNull()
        return if (bytes != null && bytes.size >= 22) java.util.Base64.getEncoder().encodeToString(bytes.copyOf(22)) else t.take(30)
    }

    /** Lo que se guarda en el aviso para reconocer las respuestas que vengan. */
    fun claves(m: MensajeCorreo): List<String> =
        (m.claves + conversacion(m.hiloOutlook).takeIf { it.isNotEmpty() }?.let { "t:$it" }).filterNotNull().distinct()

    /** ¿Este mismo correo ya está en algún aviso (como origen o como respuesta)? */
    fun yaVisto(avisos: List<Aviso>, m: MensajeCorreo): Boolean {
        val propias = m.claves.toSet()
        if (propias.isEmpty()) return false
        return avisos.any { it.correoId in propias || it.hilo.any { h -> h in propias } }
    }

    fun esRespuesta(asunto: String) = PREFIJO.containsMatchIn(asunto)

    /** El asunto sin «Re:», «RV:», «FW:»… ni mayúsculas ni tildes. */
    fun asuntoLimpio(asunto: String) = ConversorCorreo.sinAcentos(asunto.replace(PREFIJO, "")).replace(Regex("\\s+"), " ").trim()

    private fun dominio(correo: String) = correo.substringAfter('@', "").lowercase().trim()

    /**
     * El aviso al que pertenece este correo, o null si es un correo nuevo.
     * Por orden: las cabeceras de respuesta, la conversación de Outlook y,
     * como último recurso, el mismo asunto con «Re:» de alguien del mismo
     * dominio que el remitente original, en los 30 días siguientes.
     */
    fun buscar(avisos: List<Aviso>, m: MensajeCorreo, ahora: Instant = Instant.now()): Aviso? {
        val previos = anteriores(m).map { "m:$it" }.toSet()
        if (previos.isNotEmpty()) {
            avisos.firstOrNull { it.correoId in previos || it.hilo.any { h -> h in previos } }?.let { return it }
        }
        val conv = conversacion(m.hiloOutlook)
        if (conv.isNotEmpty()) avisos.firstOrNull { "t:$conv" in it.hilo }?.let { return it }

        if (!esRespuesta(m.asunto)) return null
        val asunto = asuntoLimpio(m.asunto)
        if (asunto.length < 6) return null
        val dom = dominio(m.de)
        return avisos.filter { a ->
            a.origen == "correo" && asuntoLimpio(a.titulo) == asunto &&
                dom.isNotEmpty() && dominio(a.cliente.contacto) == dom &&
                runCatching { Duration.between(Instant.parse(a.creado), ahora).toDays() <= 30 }.getOrDefault(false)
        }.maxByOrNull { it.creado }
    }

    /** Lo nuevo de la respuesta, sin el correo citado debajo. */
    fun textoRespuesta(m: MensajeCorreo): String {
        val t = ConversorCorreo.cuerpoLegible(m)
        /* Lo que algunos programas citan con «>» al final. */
        val lineas = t.lines()
        val corte = lineas.indexOfFirst { it.trimStart().startsWith(">") }
        val propio = (if (corte > 0) lineas.take(corte) else lineas).joinToString("\n").trim()
        return propio.ifEmpty { t }.take(4000)
    }

    /** «Pedro Ruiz (pedro@naves.es)» o la dirección sola. */
    fun quien(m: MensajeCorreo) = if (m.deNombre.isNotBlank() && m.deNombre != m.de) "${m.deNombre} (${m.de})" else m.de.ifBlank { "Alguien" }

    fun nota(m: MensajeCorreo) = "✉ Respuesta de ${quien(m)}:\n${textoRespuesta(m)}"
}
