package io.github.dadoga31.avisos.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * Movimiento de la app: muelles comunes, pulsar que encoge, entrada
 * escalonada, cifras que cuentan, el pulso de lo que está en curso y la
 * ficha que crece desde la fila. Se apaga con «Quitar animaciones» del
 * sistema y en las capturas (LocalMovimiento = false).
 */

object Muelles {
    /** Pantallas, hojas, indicadores: sin rebote. */
    fun <T> suave(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 380f)

    /** Lo que se desliza (menú, fichas seleccionadas): un punto de vida. */
    fun <T> deslizar(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 520f)

    /** Al soltar: rebote corto. */
    fun <T> soltar(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 420f)
}

/** false en las capturas y con las animaciones del sistema quitadas. */
val LocalMovimiento = staticCompositionLocalOf { true }

/** ¿Ha quitado el usuario las animaciones en el móvil? */
@Composable
fun movimientoDelSistema(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
}

/** Encoge un poco mientras se pulsa y vuelve con rebote al soltar. */
@Composable
fun Modifier.presionable(interaccion: MutableInteractionSource, escala: Float = 0.97f): Modifier {
    if (!LocalMovimiento.current) return this
    val pulsado by interaccion.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pulsado) escala else 1f,
        if (pulsado) spring(stiffness = Spring.StiffnessHigh) else Muelles.soltar(),
        label = "pulsar"
    )
    return graphicsLayer { scaleX = s; scaleY = s }
}

/** Momento en que se abrió la pantalla: solo lo que aparece al entrar se anima. */
val LocalInicioPantalla = staticCompositionLocalOf { 0L }

/**
 * Aparece subiendo y con fundido, con un retraso según su posición. Solo
 * en los primeros instantes de la pantalla: lo que llega al hacer scroll
 * aparece sin más, que si no marea.
 */
@Composable
fun Modifier.entrada(indice: Int): Modifier {
    val activo = LocalMovimiento.current
    val inicio = LocalInicioPantalla.current
    val anima = remember { activo && inicio > 0 && System.currentTimeMillis() - inicio < 650 }
    val p = remember { Animatable(if (anima) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (anima) {
            delay(minOf(indice, 10) * 40L)
            p.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 18.dp.toPx()
    }
}

/** Una cifra que cuenta desde 0 hasta su valor la primera vez, y luego entre valores. */
@Composable
fun Cifra(valor: Int, estilo: TextStyle, color: Color = Color.Unspecified, sufijo: String = "") {
    val activo = LocalMovimiento.current
    val a = remember { Animatable(if (activo) 0f else valor.toFloat()) }
    LaunchedEffect(valor) {
        if (activo) a.animateTo(valor.toFloat(), tween(650, easing = FastOutSlowInEasing)) else a.snapTo(valor.toFloat())
    }
    Text("${a.value.toInt()}$sufijo", style = estilo, color = color)
}

/** Anillo que se expande y se desvanece detrás de lo que está «en marcha». */
@Composable
fun Modifier.pulso(color: Color, activo: Boolean = true, radio: Float = 12f): Modifier {
    if (!activo || !LocalMovimiento.current) return this
    val t = rememberInfiniteTransition(label = "pulso")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    return drawBehind {
        val crece = 6.dp.toPx() * p
        drawRoundRect(
            color = color.copy(alpha = 0.45f * (1f - p)),
            topLeft = androidx.compose.ui.geometry.Offset(-crece, -crece),
            size = androidx.compose.ui.geometry.Size(size.width + crece * 2, size.height + crece * 2),
            cornerRadius = CornerRadius(radio.dp.toPx() + crece),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/** Para que la ficha crezca desde su fila: el título viaja de una a otra. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalCompartido = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimado = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.compartido(clave: String): Modifier {
    val s = LocalCompartido.current ?: return this
    val a = LocalAnimado.current ?: return this
    if (!LocalMovimiento.current) return this
    return with(s) {
        this@compartido.sharedBounds(
            rememberSharedContentState(clave), a,
            boundsTransform = { _, _ -> Muelles.suave() },
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        )
    }
}
