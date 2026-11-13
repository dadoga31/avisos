package io.github.dadoga31.avisos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tema
import io.github.dadoga31.avisos.ui.AccionesAgenda
import io.github.dadoga31.avisos.ui.AccionesFicha
import io.github.dadoga31.avisos.ui.AgendaPantalla
import io.github.dadoga31.avisos.ui.AvisosTema
import io.github.dadoga31.avisos.ui.FichaPantalla
import io.github.dadoga31.avisos.ui.LocalMovimiento
import io.github.dadoga31.avisos.ui.LocalRelojVivo
import io.github.dadoga31.avisos.ui.MarcoSeccion
import io.github.dadoga31.avisos.ui.Seccion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDateTime
import java.time.ZoneId

/** Los gestos de la UI nueva (3.5.0): deslizar para cerrar, la línea de tiempo, la semana y el dock. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xhdpi", application = android.app.Application::class)
class GestosTest {
    @get:Rule val regla = createComposeRule()

    private val ahora = LocalDateTime.of(2026, 11, 12, 9, 41, 27)
    private val yo = Tecnico(Tecnico.YO, "Yo")
    private val enCurso = Aviso(
        id = "a1", ref = "AV-0003", titulo = "Mantenimiento anual", fecha = "2026-11-12", hora = "09:00", duracion = "2",
        estado = Estado.EN_CURSO.id, asignadoA = Tecnico.YO, cliente = Cliente("Colegio San Pablo", "Av. de la Paz 3", "913334455"),
        empezado = ahora.minusMinutes(41).atZone(ZoneId.systemDefault()).toInstant().toString()
    )
    private val manana = Aviso(id = "a2", ref = "AV-0004", titulo = "Lector de tarjetas", fecha = "2026-11-13", hora = "08:00", estado = Estado.PROGRAMADO.id)

    private fun pantalla(contenido: @Composable () -> Unit) = regla.setContent {
        AvisosTema(Tema.CLARO) {
            CompositionLocalProvider(LocalMovimiento provides false, LocalRelojVivo provides false) { contenido() }
        }
    }

    @Test
    fun `deslizar hasta el final cierra el aviso en curso`() {
        val estados = mutableListOf<Estado>()
        pantalla { FichaPantalla(enCurso, listOf(yo), ahora, AccionesFicha(estado = { estados += it })) }
        regla.onNodeWithContentDescription("Desliza para cerrar").performTouchInput { swipeRight(startX = centerX, endX = centerX + 2000f) }
        regla.waitForIdle()
        assertEquals(listOf(Estado.RESUELTO), estados)
    }

    @Test
    fun `soltar a medias no cierra nada`() {
        val estados = mutableListOf<Estado>()
        pantalla { FichaPantalla(enCurso, listOf(yo), ahora, AccionesFicha(estado = { estados += it })) }
        regla.onNodeWithContentDescription("Desliza para cerrar").performTouchInput { swipeRight(startX = centerX, endX = centerX + 120f) }
        regla.waitForIdle()
        assertEquals(emptyList<Estado>(), estados)
    }

    @Test
    fun `sin empezar, el mismo gesto lo empieza`() {
        val estados = mutableListOf<Estado>()
        pantalla { FichaPantalla(manana, listOf(yo), ahora, AccionesFicha(estado = { estados += it })) }
        regla.onNodeWithContentDescription("Desliza para empezar").performTouchInput { swipeRight(startX = centerX, endX = centerX + 2000f) }
        regla.waitForIdle()
        assertEquals(listOf(Estado.EN_CURSO), estados)
    }

    private val luego = Aviso(
        id = "a3", ref = "AV-0005", titulo = "Sustitucion de camaras", fecha = "2026-11-12", hora = "11:30", duracion = "1,5",
        estado = Estado.PROGRAMADO.id, asignadoA = Tecnico.YO, cliente = Cliente("Naves Logisticas Sur")
    )

    @Test
    fun `lo que esta en curso sale arriba en grande con su boton de hecho`() {
        val abiertos = mutableListOf<String>()
        val cambios = mutableListOf<Pair<String, Estado>>()
        pantalla {
            AgendaPantalla(
                listOf(enCurso, manana, luego), listOf(yo), Ajustes(pistaGestos = false), ahora,
                AccionesAgenda(abrir = { abiertos += it }, cambiarEstado = { a, e -> cambios += a.id to e })
            )
        }
        regla.onNodeWithText("Mantenimiento anual").assertIsDisplayed().performClick()
        assertEquals(listOf("a1"), abiertos)
        regla.onNodeWithText("Hecho").performClick()
        assertEquals(listOf("a1" to Estado.RESUELTO), cambios)
    }

    @Test
    fun `en la linea, deslizar del todo a la izquierda da por hecho`() {
        val cambios = mutableListOf<Pair<String, Estado>>()
        pantalla {
            AgendaPantalla(
                listOf(enCurso, luego), listOf(yo), Ajustes(pistaGestos = false), ahora,
                AccionesAgenda(cambiarEstado = { a, e -> cambios += a.id to e })
            )
        }
        regla.onNodeWithText("Sustitucion de camaras").performTouchInput { swipeLeft(startX = right - 5f, endX = left - 400f) }
        regla.waitForIdle()
        assertEquals(listOf("a3" to Estado.RESUELTO), cambios)
    }

    @Test
    fun `deslizar a medias descubre cancelar y cancelar lo manda borrar`() {
        val cancelados = mutableListOf<String>()
        val cambios = mutableListOf<Pair<String, Estado>>()
        pantalla {
            AgendaPantalla(
                listOf(enCurso, luego), listOf(yo), Ajustes(pistaGestos = false), ahora,
                AccionesAgenda(cancelar = { cancelados += it.id }, cambiarEstado = { a, e -> cambios += a.id to e })
            )
        }
        regla.onNodeWithText("Sustitucion de camaras").performTouchInput { swipeLeft(startX = right - 5f, endX = right - 5f - 160.dp.toPx()) }
        regla.waitForIdle()
        regla.onNodeWithContentDescription("Cancelar aviso").assertIsDisplayed().performClick()
        regla.waitForIdle()
        assertEquals(listOf("a3"), cancelados)
        assertEquals(emptyList<Pair<String, Estado>>(), cambios)
    }

    @Test
    fun `deslizar a la derecha lo pone en curso`() {
        val cambios = mutableListOf<Pair<String, Estado>>()
        pantalla {
            AgendaPantalla(
                listOf(luego), listOf(yo), Ajustes(pistaGestos = false), ahora,
                AccionesAgenda(cambiarEstado = { a, e -> cambios += a.id to e })
            )
        }
        regla.onNodeWithText("Sustitucion de camaras").performTouchInput { swipeRight(startX = left + 5f, endX = right) }
        regla.waitForIdle()
        assertEquals(listOf("a3" to Estado.EN_CURSO), cambios)
    }

    @Test
    fun `tocar otro dia de la semana pinta su linea`() {
        pantalla { AgendaPantalla(listOf(enCurso, manana), listOf(yo), Ajustes(pistaGestos = false), ahora, AccionesAgenda()) }
        regla.onNodeWithText("13").performClick()
        regla.waitForIdle()
        regla.onNodeWithText("Lector de tarjetas").assertIsDisplayed()
    }

    @Test
    fun `el dock cambia de seccion y el + crea un aviso`() {
        val idas = mutableListOf<Seccion>()
        var nuevos = 0
        pantalla { MarcoSeccion(Seccion.AGENDA, { idas += it }, { nuevos++ }, SnackbarHostState()) {} }
        regla.onNodeWithContentDescription("Hechos").performClick()
        regla.onNodeWithContentDescription("Ajustes").performClick()
        regla.onNodeWithContentDescription("Nuevo aviso").performClick()
        assertEquals(listOf(Seccion.HECHOS, Seccion.AJUSTES), idas)
        assertEquals(1, nuevos)
    }
}
