package io.github.dadoga31.avisos

import io.github.dadoga31.avisos.correo.Hilos
import io.github.dadoga31.avisos.correo.MensajeCorreo
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Qué correo es respuesta de qué aviso (3.8.0). */
class HilosTest {
    private val ahora = Instant.parse("2026-11-13T10:00:00Z")
    private val original = Aviso(
        id = "a1", titulo = "Central sin comunicación", origen = "correo", correoId = "m:<1@naves.es>",
        cliente = Cliente("Naves Sur", contacto = "pedro@naves.es"), creado = "2026-11-12T08:00:00Z",
        hilo = listOf("m:<1@naves.es>", "t:AQHZtOmBzK1H8bA7EkW8u6hKqZ5GXg==")
    )
    private val otro = Aviso(id = "a2", titulo = "Cámara sin imagen", origen = "correo", correoId = "m:<2@hotel.es>",
        cliente = Cliente(contacto = "ana@hotel.es"), creado = "2026-11-12T09:00:00Z")
    private val avisos = listOf(original, otro)

    private fun m(asunto: String, de: String = "laura@instaladora.es", inReplyTo: String = "", refs: List<String> = emptyList(), ti: String = "", id: String = "<9@x>") =
        MensajeCorreo(uid = "u", messageId = id, de = de, asunto = asunto, enRespuestaA = inReplyTo, referencias = refs, hiloOutlook = ti)

    @Test
    fun `la respuesta de otro, aunque yo vaya en copia, cae en su aviso`() {
        assertEquals("a1", Hilos.buscar(avisos, m("RE: Central sin comunicación", inReplyTo = "<1@naves.es>"), ahora)?.id)
    }

    @Test
    fun `una respuesta a una respuesta se reconoce por References`() {
        val r = m("RE: RE: Central", inReplyTo = "<5@otro.es>", refs = listOf("<1@naves.es>", "<5@otro.es>"))
        assertEquals("a1", Hilos.buscar(avisos, r, ahora)?.id)
    }

    @Test
    fun `y por lo que ya se anadio al hilo`() {
        val conRespuesta = original.copy(hilo = original.hilo + "m:<5@otro.es>")
        assertEquals("a1", Hilos.buscar(listOf(conRespuesta, otro), m("RE: algo", inReplyTo = "<5@otro.es>"), ahora)?.id)
    }

    @Test
    fun `Outlook sin cabeceras de respuesta se reconoce por su conversacion`() {
        /* Mismos 22 primeros bytes (la conversación) y 5 bytes más de esta respuesta. */
        val respuesta = java.util.Base64.getEncoder().encodeToString(
            java.util.Base64.getDecoder().decode("AQHZtOmBzK1H8bA7EkW8u6hKqZ5GXg==").copyOf(22) + byteArrayOf(1, 2, 3, 4, 5)
        )
        assertEquals(Hilos.conversacion("AQHZtOmBzK1H8bA7EkW8u6hKqZ5GXg=="), Hilos.conversacion(respuesta))
        val original22 = original.copy(hilo = listOf("t:" + Hilos.conversacion("AQHZtOmBzK1H8bA7EkW8u6hKqZ5GXg==")))
        assertEquals("a1", Hilos.buscar(listOf(original22, otro), m("RE: Central", ti = respuesta), ahora)?.id)
    }

    @Test
    fun `sin cabeceras, el asunto solo junta si es Re y del mismo dominio`() {
        assertEquals("a1", Hilos.buscar(avisos, m("Re: Central sin comunicacion", de = "jefe@naves.es"), ahora)?.id)
        assertNull("otro dominio", Hilos.buscar(avisos, m("Re: Central sin comunicación", de = "x@otra.es"), ahora))
        assertNull("sin Re: es un aviso nuevo", Hilos.buscar(avisos, m("Central sin comunicación", de = "jefe@naves.es"), ahora))
        assertNull("pasado un mes, no", Hilos.buscar(avisos, m("Re: Central sin comunicación", de = "jefe@naves.es"), Instant.parse("2027-01-20T10:00:00Z")))
    }

    @Test
    fun `un correo nuevo no se junta con nada`() {
        assertNull(Hilos.buscar(avisos, m("Presupuesto videoportero", inReplyTo = "<desconocido@x>"), ahora))
    }

    @Test
    fun `el mismo correo no se anade dos veces`() {
        val conRespuesta = original.copy(hilo = original.hilo + "m:<9@x>")
        assertTrue(Hilos.yaVisto(listOf(conRespuesta), m("RE: x")))
        assertFalse(Hilos.yaVisto(avisos, m("RE: x")))
    }

    @Test
    fun `de la respuesta solo se queda lo nuevo`() {
        val r = MensajeCorreo(
            de = "laura@instaladora.es", deNombre = "Laura", asunto = "RE: x",
            texto = "Ya está cambiado el módulo.\n\nDe: Pedro <pedro@naves.es>\nEnviado: jueves\nAsunto: Central\n\nLa central no comunica."
        )
        assertEquals("Ya está cambiado el módulo.", Hilos.textoRespuesta(r))
        assertEquals("✉ Respuesta de Laura (laura@instaladora.es):\nYa está cambiado el módulo.", Hilos.nota(r))
        val conMayor = r.copy(texto = "Perfecto, gracias.\n> La central no comunica.\n> Llamad a Pedro.")
        assertEquals("Perfecto, gracias.", Hilos.textoRespuesta(conMayor))
    }
}
