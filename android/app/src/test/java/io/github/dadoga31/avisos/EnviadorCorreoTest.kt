package io.github.dadoga31.avisos

import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.correo.AdjuntoEnvio
import io.github.dadoga31.avisos.correo.Cuenta
import io.github.dadoga31.avisos.correo.EnviadorCorreo
import io.github.dadoga31.avisos.correo.Firma
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Nota
import io.github.dadoga31.avisos.ui.Plantillas
import io.github.dadoga31.avisos.ui.envioDe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.Properties
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/** Lo que sale al responder y reenviar: cabeceras, cita, firma con sus imágenes y adjuntos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class EnviadorCorreoTest {
    private val hoy = LocalDate.of(2026, 11, 12)
    private val cuenta = Cuenta().apply {
        usuario = "avisos@seguridadnorte.es"; clave = "x"; servidor = "mail.seguridadnorte.es"; protocolo = "pop3"
        nombre = "Dani · Seguridad Norte"; smtpServidor = "mail.seguridadnorte.es"; smtpPuerto = 465
    }
    private val delCorreo = Aviso(
        id = "a1", ref = "AV-0008", titulo = "URGENTE: alarma sin cobertura", origen = "correo", correoId = "m:<9@naves.es>",
        fecha = "2026-11-12", hora = "11:30", creado = "2026-11-12T07:50:00Z",
        descripcion = "La central no comunica.\nLlamad a Pedro.",
        cliente = Cliente("Naves Logísticas Sur", "Pol. Ind. El Prado, nave 7", "600112233", "pedro@naves.es"),
        notas = listOf(Nota("n1", "2026-11-12T10:00:00Z", "Cambiado el módulo GPRS"))
    )

    /** Construye, escribe y vuelve a leer: así se mira lo que de verdad saldría. */
    private fun ida(e: io.github.dadoga31.avisos.correo.Envio): MimeMessage {
        val s = Session.getInstance(Properties())
        val m = EnviadorCorreo.construir(s, cuenta, e)
        val bytes = ByteArrayOutputStream().also { m.writeTo(it) }.toByteArray()
        return MimeMessage(s, ByteArrayInputStream(bytes))
    }

    private fun texto(p: Part): String = (p.content as String).replace("\r\n", "\n")

    private fun partes(p: Part): List<Part> =
        if (p.isMimeType("multipart/*")) (p.content as Multipart).let { mp -> (0 until mp.count).flatMap { partes(mp.getBodyPart(it)) } } else listOf(p)

    @Test
    fun `la respuesta va a quien lo mando, en la misma conversacion y con la cita`() {
        val e = envioDe(delCorreo, false, hoy, delCorreo.cliente.contacto, "", EnviadorCorreo.conPrefijo("Re:", delCorreo.titulo),
            "Pasamos a las 11:30.", null, emptyList(), copiaParaMi = true)
        val m = ida(e)
        assertEquals("Re: URGENTE: alarma sin cobertura", m.subject)
        assertEquals("<9@naves.es>", m.getHeader("In-Reply-To")[0])
        assertEquals("pedro@naves.es", (m.getRecipients(Message.RecipientType.TO)[0] as InternetAddress).address)
        assertEquals("Dani · Seguridad Norte", (m.from[0] as InternetAddress).personal)
        assertTrue(EnviadorCorreo.esPropio(m.messageID))
        val texto = partes(m).first { it.isMimeType("text/plain") }.let { texto(it) }
        assertTrue(texto.startsWith("Pasamos a las 11:30."))
        assertTrue(texto.contains("Naves Logísticas Sur <pedro@naves.es> escribió:"))
        assertTrue(texto.contains("> La central no comunica.\n> Llamad a Pedro."))
    }

    @Test
    fun `la copia para mi va oculta`() {
        val e = envioDe(delCorreo, false, hoy, "pedro@naves.es", "", "Re: x", "Hola", null, emptyList(), copiaParaMi = true)
        val m = EnviadorCorreo.construir(Session.getInstance(Properties()), cuenta, e)
        assertEquals("avisos@seguridadnorte.es", (m.getRecipients(Message.RecipientType.BCC)[0] as InternetAddress).address)
        /* Como lo escribe el envío por SMTP: sin la cabecera Bcc. */
        val escrito = ByteArrayOutputStream().also { m.writeTo(it, arrayOf("Bcc", "Content-Length")) }.toString()
        assertFalse("la copia oculta no sale en las cabeceras", escrito.contains("Bcc:"))
    }

    @Test
    fun `la firma va en html con su logo en linea y en texto`() {
        val firma = Firma.ponerImagen("<b>Dani</b><br>Seguridad Norte<br><img src=\"Firma_files/image001.png\">", 0, byteArrayOf(1, 2, 3), "image/png")
        val e = envioDe(delCorreo, false, hoy, "pedro@naves.es", "", "Re: x", "Hola", firma, emptyList(), false)
        val m = ida(e)
        val todas = partes(m)
        val html = todas.first { it.isMimeType("text/html") }.let { texto(it) }
        assertTrue(html.contains("<b>Dani</b>"))
        assertTrue(html.contains("src=\"cid:firma0@avisos\""))
        assertFalse(html.contains("data:image"))
        val logo = todas.first { it.isMimeType("image/png") } as javax.mail.internet.MimeBodyPart
        assertEquals("<firma0@avisos>", logo.contentID)
        assertEquals(Part.INLINE, logo.disposition)
        val texto = todas.first { it.isMimeType("text/plain") }.let { texto(it) }
        assertTrue(texto.contains("-- \nDani\nSeguridad Norte"))
    }

    @Test
    fun `el reenvio lleva el original y sus adjuntos`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pdf = File(ctx.cacheDir, "parte.pdf").apply { writeText("%PDF-1.4") }
        val e = envioDe(delCorreo, true, hoy, "laura@seguridadnorte.es", "", EnviadorCorreo.conPrefijo("RV:", delCorreo.titulo),
            "Te lo paso.", null, listOf(AdjuntoEnvio(pdf, "parte de incidencia.pdf", "application/pdf")), false)
        val m = ida(e)
        assertEquals("RV: URGENTE: alarma sin cobertura", m.subject)
        assertNull(m.getHeader("In-Reply-To"))
        val todas = partes(m)
        val texto = todas.first { it.isMimeType("text/plain") }.let { texto(it) }
        assertTrue(texto.contains("---------- Mensaje reenviado ----------\nDe: Naves Logísticas Sur <pedro@naves.es>"))
        assertTrue(texto.contains("\nLa central no comunica.\nLlamad a Pedro."))
        val adjunto = todas.first { it.isMimeType("application/pdf") }
        assertEquals("parte de incidencia.pdf", adjunto.fileName)
        assertEquals(Part.ATTACHMENT, adjunto.disposition)
    }

    @Test
    fun `direcciones, prefijos y respuestas rapidas`() {
        assertEquals(listOf("a@b.es", "c@d.es"), EnviadorCorreo.direcciones("a@b.es; c@d.es, nada"))
        assertEquals("Re: Hola", EnviadorCorreo.conPrefijo("Re:", "Hola"))
        assertEquals("RE: Hola", EnviadorCorreo.conPrefijo("Re:", "RE: Hola"))
        assertEquals("RV: Re: Hola", EnviadorCorreo.conPrefijo("RV:", "Re: Hola"))
        val r = Plantillas.respuestas(delCorreo, hoy).toMap()
        assertTrue(r.getValue("Recibido").contains("Le confirmamos la visita hoy a las 11:30."))
        assertTrue(r.getValue("Visita").contains("pasaremos hoy a las 11:30 por Pol. Ind. El Prado, nave 7."))
        assertTrue(r.getValue("Terminado").contains("· Cambiado el módulo GPRS"))
    }
}
