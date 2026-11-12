package io.github.dadoga31.avisos

import io.github.dadoga31.avisos.correo.ConversorCorreo
import io.github.dadoga31.avisos.correo.MensajeCorreo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Los resultados esperados salen de pasar estos mismos correos por
 * assets/js/correo.js (la app web), con TZ=Europe/Madrid: la conversión
 * nativa tiene que dar el mismo aviso.
 */
class ConversorCorreoTest {
    private val hoy = LocalDate.of(2026, 11, 12)
    private val madrid = ZoneId.of("Europe/Madrid")

    private data class Esperado(
        val titulo: String, val tipo: String, val sistema: String, val prioridad: String, val fecha: String,
        val nombre: String, val telefono: String, val contacto: String, val descripcion: String
    )

    private val casos = listOf(
        MensajeCorreo(
            uid = "1", messageId = "<a1@empresa>", de = "pedro@naves.es", deNombre = "Pedro Gil",
            asunto = "URGENTE: alarma sin cobertura en nave 3", fecha = "2026-11-11T23:30:00Z",
            texto = "Hola,\r\nLa central no comunica con la CRA desde anoche.\nLlamad a Pedro al 600 11 22 33.\n\n" +
                "El lun, 9 nov 2026 a las 10:00, Soporte escribió:\n> texto viejo 699999999"
        ) to Esperado(
            "URGENTE: alarma sin cobertura en nave 3", "averia", "alarma", "urgente", "2026-11-12", "Pedro Gil",
            "600112233", "pedro@naves.es", "Hola,\nLa central no comunica con la CRA desde anoche.\nLlamad a Pedro al 600 11 22 33."
        ),
        MensajeCorreo(
            uid = "2", de = "admin@hotel.es", asunto = "Revisión cámaras", fecha = "2026-11-12T08:05:00+01:00",
            html = "<html><style>p{color:red}</style><p>Buenos d&iacute;as,</p><p>Las c&aacute;maras del parking no graban.<br>" +
                "Tel: +34 912 34 56 78</p><!-- pie --></html>"
        ) to Esperado(
            "Revisión cámaras", "revision", "cctv", "normal", "2026-11-12", "admin", "912345678", "admin@hotel.es",
            "Buenos días,\nLas cámaras del parking no graban.\nTel: +34 912 34 56 78"
        ),
        MensajeCorreo(
            uid = "3", messageId = "<c3@gmail>", de = "comunidad.olmo8@gmail.com", asunto = "Videoportero",
            texto = "Queremos un presupuesto para videoportero de 12 viviendas, importante."
        ) to Esperado(
            "Videoportero", "presupuesto", "interfono", "alta", "2026-11-12", "comunidad.olmo8", "",
            "comunidad.olmo8@gmail.com", "Queremos un presupuesto para videoportero de 12 viviendas, importante."
        ),
        MensajeCorreo(
            uid = "4", messageId = "<d4@x>", de = "juan@bar.es", deNombre = "Juan", fecha = "2026-11-10T10:00:00Z",
            texto = "Se ha roto la sirena\n\nDe: Juan\nEnviado: lunes\nAsunto: otra cosa"
        ) to Esperado(
            "(correo sin asunto)", "averia", "alarma", "normal", "2026-11-10", "Juan", "", "juan@bar.es", "Se ha roto la sirena"
        ),
        MensajeCorreo(
            uid = "5", messageId = "<e5@x>", de = "obra@constructora.es", deNombre = "Obra Norte",
            asunto = "Montaje control de acceso", fecha = "2026-11-12T12:00:00Z",
            texto = "Necesitamos el montaje de un control de acceso con huella en la entrada, &lt;cuanto antes&gt;."
        ) to Esperado(
            "Montaje control de acceso", "instalacion", "accesos", "urgente", "2026-11-12", "Obra Norte", "",
            "obra@constructora.es", "Necesitamos el montaje de un control de acceso con huella en la entrada, &lt;cuanto antes&gt;."
        )
    )

    @Test
    fun `da el mismo aviso que la app web`() {
        casos.forEach { (m, e) ->
            val a = ConversorCorreo.aAviso(m, "id", hoy, madrid)
            val obtenido = Esperado(
                a.titulo, a.tipo, a.sistema, a.prioridad, a.fecha, a.cliente.nombre, a.cliente.telefono,
                a.cliente.contacto, a.descripcion
            )
            assertEquals("correo ${m.uid}", e, obtenido)
            assertEquals("correo", a.origen)
            assertEquals("pendiente", a.estado)
            assertEquals("", a.asignadoA)
        }
    }

    @Test
    fun `la clave es el message-id y si no hay el uid`() {
        assertEquals("m:<a1@empresa>", casos[0].first.clave)
        assertEquals("u:2", casos[1].first.clave)
        assertEquals(listOf("m:<a1@empresa>", "u:1"), casos[0].first.claves)
    }

    @Test
    fun `remitentes ignorados por direccion exacta o por dominio`() {
        val lista = listOf("@boletines.es", "Spam@X.com")
        assertTrue(ConversorCorreo.ignorado(MensajeCorreo(de = "ofertas@boletines.es"), lista))
        assertTrue(ConversorCorreo.ignorado(MensajeCorreo(de = "spam@x.com"), lista))
        assertFalse(ConversorCorreo.ignorado(MensajeCorreo(de = "otro@x.com"), lista))
        assertFalse(ConversorCorreo.ignorado(MensajeCorreo(de = "boletines.es"), lista))
        assertFalse(ConversorCorreo.ignorado(MensajeCorreo(de = ""), lista))
    }

    @Test
    fun `html, entidades y citas`() {
        assertEquals("a & b € ñ 😀", ConversorCorreo.decodificar("a &amp; b &euro; &#241; &#x1F600;"))
        assertEquals("&raro;", ConversorCorreo.decodificar("&raro;"))
        assertEquals("· uno\n· dos", ConversorCorreo.htmlATexto("<ul><li>uno</li><li>dos</li></ul>"))
        assertEquals("Hola", ConversorCorreo.quitarCitas("Hola\n-----Mensaje original-----\nDe: x"))
        assertEquals(
            "Temperatura <5 grados",
            ConversorCorreo.cuerpoLegible(MensajeCorreo(texto = "Temperatura <5 grados", html = "<p>otra</p>"))
        )
    }
}
