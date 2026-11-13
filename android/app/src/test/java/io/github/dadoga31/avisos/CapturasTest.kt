package io.github.dadoga31.avisos

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.BaseDatos
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Repositorio
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tema
import io.github.dadoga31.avisos.correo.ConversorCorreo
import io.github.dadoga31.avisos.correo.MensajeCorreo
import io.github.dadoga31.avisos.datos.Adjunto
import io.github.dadoga31.avisos.ui.AccionesAgenda
import io.github.dadoga31.avisos.ui.AccionesCorreo
import io.github.dadoga31.avisos.ui.CorreoPantalla
import io.github.dadoga31.avisos.ui.RedactarPantalla
import io.github.dadoga31.avisos.ui.EstadoBuzon
import io.github.dadoga31.avisos.ui.FormCuenta
import io.github.dadoga31.avisos.ui.AccionesAjustes
import io.github.dadoga31.avisos.ui.AccionesFicha
import io.github.dadoga31.avisos.ui.AgendaPantalla
import io.github.dadoga31.avisos.ui.AjustesPantalla
import io.github.dadoga31.avisos.ui.AvisosTema
import io.github.dadoga31.avisos.ui.EquipoPantalla
import io.github.dadoga31.avisos.ui.FichaPantalla
import io.github.dadoga31.avisos.ui.FormularioPantalla
import io.github.dadoga31.avisos.ui.HechosPantalla
import io.github.dadoga31.avisos.ui.HojaEstado
import io.github.dadoga31.avisos.ui.LocalColores
import io.github.dadoga31.avisos.ui.MarcoSeccion
import io.github.dadoga31.avisos.ui.Seccion
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Capturas de las pantallas de verdad (Compose renderizado por Robolectric),
 * a tamaño de Pixel 7, en claro y oscuro. Van a diseno/capturas-app/.
 * La barra de estado y la de gestos son de mentira, para que se vea como en el móvil.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xhdpi", application = android.app.Application::class)
class CapturasTest {
    private val ahora = LocalDateTime.of(2026, 11, 12, 9, 41, 27)
    private lateinit var avisos: List<Aviso>
    private lateinit var tecnicos: List<Tecnico>
    private val dir = File(System.getProperty("roborazzi.output.dir") ?: "build/capturas").apply { mkdirs() }

    @Before
    fun preparar() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit().clear().commit()
        val db = BaseDatos.enMemoria(ctx)
        val reloj = Clock.fixed(ahora.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
        val repo = Repositorio(db.dao(), AlmacenAjustes(ctx), reloj)
        repo.datosDeEjemplo()
        avisos = db.dao().todosAhora()
        tecnicos = db.dao().tecnicosAhora().sortedBy { if (it.id == Tecnico.YO) "" else it.nombre }
        db.close()
    }

    private fun foto(nombre: String, contenido: @Composable () -> Unit) {
        for (tema in listOf(Tema.CLARO, Tema.OSCURO)) {
            captureRoboImage(File(dir, "$nombre-${tema.id}.png").path) {
                AvisosTema(tema) {
                    /* Quietas: el pulso de «en curso» es infinito y no dejaría terminar la captura. */
                    androidx.compose.runtime.CompositionLocalProvider(
                        io.github.dadoga31.avisos.ui.LocalMovimiento provides false,
                        io.github.dadoga31.avisos.ui.LocalRelojVivo provides false
                    ) {
                        Movil(contenido)
                    }
                }
            }
        }
    }

    @Composable
    private fun Movil(contenido: @Composable () -> Unit) {
        val fondo = LocalColores.current.superficies.pagina
        val tinta = MaterialTheme.colorScheme.onSurface
        Column(Modifier.fillMaxSize().background(fondo)) {
            Row(
                Modifier.fillMaxWidth().height(32.dp).padding(start = 26.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("9:41", Modifier.weight(1f), color = tinta, style = MaterialTheme.typography.labelLarge)
                listOf(Icons.Rounded.SignalCellularAlt, Icons.Rounded.Wifi, Icons.Rounded.Battery6Bar).forEach {
                    Icon(it, null, Modifier.size(17.dp), tint = tinta)
                }
            }
            Box(Modifier.weight(1f)) { contenido() }
            Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(108.dp, 4.dp).background(tinta.copy(alpha = 0.75f), RoundedCornerShape(2.dp)))
            }
        }
    }

    @Composable
    private fun Marco(s: Seccion, contenido: @Composable () -> Unit) =
        MarcoSeccion(s, {}, {}, SnackbarHostState(), contenido = contenido)

    private val enCurso get() = avisos.first { it.estado == "en_curso" }

    @Test
    fun agenda() = foto("1-agenda") {
        Marco(Seccion.AGENDA) {
            AgendaPantalla(avisos, tecnicos, Ajustes(), ahora, AccionesAgenda(), correo = true)
        }
    }

    @Test
    fun agendaBuscando() = foto("2-agenda-buscando") {
        Marco(Seccion.AGENDA) {
            AgendaPantalla(avisos, tecnicos, Ajustes(), ahora, AccionesAgenda(), Reglas.Filtro(texto = "alarma"))
        }
    }

    @Test
    fun ficha() = foto("3-ficha") {
        FichaPantalla(enCurso, tecnicos, ahora, AccionesFicha())
    }

    @Test
    fun hojaEstado() = foto("4-estado") {
        Box(Modifier.fillMaxSize()) {
            FichaPantalla(enCurso, tecnicos, ahora, AccionesFicha())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.36f)))
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(32.dp, 4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    }
                    HojaEstado(enCurso) {}
                }
            }
        }
    }

    @Test
    fun nuevo() = foto("5-nuevo") {
        val conocido = avisos.first { it.cliente.nombre == "Farmacia Centro" }
        FormularioPantalla(
            Aviso(id = "nuevo", titulo = "Central de alarma no comunica", fecha = ahora.toLocalDate().toString(), hora = "17:00",
                asignadoA = Tecnico.YO, prioridad = "urgente", cliente = conocido.cliente),
            esNuevo = true, tecnicos = tecnicos, clientes = Reglas.clientesConocidos(avisos), hoy = ahora.toLocalDate(),
            alGuardar = {}, alCerrar = {}, ref = "AV-0009"
        )
    }

    @Test
    fun hechos() = foto("6-hechos") {
        Marco(Seccion.HECHOS) { HechosPantalla(avisos, tecnicos, ahora) {} }
    }

    @Test
    fun equipo() = foto("7-equipo") {
        Marco(Seccion.EQUIPO) {
            EquipoPantalla(avisos, tecnicos, ahora, {}, {}, { _, _, _ -> }, {}, false, {})
        }
    }

    @Test
    fun ajustes() = foto("8-ajustes") {
        Marco(Seccion.AJUSTES) {
            val instante = ahora.atZone(ZoneId.systemDefault()).toInstant()
            AjustesPantalla(
                Ajustes(), avisos, EstadoBuzon(true, "avisos@empresa.es", imap = true, activo = true, ultima = instante.minusSeconds(180).toString()),
                AccionesAjustes(), instante
            )
        }
    }

    @Test
    fun actualizar() = foto("14-actualizar") {
        Marco(Seccion.AGENDA) {
            AgendaPantalla(
                avisos, tecnicos, Ajustes(pistaGestos = false), ahora, AccionesAgenda(), correo = true,
                actualizacion = io.github.dadoga31.avisos.EstadoActualizacion.Descargando(Actualizador.Version(13, "3.6.0"), 0.42f)
            )
        }
    }

    /** La Agenda entera (más alta que la pantalla): en curso, la línea del día y las bandejas. */
    @Test
    @Config(qualifiers = "w412dp-h1900dp-xhdpi")
    fun agendaCompleta() = foto("16-agenda-completa") {
        Marco(Seccion.AGENDA) { AgendaPantalla(avisos, tecnicos, Ajustes(pistaGestos = false), ahora, AccionesAgenda(), correo = true) }
    }

    /** Como en el móvil del usuario: varios en curso a la vez, llegados del correo y sin hora. */
    @Test
    fun variosEnCurso() = foto("15-en-curso-varios") {
        val empezado = ahora.minusMinutes(12).atZone(ZoneId.systemDefault()).toInstant().toString()
        val extra = listOf(
            Aviso(id = "c1", ref = "AV-0010", titulo = "Sirena exterior no suena", estado = "en_curso", origen = "correo", empezado = empezado,
                fecha = ahora.toLocalDate().toString(), cliente = io.github.dadoga31.avisos.datos.Cliente("Talleres Ruiz", "C/ Industria 4", "912345678")),
            Aviso(id = "c2", ref = "AV-0011", titulo = "Cámara del parking sin imagen", estado = "en_curso", origen = "correo", empezado = empezado,
                fecha = ahora.toLocalDate().toString(), cliente = io.github.dadoga31.avisos.datos.Cliente("Hotel Mirador"))
        )
        Marco(Seccion.AGENDA) {
            AgendaPantalla(avisos + extra, tecnicos, Ajustes(pistaGestos = false), ahora, AccionesAgenda(), correo = true)
        }
    }

    @Test
    fun agendaVacia() = foto("9-agenda-vacia") {
        Marco(Seccion.AGENDA) {
            AgendaPantalla(emptyList(), tecnicos, Ajustes(), ahora, AccionesAgenda())
        }
    }

    @Test
    fun correo() = foto("10-correo") {
        val instante = ahora.atZone(ZoneId.systemDefault()).toInstant()
        CorreoPantalla(
            EstadoBuzon(true, "avisos@empresa.es", imap = true, activo = true, ultima = instante.minusSeconds(180).toString()),
            FormCuenta("avisos@empresa.es", "", "imap.empresa.es"), listOf("@boletines.es", "no-responder@banco.es"),
            AccionesCorreo(), instante
        )
    }

    @Test
    fun fichaDeCorreo() = foto("11-ficha-correo") {
        val m = MensajeCorreo(
            uid = "9", messageId = "<9@naves>", de = "pedro@naves.es", deNombre = "Naves Logísticas Sur",
            asunto = "URGENTE: alarma sin cobertura en nave 3", fecha = "2026-11-12T07:50:00Z",
            texto = "Buenos días,\nLa central no comunica con la CRA desde anoche y la nave está sin cobertura.\n" +
                "Llamad a Pedro al 600 11 22 33.\n\nGracias"
        )
        val a = ConversorCorreo.aAviso(m, "c1", ahora.toLocalDate()).copy(
            ref = "AV-0008",
            adjuntos = listOf(
                Adjunto("f1", "parte-incidencia.pdf", "application/pdf", 182_000, "x"),
                Adjunto("f2", "foto-central.jpg", "image/jpeg", 1_450_000, "y")
            )
        )
        FichaPantalla(a, tecnicos, ahora, AccionesFicha())
    }

    private fun avisoDeCorreo() = ConversorCorreo.aAviso(
        MensajeCorreo(
            uid = "9", messageId = "<9@naves>", de = "pedro@naves.es", deNombre = "Naves Logísticas Sur",
            asunto = "URGENTE: alarma sin cobertura en nave 3", fecha = "2026-11-12T07:50:00Z",
            texto = "Buenos días,\nLa central no comunica con la CRA desde anoche y la nave está sin cobertura.\n" +
                "Llamad a Pedro al 600 11 22 33.\n\nGracias"
        ), "c1", ahora.toLocalDate()
    ).copy(ref = "AV-0008", creado = "2026-11-12T07:50:00Z", fecha = "2026-11-12", hora = "11:30",
        adjuntos = listOf(Adjunto("f1", "parte-incidencia.pdf", "application/pdf", 182_000, "x")))

    @Test
    @Config(qualifiers = "w412dp-h1500dp-xhdpi")
    fun responder() = foto("17-responder") {
        val a = avisoDeCorreo()
        RedactarPantalla(
            a, false, ahora.toLocalDate(), null, envioListo = true, copiaInicial = true, fichero = { File(it.ruta) },
            enviando = false, error = null, alEnviar = { _, _ -> }, alCerrar = {}, alConfigurar = {}
        )
    }

    /** Mantener pulsado el +. */
    @Test
    fun menuMas() = foto("19-menu-mas") {
        MarcoSeccion(Seccion.AGENDA, {}, {}, SnackbarHostState(), menuMasInicial = true) {
            AgendaPantalla(avisos, tecnicos, Ajustes(pistaGestos = false), ahora, AccionesAgenda(), correo = true)
        }
    }

    @Test
    fun correoNuevo() = foto("20-correo-nuevo") {
        RedactarPantalla(
            null, false, ahora.toLocalDate(), null, envioListo = true, copiaInicial = false, fichero = { File(it.ruta) },
            enviando = false, error = null, alEnviar = { _, _ -> }, alCerrar = {}, alConfigurar = {},
            extras = listOf(io.github.dadoga31.avisos.correo.AdjuntoEnvio(File("presupuesto.pdf"), "presupuesto-videoportero.pdf", "application/pdf"))
        )
    }

    /** Ajustes → Correo entero: buzón, envío (SMTP) y firma. */
    @Test
    @Config(qualifiers = "w412dp-h2600dp-xhdpi")
    fun correoEnvio() = foto("18-correo-envio") {
        val instante = ahora.atZone(ZoneId.systemDefault()).toInstant()
        CorreoPantalla(
            EstadoBuzon(true, "avisos@seguridadnorte.es", imap = false, activo = true, ultima = instante.minusSeconds(60).toString()),
            FormCuenta("avisos@seguridadnorte.es", "", "mail.seguridadnorte.es", imap = false, puerto = "995",
                nombre = "Dani · Seguridad Norte", smtpServidor = "mail.seguridadnorte.es", copiaParaMi = true),
            emptyList(), AccionesCorreo(), instante
        )
    }

    /** La ficha entera (más alta que la pantalla) con fotos y un PDF. */
    @Test
    @Config(qualifiers = "w412dp-h1500dp-xhdpi")
    fun fichaConFotos() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val a = enCurso
        val dir = File(ctx.filesDir, "adjuntos/${a.id}").apply { mkdirs() }
        val colores = listOf(0xFF3F5E98.toInt() to 0xFFF27427.toInt(), 0xFF2B6A3C.toInt() to 0xFFE0E2E8.toInt(), 0xFF5C4033.toInt() to 0xFFFFDBCB.toInt())
        val fotos = colores.mapIndexed { i, (fondo, forma) ->
            val bmp = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(bmp).apply {
                drawColor(fondo)
                drawCircle(200f, 220f, 120f - i * 20, android.graphics.Paint().apply { color = forma; isAntiAlias = true })
            }
            val f = File(dir, "foto-$i.jpg").apply { outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) } }
            Adjunto("foto$i", f.name, "image/jpeg", f.length(), "adjuntos/${a.id}/${f.name}")
        }
        val conFotos = a.copy(adjuntos = fotos + Adjunto("pdf", "parte-de-trabajo.pdf", "application/pdf", 182_000, "x"))
        foto("12-ficha-completa") {
            androidx.compose.runtime.CompositionLocalProvider(io.github.dadoga31.avisos.ui.LocalMiniaturasAlMomento provides true) {
                FichaPantalla(conFotos, tecnicos, ahora, AccionesFicha())
            }
        }
    }
}
