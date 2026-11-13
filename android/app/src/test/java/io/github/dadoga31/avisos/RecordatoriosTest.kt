package io.github.dadoga31.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Con la app de verdad (AvisosApp): guardar → alarma → notificación. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordatoriosTest {
    private lateinit var app: AvisosApp
    private lateinit var am: AlarmManager
    private lateinit var nm: NotificationManager

    @Before
    fun preparar() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        am = app.getSystemService(AlarmManager::class.java)
        nm = app.getSystemService(NotificationManager::class.java)
        app.ajustes.cambiar { it.copy(recordatorio = 20) }
        runBlocking { app.repositorio.borrarTodos() }
    }

    private fun aviso(id: String, dentroDeMin: Long, asignado: String = Tecnico.YO): Aviso {
        val cita = LocalDateTime.now().plusMinutes(dentroDeMin).withSecond(0).withNano(0)
        return Aviso(
            id = id, titulo = "Sustitución de 2 cámaras", fecha = cita.toLocalDate().toString(),
            hora = cita.format(DateTimeFormatter.ofPattern("HH:mm")), asignadoA = asignado, estado = Estado.PROGRAMADO.id,
            cliente = Cliente(nombre = "Naves Logísticas Sur", direccion = "Pol. Ind. El Prado, nave 7")
        )
    }

    /* Solo las de los recordatorios: la de buscar versión nueva también está programada. */
    private fun alarmas() = shadowOf(am).scheduledAlarms.filter {
        shadowOf(it.operation).savedIntent.component?.className != ReceptorActualizacion::class.java.name
    }

    /** Entrega la alarma como lo haría Android y espera a que el receptor termine. */
    private fun disparar(alarma: ShadowAlarmManager.ScheduledAlarm) = disparar(shadowOf(alarma.operation).savedIntent)

    private fun disparar(intento: android.content.Intent) {
        app.sendBroadcast(intento)
        repeat(50) {
            shadowOf(Looper.getMainLooper()).idle()
            if (nm.activeNotifications.isNotEmpty()) return
            Thread.sleep(50)
        }
    }

    @Test
    fun `guardar programa la alarma y al saltar avisa con Ir, Empezar y Avisar en 15 min`() = runBlocking {
        val a = app.repositorio.guardar(aviso("a1", 60))
        val alarma = alarmas().single()
        assertEquals(Reglas.momentoRecordatorio(a, 20)!!.toEpochMilli(), alarma.triggerAtMs)

        disparar(alarma)

        val n = nm.activeNotifications.single().notification
        val titulo = n.extras.getString("android.title")!!
        assertTrue(titulo, titulo.startsWith("En ") && titulo.endsWith("· Naves Logísticas Sur"))
        assertEquals(listOf("Ir", "Empezar", "Avisar en 15 min"), n.actions.map { it.title.toString() })
    }

    @Test
    fun `una alarma vieja no avisa si el aviso cambio de hora`() = runBlocking {
        val a = app.repositorio.guardar(aviso("a1", 60))
        /* Copia del intent tal como estaba: Android actualiza los extras de la alarma existente. */
        val vieja = android.content.Intent(shadowOf(alarmas().single().operation).savedIntent)
        val b = app.repositorio.guardar(a.copy(hora = aviso("x", 120).hora, fecha = aviso("x", 120).fecha))
        assertEquals(Reglas.momentoRecordatorio(b, 20)!!.toEpochMilli(), alarmas().single().triggerAtMs)
        disparar(vieja)
        assertTrue(nm.activeNotifications.isEmpty())
    }

    @Test
    fun `cerrar el aviso quita la alarma y los de otro tecnico no llevan`() = runBlocking {
        val a = app.repositorio.guardar(aviso("a1", 60))
        app.repositorio.guardar(aviso("a2", 90, asignado = "laura"))
        assertEquals(1, alarmas().size)
        app.repositorio.cambiarEstado(a.id, Estado.RESUELTO)
        assertTrue(alarmas().isEmpty())
    }
}
