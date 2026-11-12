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
import io.github.dadoga31.avisos.ui.AccionesAgenda
import io.github.dadoga31.avisos.ui.AccionesAjustes
import io.github.dadoga31.avisos.ui.AccionesFicha
import io.github.dadoga31.avisos.ui.AgendaPantalla
import io.github.dadoga31.avisos.ui.AjustesPantalla
import io.github.dadoga31.avisos.ui.AvisosTema
import io.github.dadoga31.avisos.ui.BotonFlotante
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
    private val ahora = LocalDateTime.of(2026, 11, 12, 9, 41)
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
                AvisosTema(tema) { Movil(contenido) }
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
    private fun Marco(s: Seccion, boton: BotonFlotante?, contenido: @Composable () -> Unit) =
        MarcoSeccion(s, {}, Reglas.resumen(avisos, ahora.toLocalDate()).vencidos, boton, SnackbarHostState(), contenido = contenido)

    private val enCurso get() = avisos.first { it.estado == "en_curso" }

    @Test
    fun agenda() = foto("1-agenda") {
        Marco(Seccion.AGENDA, BotonFlotante(Icons.Rounded.Add, "Nuevo aviso") {}) {
            AgendaPantalla(avisos, tecnicos, Ajustes(), ahora, AccionesAgenda())
        }
    }

    @Test
    fun agendaBuscando() = foto("2-agenda-buscando") {
        Marco(Seccion.AGENDA, BotonFlotante(Icons.Rounded.Add, "Nuevo aviso") {}) {
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
            alGuardar = {}, alCerrar = {}
        )
    }

    @Test
    fun hechos() = foto("6-hechos") {
        Marco(Seccion.HECHOS, null) { HechosPantalla(avisos, tecnicos, ahora) {} }
    }

    @Test
    fun equipo() = foto("7-equipo") {
        Marco(Seccion.EQUIPO, BotonFlotante(Icons.Rounded.PersonAdd, "Añadir técnico") {}) {
            EquipoPantalla(avisos, tecnicos, ahora, {}, {}, { _, _, _ -> }, {}, false, {})
        }
    }

    @Test
    fun ajustes() = foto("8-ajustes") {
        Marco(Seccion.AJUSTES, null) { AjustesPantalla(Ajustes(), avisos, null, AccionesAjustes()) }
    }

    @Test
    fun agendaVacia() = foto("9-agenda-vacia") {
        Marco(Seccion.AGENDA, BotonFlotante(Icons.Rounded.Add, "Nuevo aviso") {}) {
            AgendaPantalla(emptyList(), tecnicos, Ajustes(), ahora, AccionesAgenda())
        }
    }
}
