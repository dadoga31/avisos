package io.github.dadoga31.avisos

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/** El aviso de versión nueva: leer version.json, comparar, notificar una vez y programar sin retrasar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ActualizadorTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val nm get() = ctx.getSystemService(NotificationManager::class.java)

    @Before
    fun limpiar() {
        ctx.getSharedPreferences("actualizacion", Context.MODE_PRIVATE).edit().clear().commit()
        shadowOf(ctx as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun `lee el version json que publica el workflow`() {
        assertEquals(Actualizador.Version(13, "3.6.0"), Actualizador.leer("""{"versionCode": 13, "versionName": "3.6.0"}"""))
        assertNull(Actualizador.leer("<html>Not Found</html>"))
        assertNull(Actualizador.leer("""{"versionCode": 0, "versionName": "x"}"""))
        assertNull(Actualizador.leer("""{"versionName": "3.6.0"}"""))
    }

    @Test
    fun `solo hay nueva si el versionCode es mayor que el instalado`() {
        assertTrue(Actualizador.hayNueva(Actualizador.Version(14, "3.7.0"), actual = 13))
        assertFalse(Actualizador.hayNueva(Actualizador.Version(13, "3.6.0"), actual = 13))
        assertFalse(Actualizador.hayNueva(Actualizador.Version(12, "3.5.0"), actual = 13))
        assertFalse(Actualizador.hayNueva(null, actual = 13))
    }

    @Test
    fun `avisa una sola vez por version`() {
        Actualizador.notificar(ctx, Actualizador.Version(20, "9.0.0"))
        assertEquals(1, shadowOf(nm).allNotifications.size)
        val n = shadowOf(nm).allNotifications.single()
        assertEquals("Avisos 9.0.0 disponible", shadowOf(n).contentTitle)
        nm.cancelAll()
        Actualizador.notificar(ctx, Actualizador.Version(20, "9.0.0"))
        assertEquals(0, shadowOf(nm).allNotifications.size)
        Actualizador.notificar(ctx, Actualizador.Version(21, "9.1.0"))
        assertEquals(1, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun `programar dos veces no retrasa la comprobacion`() {
        val am = shadowOf(ctx.getSystemService(android.app.AlarmManager::class.java))
        Actualizador.programar(ctx)
        val primera = am.scheduledAlarms.single().triggerAtTime
        Actualizador.programar(ctx)
        assertEquals(1, am.scheduledAlarms.size)
        assertEquals(primera, am.scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun `borra las descargas a medias y lo que no es una apk de la app`() {
        val dir = File(ctx.cacheDir, "actualizacion").apply { mkdirs() }
        val parcial = File(dir, "avisos.apk.part").apply { writeText("a medias") }
        val basura = File(dir, "avisos.apk").apply { writeText("no es una apk") }
        Actualizador.limpiar(ctx)
        assertFalse(parcial.exists())
        assertFalse(basura.exists())
    }
}
