package io.github.dadoga31.avisos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.BaseDatos
import io.github.dadoga31.avisos.datos.Repositorio
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = android.app.Application::class)
class FotosTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    /** Una «foto» apaisada de la cámara con el EXIF diciendo que hay que girarla 90°. */
    private fun fotoDeCamara(ancho: Int, alto: Int, giro: Int): File {
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(bmp).apply {
            drawColor(Color.rgb(40, 90, 160))
            drawRect(0f, 0f, ancho / 3f, alto.toFloat(), Paint().apply { color = Color.rgb(242, 116, 39) })
        }
        val f = File(ctx.cacheDir, "camara-prueba.jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(f.path).apply {
            setAttribute(
                ExifInterface.TAG_ORIENTATION,
                (if (giro == 90) ExifInterface.ORIENTATION_ROTATE_90 else ExifInterface.ORIENTATION_NORMAL).toString()
            )
            saveAttributes()
        }
        return f
    }

    @Test
    fun `reduce a 1600 por el lado largo y endereza segun el EXIF`() {
        val f = fotoDeCamara(3000, 2000, 90)
        val base = File(ctx.filesDir, "fotos1").apply { deleteRecursively() }
        val adj = Fotos.guardarDesde(base, "av1") { f.inputStream() }!!
        val guardada = File(base, adj.ruta)
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(guardada.path, o)
        assertEquals(1600, maxOf(o.outWidth, o.outHeight))
        assertTrue("tiene que quedar de pie: ${o.outWidth}x${o.outHeight}", o.outHeight > o.outWidth)
        assertEquals("image/jpeg", adj.mime)
        assertTrue(adj.ruta.startsWith("adjuntos/av1/foto-"))
        assertTrue(adj.tam < f.length())
    }

    @Test
    fun `una foto pequena no se agranda y un archivo que no es imagen no se guarda`() {
        val f = fotoDeCamara(800, 600, 0)
        val base = File(ctx.filesDir, "fotos2").apply { deleteRecursively() }
        val adj = Fotos.guardarDesde(base, "av1") { f.inputStream() }!!
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(File(base, adj.ruta).path, o)
        assertEquals(800 to 600, o.outWidth to o.outHeight)
        assertNull(Fotos.guardarDesde(base, "av1") { "esto no es una foto".byteInputStream() })
        assertEquals(1, Fotos.muestreo(1600, 1200, 1600))
        assertEquals(2, Fotos.muestreo(4000, 3000, 1600))
    }

    @Test
    fun `anadir y borrar una foto del aviso borra tambien el fichero`() = runBlocking {
        val base = File(ctx.filesDir, "fotos3").apply { deleteRecursively() }
        val db = BaseDatos.enMemoria(ctx)
        val repo = Repositorio(db.dao(), AlmacenAjustes(ctx), carpeta = base)
        val a = repo.guardar(repo.nuevo().copy(titulo = "Con foto"))
        val adj = Fotos.guardarDesde(base, a.id) { fotoDeCamara(1200, 900, 0).inputStream() }!!
        repo.anadirAdjunto(a.id, adj)
        assertEquals(listOf(adj), repo.porId(a.id)!!.adjuntos)
        repo.borrarAdjunto(a.id, adj.id)
        assertTrue(repo.porId(a.id)!!.adjuntos.isEmpty())
        assertFalse(File(base, adj.ruta).exists())
        db.close()
    }
}
