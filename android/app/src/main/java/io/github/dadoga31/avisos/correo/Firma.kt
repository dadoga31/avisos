package io.github.dadoga31.avisos.correo

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

/**
 * La firma de los correos, en HTML, tal como la tiene el usuario en Outlook.
 * Se trae pegándola (si se copia de un correo, el portapapeles lleva el HTML)
 * o abriendo el .htm de la carpeta de firmas de Outlook. Las imágenes que no
 * se pueden traer (las de Outlook van como ficheros sueltos o «cid:») se
 * eligen a mano y quedan dentro de la firma; al enviar van como imágenes en
 * línea, que es como se ven en Outlook y en Gmail.
 */
object Firma {
    private val TODO = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

    private fun fichero(ctx: Context) = File(ctx.filesDir, "firma/firma.html")

    fun leer(ctx: Context): String? = fichero(ctx).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }

    fun guardar(ctx: Context, html: String) {
        val f = fichero(ctx)
        f.parentFile?.mkdirs()
        f.writeText(html)
    }

    fun borrar(ctx: Context) { fichero(ctx).delete() }

    /** Del portapapeles: el HTML si lo trae (copiado de un correo); si no, el texto. */
    fun desdePortapapeles(ctx: Context): String? {
        val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        item.htmlText?.takeIf { it.isNotBlank() }?.let { return limpiar(it) }
        val t = item.coerceToText(ctx)?.toString()?.trim().orEmpty()
        return if (t.isEmpty()) null else deTexto(t)
    }

    /** Un .htm de Outlook: respeta su juego de caracteres (suele ser windows-1252). */
    fun desdeArchivo(bytes: ByteArray): String {
        val texto = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> String(bytes, Charsets.UTF_16LE)
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> String(bytes, Charsets.UTF_16BE)
            else -> {
                val cabeza = String(bytes, 0, minOf(bytes.size, 4096), Charsets.ISO_8859_1)
                val juego = Regex("charset\\s*=\\s*[\"']?([\\w-]+)", RegexOption.IGNORE_CASE).find(cabeza)?.groupValues?.get(1)
                String(bytes, runCatching { Charset.forName(juego ?: "UTF-8") }.getOrDefault(Charsets.UTF_8))
            }
        }
        return limpiar(texto.removePrefix("\uFEFF"))
    }

    fun deTexto(t: String) = "<div>" + escapar(t.trim()).replace("\n", "<br>") + "</div>"

    fun escapar(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /**
     * Se queda con lo que se ve: el fragmento copiado o el cuerpo del .htm, y
     * los estilos delante. Fuera scripts, eventos, comentarios condicionales
     * de Outlook y etiquetas de Office que ningún otro programa entiende.
     */
    fun limpiar(html: String): String {
        val estilos = Regex("<style[^>]*>.*?</style>", TODO).findAll(html).joinToString("") { it.value }
        var h = html
        Regex("<!--\\s*StartFragment\\s*-->(.*)<!--\\s*EndFragment\\s*-->", TODO).find(h)?.let { h = it.groupValues[1] }
        Regex("<body[^>]*>(.*)</body>", TODO).find(h)?.let { h = it.groupValues[1] }
        h = h.replace(Regex("<script[^>]*>.*?</script>", TODO), "")
            .replace(Regex("<style[^>]*>.*?</style>", TODO), "")
            .replace(Regex("<!--.*?-->", TODO), "")
            .replace(Regex("<!\\[if[^\\]]*\\]>|<!\\[endif\\]>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?(meta|link|base|title|head|html|body)\\b[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</?o:p>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\son[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(href|src)\\s*=\\s*([\"'])\\s*javascript:[^\"']*\\2", RegexOption.IGNORE_CASE), "$1=\"#\"")
            .trim()
        return (estilos + h).trim()
    }

    // ---------------------------------------------------------------- imágenes

    data class Imagen(val indice: Int, val src: String, val ok: Boolean)

    private val IMG = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val SRC = Regex("\\ssrc\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE)

    private fun srcDe(etiqueta: String): String =
        SRC.find(etiqueta)?.let { m -> m.groupValues[2].ifEmpty { m.groupValues[3] }.ifEmpty { m.groupValues[4] } } ?: ""

    /** Las imágenes de la firma; [Imagen.ok] = se verán (web o metidas en la firma). */
    fun imagenes(html: String): List<Imagen> = IMG.findAll(html).mapIndexed { i, m ->
        val src = srcDe(m.value)
        Imagen(i, src, src.startsWith("https://", true) || src.startsWith("http://", true) || src.startsWith("data:image/", true))
    }.toList()

    /** Mete la imagen número [indice] dentro de la firma (data: URI). */
    fun ponerImagen(html: String, indice: Int, bytes: ByteArray, mime: String): String {
        val dato = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        var n = -1
        return IMG.replace(html) { m ->
            n++
            if (n != indice) return@replace m.value
            if (SRC.containsMatchIn(m.value)) SRC.replace(m.value, " src=\"$dato\"")
            else m.value.replaceFirst(Regex("<img", RegexOption.IGNORE_CASE), "<img src=\"$dato\"")
        }
    }

    /** Un logo elegido de la galería, reducido para que el correo no pese. */
    fun logoDe(ctx: Context, uri: Uri, ancho: Int = 400): Pair<ByteArray, String>? = runCatching {
        val bmp = ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) } ?: return null
        val escala = if (bmp.width > ancho) ancho.toFloat() / bmp.width else 1f
        val final = if (escala < 1f) Bitmap.createScaledBitmap(bmp, ancho, (bmp.height * escala).toInt().coerceAtLeast(1), true) else bmp
        val salida = ByteArrayOutputStream()
        final.compress(Bitmap.CompressFormat.PNG, 100, salida)
        salida.toByteArray() to "image/png"
    }.getOrNull()

    // ---------------------------------------------------------------- texto

    /** La firma en texto, para los programas que no enseñan HTML. */
    fun aTexto(html: String): String {
        /* En HTML los saltos de línea del código no cuentan: los ponen <br> y </p>. */
        val sin = html.replace(Regex("<style[^>]*>.*?</style>", TODO), "")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|div|tr|li|h[1-6]|table)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</t[dh]>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), "")
        return entidades(sin).lines().map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
            .joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun entidades(t: String): String = t
        .replace(Regex("&#(\\d+);")) { runCatching { String(Character.toChars(it.groupValues[1].toInt())) }.getOrDefault("") }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { runCatching { String(Character.toChars(it.groupValues[1].toInt(16))) }.getOrDefault("") }
        .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&aacute;", "á").replace("&eacute;", "é").replace("&iacute;", "í").replace("&oacute;", "ó").replace("&uacute;", "ú")
        .replace("&ntilde;", "ñ").replace("&Ntilde;", "Ñ").replace("&amp;", "&")
}
