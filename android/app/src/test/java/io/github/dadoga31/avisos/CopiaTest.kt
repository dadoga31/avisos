package io.github.dadoga31.avisos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.BaseDatos
import io.github.dadoga31.avisos.datos.CopiaSeguridad
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Repositorio
import io.github.dadoga31.avisos.datos.Tecnico
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CopiaTest {
    private lateinit var ctx: Context
    private val abiertas = mutableListOf<BaseDatos>()

    @Before
    fun preparar() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cerrar() = abiertas.forEach { it.close() }

    /** Un móvil limpio: base de datos, ajustes y carpeta propios. */
    private fun movil(nombre: String): Pair<Repositorio, BaseDatos> {
        ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit().clear().commit()
        val db = BaseDatos.enMemoria(ctx).also { abiertas += it }
        val carpeta = File(ctx.filesDir, nombre).apply { deleteRecursively(); mkdirs() }
        return Repositorio(db.dao(), AlmacenAjustes(ctx), carpeta = carpeta) to db
    }

    private val copiaWeb: String get() = javaClass.classLoader!!.getResource("copia-web.json")!!.readText()

    @Test
    fun `recupera una copia de la app web`() = runBlocking {
        val (repo, db) = movil("web")
        repo.asegurarYo()
        val r = repo.importar(CopiaSeguridad.leer(copiaWeb))
        assertEquals(6, r.avisos)
        assertEquals(2, r.tecnicos)        // el «Yo» de la web se junta con el de aquí
        assertEquals(1, r.ficheros)

        val avisos = db.dao().todosAhora()
        val tecnicos = db.dao().tecnicosAhora()
        assertEquals(listOf("Laura Pinto", "Marcos Rey", "Yo"), tecnicos.map { it.nombre }.sorted())
        val farmacia = avisos.first { it.cliente.nombre == "Farmacia Centro" }
        assertEquals(Tecnico.YO, farmacia.asignadoA)
        assertEquals("m:<abc@empresa.es>", farmacia.correoId)
        val resuelto = avisos.first { it.estadoE == Estado.RESUELTO }
        assertEquals(3.5, resuelto.horas, 0.0)
        assertTrue(resuelto.descripcion.contains("Detector PIR grado 2 × 4"))
        assertEquals("Trabajo terminado y firmado por el cliente.", resuelto.notas.single().texto)
        val foto = resuelto.adjuntos.single()
        assertEquals("JPEGDEPRUEBA", File(File(ctx.filesDir, "web"), foto.ruta).readText())
        assertEquals("AV-0007", repo.guardar(repo.nuevo().copy(titulo = "Siguiente")).ref)
    }

    @Test
    fun `guardar y recuperar una copia nativa no pierde nada`() = runBlocking {
        val (origen, dbOrigen) = movil("origen")
        origen.datosDeEjemplo()
        val conFoto = dbOrigen.dao().todosAhora().first { it.estadoE == Estado.EN_CURSO }
        File(File(ctx.filesDir, "origen"), "adjuntos/${conFoto.id}").mkdirs()
        File(File(ctx.filesDir, "origen"), "adjuntos/${conFoto.id}/0-foto.jpg").writeText("FOTO")
        dbOrigen.dao().guardar(conFoto.copy(adjuntos = listOf(
            io.github.dadoga31.avisos.datos.Adjunto("f1", "foto.jpg", "image/jpeg", 4, "adjuntos/${conFoto.id}/0-foto.jpg")
        )))
        val json = origen.exportar()

        val (destino, dbDestino) = movil("destino")
        destino.asegurarYo()
        val r = destino.importar(CopiaSeguridad.leer(json))
        assertEquals(6, r.avisos)
        val antes = dbOrigen.dao().todosAhora().sortedBy { it.id }
        val despues = dbDestino.dao().todosAhora().sortedBy { it.id }
        /* Igual salvo la ruta del fichero, que ahora está en otra carpeta. */
        assertEquals(antes.map { it.copy(adjuntos = emptyList()) }, despues.map { it.copy(adjuntos = emptyList()) })
        val adj = despues.first { it.id == conFoto.id }.adjuntos.single()
        assertEquals("FOTO", File(File(ctx.filesDir, "destino"), adj.ruta).readText())
        assertEquals(dbOrigen.dao().tecnicosAhora().sortedBy { it.id }, dbDestino.dao().tecnicosAhora().sortedBy { it.id })
    }

    @Test
    fun `recuperar dos veces no duplica y un archivo cualquiera no vale`() = runBlocking {
        val (repo, db) = movil("dos")
        repo.importar(CopiaSeguridad.leer(copiaWeb))
        val r = repo.importar(CopiaSeguridad.leer(copiaWeb))
        assertEquals(0, r.avisos)
        assertEquals(6, db.dao().todosAhora().size)
        val error = runCatching { CopiaSeguridad.leer("{\"hola\": 1}") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(runCatching { CopiaSeguridad.leer("no es json") }.exceptionOrNull() is IllegalArgumentException)
    }
}
