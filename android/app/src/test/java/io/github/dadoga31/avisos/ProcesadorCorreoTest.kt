package io.github.dadoga31.avisos

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dadoga31.avisos.correo.AlmacenCorreo
import io.github.dadoga31.avisos.correo.Mensaje
import io.github.dadoga31.avisos.correo.ProcesadorCorreo
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.BaseDatos
import io.github.dadoga31.avisos.datos.Repositorio
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ProcesadorCorreoTest {
    private lateinit var ctx: Context
    private lateinit var db: BaseDatos
    private lateinit var repo: Repositorio
    private lateinit var almacen: AlmacenCorreo

    @get:Rule
    val migraciones = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), BaseDatos::class.java)

    @Before
    fun preparar() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit().clear().commit()
        db = BaseDatos.enMemoria(ctx)
        repo = Repositorio(db.dao(), AlmacenAjustes(ctx), carpeta = ctx.filesDir)
        almacen = AlmacenCorreo(ctx).apply { limpiarTodo() }
        File(ctx.filesDir, "adjuntos").deleteRecursively()
    }

    @After
    fun cerrar() = db.close()

    private fun mensaje(uid: String, de: String, asunto: String, messageId: String = "<$uid@empresa>") = Mensaje().apply {
        this.uid = uid; this.messageId = messageId; this.de = de; this.asunto = asunto
        this.fecha = "2026-11-12T08:00:00Z"; this.texto = "La central no comunica. Teléfono 600 11 22 33"
    }

    private fun procesar() = runBlocking { ProcesadorCorreo.procesar(ctx, repo, almacen, listOf("@boletines.es")) }

    @Test
    fun `convierte, salta los ignorados y conserva los adjuntos`() {
        almacen.guardarAdjunto("adj1", "%PDF-1.4 parte de trabajo".byteInputStream())
        val conAdjunto = mensaje("1", "pedro@naves.es", "Avería alarma nave 3").apply {
            adjuntos.add(Mensaje.Adjunto().apply { id = "adj1"; nombre = "parte.pdf"; mime = "application/pdf"; tam = 25 })
        }
        almacen.anadirPendientes(listOf(conAdjunto, mensaje("2", "ofertas@boletines.es", "Ofertas de noviembre")))

        val r = procesar()

        assertEquals(1, r.creados.size)
        assertEquals(1, r.saltados)
        val a = runBlocking { db.dao().todosAhora() }.single()
        assertEquals("Avería alarma nave 3", a.titulo)
        assertEquals("correo", a.origen)
        assertEquals("m:<1@empresa>", a.correoId)
        assertEquals("600112233", a.cliente.telefono)
        assertEquals("AV-0001", a.ref)
        val adj = a.adjuntos.single()
        assertEquals("parte.pdf", adj.nombre)
        assertTrue(File(ctx.filesDir, adj.ruta).readText().startsWith("%PDF"))
        assertEquals(0, almacen.cuantosPendientes())
        assertFalse("el almacén del correo borra su copia", almacen.ficheroAdjunto("adj1")!!.exists())

        /* Borrar el aviso se lleva sus adjuntos. */
        runBlocking { repo.borrar(a.id) }
        assertFalse(File(ctx.filesDir, adj.ruta).exists())
    }

    @Test
    fun `el mismo correo no crea dos avisos aunque vuelva a llegar`() {
        almacen.anadirPendientes(listOf(mensaje("1", "pedro@naves.es", "Avería")))
        assertEquals(1, procesar().creados.size)
        /* Vuelve a llegar (por ejemplo, al releer los últimos 20). */
        almacen.anadirPendientes(listOf(mensaje("1", "pedro@naves.es", "Avería")))
        val r = procesar()
        assertEquals(0, r.creados.size)
        assertEquals(1, r.saltados)
        assertEquals(1, runBlocking { db.dao().todosAhora() }.size)
        assertEquals(0, almacen.cuantosPendientes())
    }

    @Test
    fun `dos pasadas a la vez no duplican (el fallo de los correos que entraban dos veces)`() {
        almacen.anadirPendientes((1..5).map { mensaje("$it", "cliente$it@x.es", "Aviso $it") })
        val resultados = runBlocking {
            listOf(
                async { ProcesadorCorreo.procesar(ctx, repo, almacen, emptyList()) },
                async { ProcesadorCorreo.procesar(ctx, repo, almacen, emptyList()) }
            ).awaitAll()
        }
        assertEquals(5, resultados.sumOf { it.creados.size })
        assertEquals(5, runBlocking { db.dao().todosAhora() }.map { it.correoId }.distinct().size)
    }

    @Test
    fun `la migracion de la 3_0_0 conserva los avisos`() {
        val nombre = "migracion.db"
        migraciones.createDatabase(nombre, 1).apply {
            execSQL(
                "INSERT INTO avisos (id, ref, titulo, tipo, sistema, prioridad, estado, origen, correoId, " +
                    "cliente_nombre, cliente_direccion, cliente_telefono, cliente_contacto, fecha, hora, duracion, " +
                    "asignadoA, descripcion, notas, horas, creado, actualizado, empezado, cerrado) VALUES " +
                    "('av1','AV-0001','Antiguo','averia','alarma','normal','pendiente','manual','', 'Farmacia','','','', " +
                    "'2026-11-12','','','yo','','[]',1.5,'','','','')"
            )
            close()
        }
        migraciones.runMigrationsAndValidate(nombre, 2, true).close()
        val db2 = androidx.room.Room.databaseBuilder(ctx, BaseDatos::class.java, nombre).allowMainThreadQueries().build()
        val a = runBlocking { db2.dao().porId("av1") }!!
        assertEquals("Antiguo", a.titulo)
        assertEquals(1.5, a.horas, 0.0)
        assertTrue(a.adjuntos.isEmpty())
        db2.close()
    }
}
