package io.github.dadoga31.avisos

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tema
import io.github.dadoga31.avisos.ui.AppAvisos
import io.github.dadoga31.avisos.ui.AvisosTema
import io.github.dadoga31.avisos.ui.AvisosVM
import io.github.dadoga31.avisos.ui.LocalMovimiento
import io.github.dadoga31.avisos.ui.LocalRelojVivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Fallo contado por el usuario (3.6.0): al borrar un aviso desde su ficha la
 * app se quedaba en blanco. La ficha, mientras salía, veía que su aviso ya no
 * existía y volvía atrás otra vez: se llevaba también la Agenda.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xhdpi")
class BorrarTest {
    @get:Rule val regla = createComposeRule()

    @Test
    fun `borrar desde la ficha lo borra y vuelve a la agenda`() {
        val app = ApplicationProvider.getApplicationContext<AvisosApp>()
        runBlocking {
            app.repositorio.borrarTodos()
            app.repositorio.asegurarYo()
            app.repositorio.guardar(Aviso(id = "b1", titulo = "Aviso para borrar", fecha = LocalDate.now().toString(), hora = "10:00", asignadoA = Tecnico.YO))
        }
        val vm = AvisosVM(app)
        val ruta = MutableStateFlow<String?>("aviso/b1")
        regla.setContent {
            AvisosTema(Tema.CLARO) {
                CompositionLocalProvider(LocalMovimiento provides false, LocalRelojVivo provides false) {
                    val r by ruta.collectAsState()
                    AppAvisos(vm, r) { ruta.value = null }
                }
            }
        }
        regla.waitUntil(5_000) { regla.onAllNodesWithText("Aviso para borrar").fetchSemanticsNodes().isNotEmpty() }
        regla.onNodeWithContentDescription("Más").performClick()
        regla.onNodeWithText("Borrar").performClick()
        regla.waitUntil(5_000) { regla.onAllNodesWithText("¿Borrar", substring = true).fetchSemanticsNodes().isNotEmpty() }
        regla.waitForIdle()

        /* Que el borrado llegue a la ficha mientras aún está saliendo, como en el móvil. */
        regla.mainClock.autoAdvance = false
        regla.onNodeWithText("Borrar").performClick()
        val limite = System.currentTimeMillis() + 5_000
        while (runBlocking { app.repositorio.porId("b1") } != null && System.currentTimeMillis() < limite) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        repeat(30) { regla.mainClock.advanceTimeByFrame() }
        regla.mainClock.autoAdvance = true
        regla.waitForIdle()

        assertNull(runBlocking { app.repositorio.porId("b1") })
        /* Sigue la Agenda con su buscador y el dock. */
        regla.onNodeWithContentDescription("Buscar").assertExists()
        regla.onNodeWithContentDescription("Nuevo aviso").assertExists()
    }
}
