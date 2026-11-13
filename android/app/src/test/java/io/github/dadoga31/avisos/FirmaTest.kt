package io.github.dadoga31.avisos

import io.github.dadoga31.avisos.correo.Firma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La firma pegada o abierta de Outlook. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class FirmaTest {
    /* Lo que deja Outlook en su carpeta de firmas, recortado. */
    private val outlook = """
        <html xmlns:o="urn:schemas-microsoft-com:office:office"><head><meta http-equiv=Content-Type content="text/html; charset=windows-1252">
        <style><!-- p.MsoNormal {margin:0cm;font-size:11.0pt;font-family:"Calibri",sans-serif;} --></style>
        <!--[if gte mso 9]><xml><o:shapedefaults v:ext="edit" spidmax="1026" /></xml><![endif]-->
        </head><body lang=ES>
        <p class=MsoNormal><b><span style='color:#FF4200'>Daniel García</span></b><o:p></o:p></p>
        <p class=MsoNormal>Técnico · Seguridad Norte<o:p></o:p></p>
        <p class=MsoNormal>Tel. 600&nbsp;11&nbsp;22&nbsp;33 · <a href="https://seguridadnorte.es">seguridadnorte.es</a></p>
        <p class=MsoNormal><img width=120 height=40 src="Firma_files/image001.png" alt="Logo"></p>
        <script>alert(1)</script><p onclick="robar()">Aviso legal</p>
        </body></html>
    """.trimIndent()

    @Test
    fun `del htm de Outlook se queda con lo que se ve`() {
        val f = Firma.desdeArchivo(outlook.toByteArray(Charsets.ISO_8859_1))
        assertTrue(f.startsWith("<style>"))
        assertTrue(f.contains("Daniel García"))
        assertTrue(f.contains("Técnico · Seguridad Norte"))
        assertFalse(f.contains("<script"))
        assertFalse(f.contains("onclick"))
        assertFalse(f.contains("o:p"))
        assertFalse(f.contains("[if gte mso"))
        assertFalse(f.contains("<body"))
    }

    @Test
    fun `del portapapeles toma solo el fragmento copiado`() {
        val pegado = "<html><body><!--StartFragment--><b>Dani</b><br>Seguridad Norte<!--EndFragment--></body></html>"
        assertEquals("<b>Dani</b><br>Seguridad Norte", Firma.limpiar(pegado))
    }

    @Test
    fun `las imagenes de Outlook que no se pueden traer se eligen a mano`() {
        val f = Firma.desdeArchivo(outlook.toByteArray(Charsets.ISO_8859_1))
        val imgs = Firma.imagenes(f)
        assertEquals(1, imgs.size)
        assertFalse(imgs[0].ok)
        val con = Firma.ponerImagen(f, 0, byteArrayOf(1, 2, 3), "image/png")
        assertTrue(Firma.imagenes(con).single().ok)
        assertTrue(con.contains("src=\"data:image/png;base64,AQID\""))
        assertTrue(con.contains("width=120"))
    }

    @Test
    fun `la firma en texto para los programas sin HTML`() {
        val f = Firma.desdeArchivo(outlook.toByteArray(Charsets.ISO_8859_1))
        val t = Firma.aTexto(f)
        assertTrue(t.startsWith("Daniel García\nTécnico · Seguridad Norte\nTel. 600 11 22 33 · seguridadnorte.es"))
        assertFalse(t.contains("MsoNormal"))
    }

    @Test
    fun `el texto suelto se convierte en una firma sencilla`() {
        assertEquals("<div>Dani &amp; Co<br>600 11 22 33</div>", Firma.deTexto("Dani & Co\n600 11 22 33"))
    }
}
