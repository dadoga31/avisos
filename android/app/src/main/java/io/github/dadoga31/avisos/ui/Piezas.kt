package io.github.dadoga31.avisos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

/*
 * Piezas del estilo «tech minimal» (3.5.0): fondo con rejilla de puntos,
 * marca con el cuadrado naranja, botones redondos con línea, fila de cifras,
 * anillo de progreso, punto vivo, interruptor, segmentado y relojes que
 * corren con los dígitos rodando.
 */

/** Fondo de página con la rejilla de puntos. La rejilla no se mueve con el scroll. */
@Composable
fun Modifier.fondoPagina(): Modifier {
    val s = LocalColores.current.superficies
    return this.background(s.pagina).rejilla(s.rejilla)
}

fun Modifier.rejilla(color: Color, paso: Dp = 18.dp): Modifier = drawWithCache {
    val p = paso.toPx()
    val puntos = ArrayList<Offset>()
    var y = p / 2
    while (y < size.height) {
        var x = p / 2
        while (x < size.width) { puntos += Offset(x, y); x += p }
        y += p
    }
    val grosor = 1.7.dp.toPx()
    onDrawBehind { drawPoints(puntos, PointMode.Points, color, strokeWidth = grosor, cap = StrokeCap.Round) }
}

/** «■ AVISOS / HOY». */
@Composable
fun Marca(titulo: String, extra: String = "", modifier: Modifier = Modifier) {
    val estilo = EstiloDato.copy(fontSize = 11.5.sp, letterSpacing = 1.8.sp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(RedOrange))
        Text(titulo.uppercase(), Modifier.padding(start = 9.dp), style = estilo, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        if (extra.isNotEmpty()) {
            Text("  /  " + extra.uppercase(), style = estilo, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Barra de arriba de una sección: la marca a la izquierda y sus botones a la derecha. */
@Composable
fun BarraMarca(titulo: String, extra: String = "", acciones: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.statusBarsPadding().fillMaxWidth().height(60.dp).padding(start = 22.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Marca(titulo, extra, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = acciones)
    }
}

/** Botón redondo con línea fina: atrás, buscar, editar, llamar… */
@Composable
fun BotonRedondo(
    icono: ImageVector,
    descripcion: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    tam: Dp = 40.dp,
    tamIcono: Dp = 21.dp,
    habilitado: Boolean = true,
    relleno: Color? = null,
    tinta: Color? = null
) {
    val s = LocalColores.current.superficies
    val i = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(tam)
            .presionable(i, 0.9f)
            .clip(CircleShape)
            .background(relleno ?: s.item)
            .then(if (relleno == null) Modifier.border(1.dp, s.borde, CircleShape) else Modifier)
            .clickable(i, LocalIndication.current, enabled = habilitado, role = Role.Button, onClickLabel = descripcion, onClick = alPulsar),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icono, descripcion, Modifier.size(tamIcono),
            tint = when {
                !habilitado -> s.tenue
                tinta != null -> tinta
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Etiqueta en mayúsculas monoespaciada. */
@Composable
fun Dato(texto: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, tam: Float = 10.5f, peso: FontWeight = FontWeight.Medium) {
    Text(
        texto.uppercase(), modifier, maxLines = 1,
        style = EstiloDato.copy(fontSize = tam.sp, fontWeight = peso),
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color
    )
}

/** Cabecera de bloque: «REGISTRO ······ 2». */
@Composable
fun CabeceraDato(titulo: String, modifier: Modifier = Modifier, extra: String = "", alerta: Boolean = false) {
    val s = LocalColores.current.superficies
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (alerta) Box(Modifier.padding(end = 8.dp).size(6.dp).clip(CircleShape).background(RedOrange))
        Dato(titulo, Modifier.weight(1f), color = if (alerta) s.acentoTexto else Color.Unspecified)
        if (extra.isNotEmpty()) Dato(extra)
    }
}

data class Kpi(val valor: String, val nombre: String, val alerta: Boolean = false, val activo: Boolean = false, val alPulsar: (() -> Unit)? = null)

/** Cifras en fila con separadores finos, como un panel de instrumentos. */
@Composable
fun FilaKpi(kpis: List<Kpi>, modifier: Modifier = Modifier) {
    val s = LocalColores.current.superficies
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .drawBehind {
                val g = 1.dp.toPx()
                drawRect(s.borde, Offset.Zero, Size(size.width, g))
                drawRect(s.borde, Offset(0f, size.height - g), Size(size.width, g))
            }
    ) {
        kpis.forEachIndexed { n, k ->
            if (n > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(s.borde))
            val fondo by animateColorAsState(if (k.activo) s.tinte else Color.Transparent, Muelles.suave(), label = "kpi")
            Column(
                Modifier
                    .weight(1f)
                    .background(fondo)
                    .then(if (k.alPulsar != null) Modifier.clickable(role = Role.Button, onClick = k.alPulsar) else Modifier)
                    .padding(start = if (n == 0) 6.dp else 12.dp, top = 11.dp, bottom = 11.dp, end = 4.dp)
            ) {
                val color = if (k.alerta) s.acentoTexto else MaterialTheme.colorScheme.onSurface
                val estilo = TextStyle(fontFamily = SpaceGrotesk, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
                val entero = k.valor.toIntOrNull()
                if (entero != null) Cifra(entero, estilo, color, relleno = k.valor.length) else Text(k.valor, style = estilo, color = color)
                Dato(k.nombre, Modifier.padding(top = 4.dp), tam = 9f, color = if (k.activo) s.acentoTexto else Color.Unspecified)
            }
        }
    }
}

/** Anillo de progreso: el arco se llena al entrar. */
@Composable
fun Anillo(hechos: Int, total: Int, modifier: Modifier = Modifier, tam: Dp = 88.dp, nombre: String = "hechos") {
    val s = LocalColores.current.superficies
    val objetivo = if (total <= 0) 0f else hechos.toFloat() / total
    val mov = LocalMovimiento.current
    val p = remember { Animatable(if (mov) 0f else objetivo) }
    LaunchedEffect(objetivo) {
        if (mov) p.animateTo(objetivo, tween(1100, delayMillis = 150, easing = FastOutSlowInEasing)) else p.snapTo(objetivo)
    }
    Box(
        modifier.size(tam).drawBehind {
            val g = 4.dp.toPx()
            val r = Size(size.width - g, size.height - g)
            drawArc(s.borde, 0f, 360f, false, Offset(g / 2, g / 2), r, style = Stroke(g))
            if (p.value > 0f) drawArc(RedOrange, -90f, 360f * p.value, false, Offset(g / 2, g / 2), r, style = Stroke(g, cap = StrokeCap.Round))
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$hechos/$total",
                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp)
            )
            Dato(nombre, Modifier.padding(top = 2.dp), tam = 8.5f)
        }
    }
}

/** Punto de estado con su halo; si está vivo, el halo respira. */
@Composable
fun PuntoVivo(color: Color, modifier: Modifier = Modifier, vivo: Boolean = false, tam: Dp = 7.dp) {
    val mov = LocalMovimiento.current && vivo
    val f = if (mov) {
        val t = rememberInfiniteTransition(label = "punto")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "f").value
    } else 0f
    Box(
        modifier.size(tam).drawBehind {
            val r = size.minDimension / 2
            drawCircle(color.copy(alpha = 0.22f), radius = r + 3.5.dp.toPx())
            if (mov) drawCircle(color.copy(alpha = 0.35f * (1f - f)), radius = r + (3.5f + 6f * f).dp.toPx())
            drawCircle(color, radius = r)
        }
    )
}

/** Interruptor fino: pista naranja y bola oscura cuando está encendido. */
@Composable
fun Interruptor(activo: Boolean, alCambiar: (Boolean) -> Unit, habilitado: Boolean = true, descripcion: String = "") {
    val s = LocalColores.current.superficies
    val x by animateDpAsState(if (activo) 21.dp else 3.dp, Muelles.deslizar(), label = "bola")
    val pista by animateColorAsState(if (activo) RedOrange else if (s.oscuro) s.borde else Color(0xFFE3E3E6), Muelles.suave(), label = "pista")
    val bola by animateColorAsState(if (activo) Color(0xFF141416) else if (s.oscuro) s.tenue else Color.White, Muelles.suave(), label = "bolaColor")
    Box(
        Modifier
            .size(46.dp, 28.dp)
            .alpha(if (habilitado) 1f else 0.4f)
            .clip(CircleShape)
            .background(pista)
            .toggleable(activo, enabled = habilitado, role = Role.Switch, onValueChange = alCambiar)
            .then(if (descripcion.isNotEmpty()) Modifier.semantics { contentDescription = descripcion } else Modifier)
    ) {
        Box(Modifier.offset(x = x, y = 3.dp).size(22.dp).clip(CircleShape).background(bola))
    }
}

/** Segmentado monoespaciado; la pieza elegida se desliza con muelle. */
@Composable
fun Segmentado(opciones: List<String>, elegida: Int, alElegir: (Int) -> Unit, modifier: Modifier = Modifier, ancho: Dp = 60.dp) {
    val s = LocalColores.current.superficies
    val forma = RoundedCornerShape(14.dp)
    val x by animateDpAsState(ancho * elegida, Muelles.deslizar(), label = "segmento")
    Box(modifier.height(36.dp).clip(forma).background(s.item).border(1.dp, s.borde, forma)) {
        Box(Modifier.offset(x = x).width(ancho).fillMaxHeight().padding(3.dp).clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.secondaryContainer))
        Row(Modifier.fillMaxHeight()) {
            opciones.forEachIndexed { n, t ->
                val sel = n == elegida
                val color by animateColorAsState(
                    if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, Muelles.suave(), label = "c"
                )
                Box(
                    Modifier.width(ancho).fillMaxHeight().clickable(role = Role.Tab) { alElegir(n) },
                    contentAlignment = Alignment.Center
                ) { Dato(t, color = color, tam = 10f) }
            }
        }
    }
}

/** Ficha de elección: línea fina, y rellena en Matte Black (Off-White en oscuro) al elegirla. */
@Composable
fun Pastilla(
    texto: String,
    elegida: Boolean,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    mono: Boolean = false,
    alPulsar: () -> Unit
) {
    val s = LocalColores.current.superficies
    val fondo by animateColorAsState(if (elegida) MaterialTheme.colorScheme.secondaryContainer else s.item, Muelles.suave(), label = "f")
    val tinta by animateColorAsState(
        if (elegida) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, Muelles.suave(), label = "t"
    )
    val i = remember { MutableInteractionSource() }
    Row(
        modifier
            .height(38.dp)
            .presionable(i, 0.94f)
            .clip(CircleShape)
            .background(fondo)
            .border(1.dp, if (elegida) fondo else s.borde, CircleShape)
            .clickable(i, LocalIndication.current, role = Role.RadioButton, onClick = alPulsar)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icono != null) Icon(icono, null, Modifier.size(16.dp), tint = tinta)
        Text(
            texto, color = tinta, maxLines = 1,
            style = if (mono) EstiloMono.copy(fontSize = 13.sp) else MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
        )
    }
}

// ---------------------------------------------------------------- relojes

/** false en las capturas: los relojes se quedan en la hora que se les da. */
val LocalRelojVivo = staticCompositionLocalOf { true }

/** La hora al segundo (en la app) o la que llega (en las capturas). */
@Composable
fun ahoraVivo(base: LocalDateTime): LocalDateTime {
    if (!LocalRelojVivo.current) return base
    val t by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - value.nano / 1_000_000L)
        }
    }
    return t
}

/** «00:41:27» a partir de una duración (no negativa). */
fun hhmmss(d: Duration): String {
    val s = maxOf(0L, d.seconds)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/**
 * Texto en trozos de colores cuyos dígitos ruedan al cambiar (el segundero de
 * un cronómetro). Sin movimiento, texto normal.
 */
@Composable
fun TextoRodante(partes: List<Pair<String, Color>>, estilo: TextStyle, modifier: Modifier = Modifier) {
    if (!LocalMovimiento.current) {
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                partes.forEach { (t, c) -> pushStyle(androidx.compose.ui.text.SpanStyle(color = c)); append(t); pop() }
            },
            modifier, style = estilo, maxLines = 1
        )
        return
    }
    Row(modifier) {
        partes.forEachIndexed { np, (t, c) ->
            t.forEachIndexed { n, ch ->
                if (!ch.isDigit()) Text(ch.toString(), style = estilo, color = c)
                else AnimatedContent(
                    ch, label = "d$np-$n",
                    transitionSpec = {
                        (slideInVertically(tween(260)) { -it / 2 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(160)))
                    }
                ) { d -> Text(d.toString(), style = estilo, color = c) }
            }
        }
    }
}

/** Tiempo desde [desde] que corre al segundo: «00:41» y los segundos en otro color. */
@Composable
fun Cronometro(desde: java.time.Instant?, base: LocalDateTime, estilo: TextStyle, color: Color, colorSegundos: Color, modifier: Modifier = Modifier) {
    val t = ahoraVivo(base)
    val d = if (desde == null) Duration.ZERO else Duration.between(desde, t.atZone(java.time.ZoneId.systemDefault()).toInstant())
    val texto = hhmmss(d)
    TextoRodante(listOf(texto.dropLast(3) to color, texto.takeLast(3) to colorSegundos), estilo, modifier)
}

/** Separación vertical. */
@Composable
fun Alto(dp: Int) = Spacer(Modifier.height(dp.dp))
