package io.github.dadoga31.avisos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import io.github.dadoga31.avisos.datos.Adjunto
import io.github.dadoga31.avisos.datos.Repositorio
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Fotos de la ficha: se reducen a 1600 px por el lado largo y se enderezan
 * según el EXIF (si no, las de la cámara salen tumbadas), y se guardan como
 * JPEG con el aviso, en filesDir/adjuntos/<aviso>/.
 */
object Fotos {
    const val LADO_MAX = 1600
    private const val CALIDAD = 82

    /** Guarda la foto de [origen] con el aviso. Null si no se puede leer como imagen. */
    fun guardar(ctx: Context, avisoId: String, origen: Uri): Adjunto? {
        val cr = ctx.contentResolver
        return guardarDesde(ctx.filesDir, avisoId) { cr.openInputStream(origen) }
    }

    /** Lo mismo con cualquier fuente que se pueda abrir varias veces (para las pruebas). */
    fun guardarDesde(base: File, avisoId: String, abrir: () -> InputStream?): Adjunto? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        abrir()?.use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo(limites.outWidth, limites.outHeight, LADO_MAX) }
        var bmp = abrir()?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
        bmp = ajustar(bmp, giro(abrir), LADO_MAX)

        val dir = File(base, "adjuntos/$avisoId").apply { mkdirs() }
        val nombre = "foto-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".jpg"
        var destino = File(dir, nombre)
        var n = 2
        while (destino.exists()) destino = File(dir, nombre.removeSuffix(".jpg") + "-${n++}.jpg")
        destino.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
        return Adjunto(Repositorio.nuevoId("f"), destino.name, "image/jpeg", destino.length(), "adjuntos/$avisoId/${destino.name}")
    }

    /** Miniatura para la ficha (también endereza las que vienen del correo). */
    fun miniatura(f: File, lado: Int): Bitmap? {
        if (!f.exists()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, limites)
        if (limites.outWidth <= 0) return null
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply {
            inSampleSize = muestreo(limites.outWidth, limites.outHeight, lado)
        }) ?: return null
        return ajustar(bmp, giro { f.inputStream() }, lado)
    }

    /** La mayor potencia de dos que deja el lado largo por encima de [objetivo]. */
    fun muestreo(ancho: Int, alto: Int, objetivo: Int): Int {
        var m = 1
        while (maxOf(ancho, alto) / (m * 2) >= objetivo) m *= 2
        return m
    }

    private fun giro(abrir: () -> InputStream?): Int = runCatching {
        abrir()?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun ajustar(bmp: Bitmap, grados: Int, ladoMax: Int): Bitmap {
        val escala = minOf(1f, ladoMax.toFloat() / maxOf(bmp.width, bmp.height))
        if (escala >= 1f && grados == 0) return bmp
        val m = Matrix().apply {
            postScale(escala, escala)
            if (grados != 0) postRotate(grados.toFloat())
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
