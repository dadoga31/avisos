package io.github.dadoga31.avisos.correo

import android.util.Base64
import java.io.File
import java.util.Date
import java.util.Properties
import java.util.UUID
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Part
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

/** Un fichero del aviso que va adjunto. */
data class AdjuntoEnvio(val fichero: File, val nombre: String, val mime: String)

/**
 * Lo que se va a mandar. [cita] es el texto del correo original: en una
 * respuesta va citado debajo («> …»); en un reenvío, tal cual tras
 * [cabeceraCita]. [enRespuestaA] es el Message-ID original, para que la
 * respuesta caiga en la misma conversación.
 */
data class Envio(
    val para: List<String>,
    val cc: List<String> = emptyList(),
    val asunto: String,
    val texto: String,
    val firma: String? = null,
    val cabeceraCita: String = "",
    val cita: String = "",
    val reenvio: Boolean = false,
    val enRespuestaA: String = "",
    val adjuntos: List<AdjuntoEnvio> = emptyList(),
    val copiaParaMi: Boolean = false
)

/**
 * Envía correo por el SMTP de la empresa con el usuario y la contraseña que
 * la app ya guarda cifrados. Texto y HTML (con la firma y sus imágenes en
 * línea), adjuntos, respuesta en la misma conversación y copia: con IMAP se
 * guarda en «Enviados»; con POP3 no hay «Enviados» en el servidor y se manda
 * copia oculta a la propia cuenta. Lo enviado lleva un Message-ID propio
 * para que el buzón no lo convierta luego en un aviso.
 */
object EnviadorCorreo {
    const val PREFIJO_ID = "<avisos-"

    /** ¿Lo mandó esta app? (la copia que vuelve por POP3 no es un aviso). */
    fun esPropio(messageId: String) = messageId.trim().startsWith(PREFIJO_ID)

    fun direcciones(texto: String): List<String> =
        texto.split(',', ';', ' ', '\n').map { it.trim().trim('<', '>') }.filter { it.contains('@') && it.indexOf('@') > 0 }.distinct()

    /** «Re: …» o «RV: …» sin repetir el prefijo si ya lo lleva. */
    fun conPrefijo(prefijo: String, asunto: String): String {
        val limpio = asunto.trim()
        return if (limpio.startsWith(prefijo, ignoreCase = true)) limpio else "$prefijo $limpio"
    }

    fun propiedades(c: Cuenta): Properties = Properties().apply {
        setProperty("mail.transport.protocol", "smtp")
        setProperty("mail.smtp.host", c.smtpServidor)
        setProperty("mail.smtp.port", c.smtpPuerto.toString())
        setProperty("mail.smtp.auth", "true")
        setProperty("mail.smtp.connectiontimeout", "20000")
        setProperty("mail.smtp.timeout", "40000")
        setProperty("mail.smtp.writetimeout", "40000")
        setProperty("mail.smtp.from", c.usuario)
        setProperty("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
        if (c.smtpSSL()) setProperty("mail.smtp.ssl.enable", "true")
        else {
            setProperty("mail.smtp.starttls.enable", "true")
            setProperty("mail.smtp.starttls.required", "true")
        }
        setProperty("mail.mime.encodefilename", "true")
        setProperty("mail.mime.charset", "UTF-8")
    }

    private fun html(t: String) = Firma.escapar(t).replace("\r\n", "\n").replace("\n", "<br>")

    /** Las imágenes metidas en la firma (data:) pasan a ser partes en línea (cid:). */
    private fun separarImagenes(firma: String): Pair<String, List<Triple<String, ByteArray, String>>> {
        val partes = mutableListOf<Triple<String, ByteArray, String>>()
        val dato = Regex("data:(image/[a-zA-Z0-9.+-]+);base64,([A-Za-z0-9+/=\\s]+)")
        val html = dato.replace(firma) { m ->
            val id = "firma${partes.size}@avisos"
            val bytes = runCatching { Base64.decode(m.groupValues[2].replace(Regex("\\s"), ""), Base64.DEFAULT) }.getOrNull()
                ?: return@replace m.value
            partes += Triple(id, bytes, m.groupValues[1])
            "cid:$id"
        }
        return html to partes
    }

    fun construir(sesion: Session, c: Cuenta, e: Envio): MimeMessage {
        val dominio = c.usuario.substringAfter('@', "").ifBlank { "avisos.local" }
        val m = object : MimeMessage(sesion) {
            override fun updateMessageID() { setHeader("Message-ID", "$PREFIJO_ID${UUID.randomUUID()}@$dominio>") }
        }
        m.setFrom(InternetAddress(c.usuario, c.nombre.ifBlank { null }, "UTF-8"))
        m.setRecipients(Message.RecipientType.TO, e.para.map { InternetAddress(it, true) }.toTypedArray())
        if (e.cc.isNotEmpty()) m.setRecipients(Message.RecipientType.CC, e.cc.map { InternetAddress(it, true) }.toTypedArray())
        if (e.copiaParaMi) m.setRecipients(Message.RecipientType.BCC, arrayOf(InternetAddress(c.usuario)))
        m.setSubject(e.asunto, "UTF-8")
        m.sentDate = Date()
        if (e.enRespuestaA.isNotBlank()) {
            m.setHeader("In-Reply-To", e.enRespuestaA)
            m.setHeader("References", e.enRespuestaA)
        }
        m.setHeader("X-Mailer", "Avisos para Android")

        /* Texto plano. */
        val plano = buildString {
            append(e.texto.trim())
            e.firma?.let { append("\n\n-- \n").append(Firma.aTexto(it)) }
            if (e.cita.isNotBlank() || e.cabeceraCita.isNotBlank()) {
                append("\n\n").append(e.cabeceraCita.trim()).append("\n")
                if (e.reenvio) append("\n").append(e.cita.trim())
                else append(e.cita.trim().lines().joinToString("\n") { "> $it" })
            }
        }

        /* HTML: el mensaje, la firma tal cual y el original. */
        val (firmaHtml, imagenes) = e.firma?.let { separarImagenes(it) } ?: ("" to emptyList())
        val estilos = Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val cabeza = estilos.findAll(firmaHtml).joinToString("") { it.value }
        val cita = when {
            e.cita.isBlank() && e.cabeceraCita.isBlank() -> ""
            e.reenvio -> "<br><div style=\"color:#555\">${html(e.cabeceraCita.trim())}<br><br>${html(e.cita.trim())}</div>"
            else -> "<br><div style=\"color:#555\">${html(e.cabeceraCita.trim())}</div>" +
                "<blockquote style=\"margin:0 0 0 .8ex;border-left:2px solid #ccc;padding-left:1ex;color:#555\">${html(e.cita.trim())}</blockquote>"
        }
        val cuerpo = "<!DOCTYPE html><html><head><meta charset=\"utf-8\">$cabeza</head><body>" +
            "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#222\">${html(e.texto.trim())}</div>" +
            (if (firmaHtml.isNotBlank()) "<br><div>${estilos.replace(firmaHtml, "")}</div>" else "") +
            cita + "</body></html>"

        val textoParte = MimeBodyPart().apply { setText(plano, "UTF-8", "plain") }
        val htmlParte = MimeBodyPart().apply { setText(cuerpo, "UTF-8", "html") }
        val alternativa = MimeMultipart("alternative").apply {
            addBodyPart(textoParte)
            if (imagenes.isEmpty()) addBodyPart(htmlParte)
            else addBodyPart(MimeBodyPart().apply {
                setContent(MimeMultipart("related").apply {
                    addBodyPart(htmlParte)
                    imagenes.forEachIndexed { n, (id, bytes, mime) ->
                        addBodyPart(MimeBodyPart().apply {
                            dataHandler = DataHandler(ByteArrayDataSource(bytes, mime))
                            setContentID("<$id>")
                            disposition = Part.INLINE
                            fileName = "firma$n." + mime.substringAfter('/').substringBefore('+')
                        })
                    }
                })
            })
        }
        if (e.adjuntos.isEmpty()) m.setContent(alternativa)
        else m.setContent(MimeMultipart("mixed").apply {
            addBodyPart(MimeBodyPart().apply { setContent(alternativa) })
            e.adjuntos.filter { it.fichero.exists() }.forEach { a ->
                addBodyPart(MimeBodyPart().apply {
                    dataHandler = DataHandler(object : FileDataSource(a.fichero) {
                        override fun getContentType() = a.mime.ifBlank { "application/octet-stream" }
                    })
                    fileName = a.nombre
                    disposition = Part.ATTACHMENT
                })
            }
        })
        m.saveChanges()
        return m
    }

    /** Conecta y se identifica sin mandar nada. null si va bien; si no, el motivo. */
    fun probar(c: Cuenta): String? = try {
        val t = Session.getInstance(propiedades(c)).getTransport("smtp")
        t.connect(c.smtpServidor, c.smtpPuerto, c.usuarioSmtp(), c.claveSmtp())
        t.close()
        null
    } catch (e: Exception) {
        explicar(e)
    }

    /**
     * Lo manda. Devuelve un aviso si se envió pero no se pudo guardar copia
     * en «Enviados» (o null). Si no se puede enviar, lanza la excepción.
     */
    fun enviar(c: Cuenta, e: Envio): String? {
        val sesion = Session.getInstance(propiedades(c))
        val m = construir(sesion, c, e)
        val t = sesion.getTransport("smtp")
        try {
            t.connect(c.smtpServidor, c.smtpPuerto, c.usuarioSmtp(), c.claveSmtp())
            t.sendMessage(m, m.allRecipients)
        } finally {
            runCatching { t.close() }
        }
        return if (c.esImap()) guardarEnEnviados(c, m) else null
    }

    /** Con IMAP: copia en la carpeta de enviados (la marcada \Sent o la que se llame así). */
    private fun guardarEnEnviados(c: Cuenta, m: MimeMessage): String? {
        var store: Store? = null
        return try {
            store = Session.getInstance(ClienteCorreo.propiedades(c)).store
            store.connect(c.servidor, c.puerto, c.usuario, c.clave)
            val todas = store.defaultFolder.list("*")
            val enviados = todas.firstOrNull { f -> (f as? com.sun.mail.imap.IMAPFolder)?.attributes?.any { it.equals("\\Sent", true) } == true }
                ?: listOf("Sent", "Enviados", "INBOX.Sent", "INBOX.Enviados", "Sent Items", "Elementos enviados", "Sent Messages")
                    .firstNotNullOfOrNull { n -> todas.firstOrNull { it.fullName.equals(n, true) } }
                ?: return "Enviado. No se encontró la carpeta «Enviados» para guardar la copia."
            if (!enviados.isOpen) enviados.open(Folder.READ_WRITE)
            m.setFlag(Flags.Flag.SEEN, true)
            enviados.appendMessages(arrayOf(m))
            enviados.close(false)
            null
        } catch (e: Exception) {
            "Enviado, pero no se pudo guardar copia en «Enviados»"
        } finally {
            runCatching { store?.close() }
        }
    }

    fun explicar(e: Exception): String = when (e) {
        is javax.mail.AuthenticationFailedException -> "Usuario o contraseña del envío incorrectos"
        is javax.mail.SendFailedException ->
            "El servidor no acepta " + (e.invalidAddresses?.joinToString { it.toString() }?.ifBlank { null } ?: "el envío") +
                (e.message?.let { ": $it" } ?: "")
        is javax.mail.internet.AddressException -> "Dirección no válida: ${e.ref ?: ""}"
        else -> ClienteCorreo.explicar(e)
    }
}
