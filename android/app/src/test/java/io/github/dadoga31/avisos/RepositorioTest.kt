package io.github.dadoga31.avisos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.BaseDatos
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RepositorioTest {
    private lateinit var db: BaseDatos
    private lateinit var repo: Repositorio

    @Before
    fun preparar() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit().clear().commit()
        db = BaseDatos.enMemoria(ctx)
        repo = Repositorio(db.dao(), AlmacenAjustes(ctx))
    }

    @After
    fun cerrar() = db.close()

    @Test
    fun `guardar pone referencia correlativa y fecha de creacion`() = runBlocking {
        val a = repo.guardar(repo.nuevo().copy(titulo = "Uno"))
        val b = repo.guardar(repo.nuevo().copy(titulo = "Dos"))
        assertEquals("AV-0001", a.ref)
        assertEquals("AV-0002", b.ref)
        assertTrue(a.creado.isNotEmpty())
        val otra = repo.guardar(a.copy(titulo = "Uno bis"))
        assertEquals("AV-0001", otra.ref)
        assertEquals(a.creado, otra.creado)
    }

    @Test
    fun `cerrar pone fecha de cierre, reabrir la quita y en curso apunta cuando empezo`() = runBlocking {
        val a = repo.guardar(repo.nuevo().copy(titulo = "Uno"))
        repo.cambiarEstado(a.id, Estado.EN_CURSO)
        assertTrue(repo.porId(a.id)!!.empezado.isNotEmpty())
        repo.cambiarEstado(a.id, Estado.RESUELTO)
        assertTrue(repo.porId(a.id)!!.cerrado.isNotEmpty())
        repo.cambiarEstado(a.id, Estado.PENDIENTE)
        assertEquals("", repo.porId(a.id)!!.cerrado)
    }

    @Test
    fun `deshacer deja el aviso exactamente como estaba`() = runBlocking {
        val a = repo.guardar(repo.nuevo().copy(titulo = "Uno"))
        val antes = repo.cambiarEstado(a.id, Estado.RESUELTO)!!
        repo.restaurar(antes)
        assertEquals(a, repo.porId(a.id))
    }

    @Test
    fun `notas, horas y borrar un tecnico lo quita de sus avisos`() = runBlocking {
        repo.asegurarYo()
        repo.guardarTecnico("Laura Pinto", "600445566")
        val laura = db.dao().tecnicosAhora().first { it.id != Tecnico.YO }
        val a = repo.guardar(repo.nuevo().copy(titulo = "Uno", asignadoA = laura.id))
        repo.anadirNota(a.id, "  Falta el sótano  ")
        repo.sumarHoras(a.id, 0.5)
        repo.sumarHoras(a.id, 1.0)
        repo.sumarHoras(a.id, -5.0)
        repo.borrarTecnico(laura.id)
        val b = repo.porId(a.id)!!
        assertEquals("Falta el sótano", b.notas.single().texto)
        assertEquals(0.0, b.horas, 0.0)
        assertEquals("", b.asignadoA)
        repo.borrarTecnico(Tecnico.YO)
        assertTrue(db.dao().tecnicosAhora().any { it.id == Tecnico.YO })
    }

    @Test
    fun `los datos de ejemplo siguen la numeracion`() = runBlocking {
        repo.guardar(repo.nuevo().copy(titulo = "Primero"))
        repo.datosDeEjemplo()
        val refs = db.dao().todosAhora().map { it.ref }.sorted()
        assertEquals((1..7).map { "AV-" + it.toString().padStart(4, '0') }, refs)
    }
}
