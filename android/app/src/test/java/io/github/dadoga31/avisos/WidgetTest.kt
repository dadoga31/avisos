package io.github.dadoga31.avisos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** El widget de verdad (el mismo RemoteViews que recibe el lanzador), pintado sobre un fondo. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xhdpi", application = android.app.Application::class)
class WidgetTest {
    private val dir = File(System.getProperty("roborazzi.output.dir") ?: "build/capturas").apply { mkdirs() }

    private fun datos(ctx: Context, dia: String = LocalDate.now().toString()) {
        ctx.getSharedPreferences(WidgetAvisos.PREFS, Context.MODE_PRIVATE).edit()
            .putString(WidgetAvisos.CLAVE_DIA, dia).putInt(WidgetAvisos.CLAVE_HOY, 2)
            .putInt(WidgetAvisos.CLAVE_VENCIDOS, 1).putInt(WidgetAvisos.CLAVE_HECHOS, 1)
            .putString(WidgetAvisos.CLAVE_SIGUIENTE_ID, "av_x")
            .putString(WidgetAvisos.CLAVE_SIGUIENTE_CABECERA, "SIGUIENTE · 11:00")
            .putString(WidgetAvisos.CLAVE_SIGUIENTE_TEXTO, "Naves Logísticas Sur · Sustitución de 2 cámaras exteriores")
            .commit()
    }

    private fun pintar(ctx: Context, archivo: String): View {
        val d = ctx.resources.displayMetrics.density
        val marco = FrameLayout(ctx).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF2B3D6B.toInt(), 0xFF6D4A7D.toInt(), 0xFFC27B5A.toInt())
            )
            setPadding((24 * d).toInt(), (40 * d).toInt(), (24 * d).toInt(), (40 * d).toInt())
        }
        val widget = WidgetAvisos.vistas(ctx).apply(ctx, marco)
        marco.addView(widget, FrameLayout.LayoutParams((330 * d).toInt(), (190 * d).toInt()))
        val ancho = (378 * d).toInt()
        val alto = (270 * d).toInt()
        marco.measure(View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(alto, View.MeasureSpec.EXACTLY))
        marco.layout(0, 0, ancho, alto)
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        marco.draw(Canvas(bmp))
        File(dir, archivo).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return widget
    }

    @Test
    fun `widget con lo de hoy, en claro y en oscuro`() {
        var ctx = ApplicationProvider.getApplicationContext<Context>()
        datos(ctx)
        val v = pintar(ctx, "13-widget-claro.png")
        assertEquals("2", v.findViewById<TextView>(R.id.widget_numero).text.toString())
        assertEquals("1 vencido", v.findViewById<TextView>(R.id.widget_vencidos).text.toString())
        assertEquals(View.VISIBLE, v.findViewById<View>(R.id.widget_siguiente).visibility)

        RuntimeEnvironment.setQualifiers("+night")
        ctx = ApplicationProvider.getApplicationContext()
        pintar(ctx, "13-widget-oscuro.png")
    }

    @Test
    fun `con datos de otro dia no enseña numeros viejos`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        datos(ctx, dia = "2020-01-01")
        val v = pintar(ctx, "13-widget-sin-datos.png")
        assertEquals("–", v.findViewById<TextView>(R.id.widget_numero).text.toString())
        assertEquals(View.GONE, v.findViewById<View>(R.id.widget_siguiente).visibility)
        File(dir, "13-widget-sin-datos.png").delete()
    }
}
