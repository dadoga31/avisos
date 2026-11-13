package io.github.dadoga31.avisos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

enum class Seccion(val ruta: String, val nombre: String, val icono: ImageVector, val iconoActivo: ImageVector) {
    AGENDA("agenda", "Agenda", Icons.Outlined.ViewTimeline, Icons.Rounded.ViewTimeline),
    HECHOS("hechos", "Hechos", Icons.Outlined.Insights, Icons.Rounded.Insights),
    EQUIPO("equipo", "Equipo", Icons.Outlined.Groups, Icons.Rounded.Groups),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Tune, Icons.Rounded.Tune);

    companion object {
        fun de(ruta: String?) = entries.firstOrNull { it.ruta == ruta }
    }
}

private val AnchoIcono = 54.dp
private val AnchoMas = 68.dp

/** Centro de cada sección en el dock (el + va entre Hechos y Equipo). */
private fun centro(s: Seccion): Dp = when (s) {
    Seccion.AGENDA -> AnchoIcono / 2
    Seccion.HECHOS -> AnchoIcono + AnchoIcono / 2
    Seccion.EQUIPO -> AnchoIcono * 2 + AnchoMas + AnchoIcono / 2
    Seccion.AJUSTES -> AnchoIcono * 3 + AnchoMas + AnchoIcono / 2
}

/** Cristal: desenfoca lo que pasa por debajo (Android 12+); antes, fondo translúcido. */
@Composable
fun estiloCristal(): HazeStyle {
    val s = LocalColores.current.superficies
    return HazeStyle(
        backgroundColor = s.pagina, tint = HazeTint(s.flotante), blurRadius = 26.dp, noiseFactor = 0.04f,
        fallbackTint = HazeTint(s.flotante.copy(alpha = 0.96f))
    )
}

/**
 * Dock flotante: solo iconos, el + naranja en el centro y un punto naranja
 * bajo la sección activa que se desliza con muelle de una a otra.
 */
@Composable
fun Dock(actual: Seccion, alIr: (Seccion) -> Unit, alNuevo: () -> Unit, cristal: HazeState?, modifier: Modifier = Modifier, alMantener: () -> Unit = {}) {
    val s = LocalColores.current.superficies
    val forma = RoundedCornerShape(32.dp)
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .shadow(18.dp, forma, ambientColor = Color.Transparent, spotColor = Color.Black.copy(alpha = if (s.oscuro) 0.5f else 0.18f))
                .clip(forma)
                .then(if (cristal != null) Modifier.hazeChild(cristal, estiloCristal()) else Modifier.background(s.flotante.copy(alpha = 0.96f)))
                .border(1.dp, s.borde, forma)
                .height(64.dp)
                .padding(horizontal = 8.dp)
        ) {
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                IconoDock(Seccion.AGENDA, actual, alIr)
                IconoDock(Seccion.HECHOS, actual, alIr)
                BotonMas(alNuevo, alMantener)
                IconoDock(Seccion.EQUIPO, actual, alIr)
                IconoDock(Seccion.AJUSTES, actual, alIr)
            }
            val x by animateDpAsState(centro(actual), Muelles.deslizar(), label = "punto")
            Box(Modifier.offset(x = x - 2.dp, y = 47.dp).size(4.dp).clip(CircleShape).background(RedOrange))
        }
    }
}

@Composable
private fun IconoDock(s: Seccion, actual: Seccion, alIr: (Seccion) -> Unit) {
    val activo = s == actual
    val sup = LocalColores.current.superficies
    val color by animateColorAsState(if (activo) MaterialTheme.colorScheme.onSurface else sup.tenue, Muelles.suave(), label = "color")
    val i = remember { MutableInteractionSource() }
    Box(
        Modifier
            .width(AnchoIcono)
            .fillMaxHeight()
            .presionable(i, 0.88f)
            .clickable(i, null, role = Role.Tab) { alIr(s) }
            .semantics { contentDescription = s.nombre; selected = activo },
        contentAlignment = Alignment.Center
    ) {
        Icon(if (activo) s.iconoActivo else s.icono, null, Modifier.offset(y = (-3).dp).size(24.dp), tint = color)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BotonMas(alPulsar: () -> Unit, alMantener: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val forma = RoundedCornerShape(18.dp)
    val vibrar = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(Modifier.width(AnchoMas).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(52.dp)
                .presionable(i, 0.88f)
                .shadow(14.dp, forma, ambientColor = RedOrange, spotColor = RedOrange)
                .clip(forma)
                .background(RedOrange)
                .combinedClickable(
                    i, LocalIndication.current, role = Role.Button, onClickLabel = "Nuevo aviso", onClick = alPulsar,
                    onLongClickLabel = "Más opciones",
                    onLongClick = { vibrar.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); alMantener() }
                )
                .semantics { contentDescription = "Nuevo aviso" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, Modifier.size(28.dp), tint = Color(0xFF141416))
        }
    }
}

/** Avisos de abajo («Hecho · Deshacer»), por encima del dock. */
@Composable
fun AvisosAbajo(estado: SnackbarHostState, conMenu: Boolean, modifier: Modifier = Modifier) {
    val oscuro = LocalColores.current.superficies.oscuro
    SnackbarHost(estado, modifier.navigationBarsPadding().padding(bottom = if (conMenu) 88.dp else 96.dp)) { datos ->
        Snackbar(
            datos, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = if (oscuro) Color(0xFFD63800) else Color(0xFFFF6A33)
        )
    }
}

/** Pantalla de primer nivel: fondo con rejilla y dock de cristal. Lo usan la app y las capturas. */
@Composable
fun MarcoSeccion(
    actual: Seccion,
    alIr: (Seccion) -> Unit,
    alNuevo: () -> Unit,
    snackbar: SnackbarHostState,
    mostrarMenu: Boolean = true,
    alNuevoCorreo: () -> Unit = {},
    menuMasInicial: Boolean = false,
    contenido: @Composable () -> Unit
) {
    val cristal = remember { HazeState() }
    var menuMas by remember { mutableStateOf(menuMasInicial) }
    Box(Modifier.fillMaxSize().fondoPagina()) {
        Box(Modifier.fillMaxSize().haze(cristal)) { contenido() }
        AvisosAbajo(snackbar, conMenu = mostrarMenu, modifier = Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(
            mostrarMenu, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Muelles.suave()) { it } + fadeIn(), exit = slideOutVertically(Muelles.suave()) { it } + fadeOut()
        ) {
            Dock(actual, alIr, alNuevo, cristal, alMantener = { menuMas = true })
        }
        MenuMas(
            menuMas && mostrarMenu, cristal, { menuMas = false },
            listOf(
                OpcionMas(Icons.Rounded.MailOutline, "Nuevo correo", "A quien quieras, con tu firma") { menuMas = false; alNuevoCorreo() },
                OpcionMas(Icons.Rounded.Add, "Nuevo aviso", "Un trabajo para la agenda") { menuMas = false; alNuevo() }
            )
        )
    }
}

class OpcionMas(val icono: ImageVector, val titulo: String, val detalle: String, val alPulsar: () -> Unit)

/**
 * Lo que sale al mantener pulsado el +: el fondo se desenfoca y oscurece,
 * una onda naranja sale del botón, las opciones suben una tras otra con
 * muelle y el + gira hasta ser una ×. Tocar fuera, la × o Atrás lo cierra.
 */
@Composable
private fun MenuMas(visible: Boolean, cristal: HazeState, alCerrar: () -> Unit, opciones: List<OpcionMas>) {
    val s = LocalColores.current.superficies
    val mov = LocalMovimiento.current
    androidx.activity.compose.BackHandler(visible, onBack = alCerrar)
    AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(180))) {
        Box(
            Modifier.fillMaxSize()
                .hazeChild(
                    cristal,
                    HazeStyle(
                        backgroundColor = s.pagina, tint = HazeTint(Color.Black.copy(alpha = if (s.oscuro) 0.45f else 0.30f)), blurRadius = 18.dp,
                        noiseFactor = 0.03f, fallbackTint = HazeTint(Color.Black.copy(alpha = 0.55f))
                    )
                )
                .clickable(remember { MutableInteractionSource() }, null, onClickLabel = "Cerrar", onClick = alCerrar)
        )
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        /* La onda que sale del + al abrir. */
        val onda = remember { Animatable(1f) }
        LaunchedEffect(visible) {
            if (visible && mov) { onda.snapTo(0f); onda.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        }
        if (visible && onda.value < 1f) {
            Box(
                /* Centrada en el + (a 44 dp del borde de abajo): 160 − 44 = 116. */
                Modifier.offset(y = 116.dp).size(320.dp).drawBehind {
                    val r = (26.dp.toPx() + (size.minDimension / 2 - 26.dp.toPx()) * onda.value)
                    drawCircle(RedOrange.copy(alpha = 0.55f * (1f - onda.value)), radius = r, style = Stroke((3f * (1f - onda.value) + 1f).dp.toPx()))
                    drawCircle(RedOrange.copy(alpha = 0.18f * (1f - onda.value)), radius = r * 0.8f)
                }
            )
        }
        Column(Modifier.padding(bottom = 104.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            opciones.forEachIndexed { n, o ->
                /* La de abajo (la más cerca del +) sale primero. */
                val retraso = (opciones.size - 1 - n) * 70
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(180, delayMillis = retraso)) +
                        scaleIn(spring(dampingRatio = 0.62f, stiffness = 420f), initialScale = 0.55f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                        slideInVertically(spring(dampingRatio = 0.7f, stiffness = 380f)) { it },
                    exit = fadeOut(tween(120)) + scaleOut(tween(150), targetScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f))
                ) { BotonOpcion(o) }
            }
        }
        val giro by animateFloatAsState(if (visible) 45f else 0f, Muelles.soltar(), label = "giro")
        if (visible || giro > 0.5f) {
            val forma = RoundedCornerShape(18.dp)
            Box(
                Modifier.padding(bottom = 18.dp).size(52.dp)
                    .shadow(14.dp, forma, ambientColor = RedOrange, spotColor = RedOrange)
                    .clip(forma).background(RedOrange)
                    .clickable(role = Role.Button, onClickLabel = "Cerrar", onClick = alCerrar)
                    .semantics { contentDescription = "Cerrar opciones" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(28.dp).graphicsLayer { rotationZ = giro }, tint = Color(0xFF141416))
            }
        }
    }
}

@Composable
private fun BotonOpcion(o: OpcionMas) {
    val s = LocalColores.current.superficies
    val i = remember { MutableInteractionSource() }
    val forma = RoundedCornerShape(30.dp)
    Row(
        Modifier.width(284.dp).height(62.dp).presionable(i, 0.95f)
            .shadow(20.dp, forma, ambientColor = Color.Black, spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(forma).background(s.heroe).border(1.dp, s.sobreHeroe.copy(alpha = 0.12f), forma)
            .clickable(i, LocalIndication.current, role = Role.Button, onClick = o.alPulsar)
            .padding(start = 9.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(RedOrange), contentAlignment = Alignment.Center) {
            Icon(o.icono, null, Modifier.size(22.dp), tint = Color(0xFF141416))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(o.titulo, color = s.sobreHeroe, style = MaterialTheme.typography.titleMedium)
            Dato(o.detalle, color = s.sobreHeroeSuave, tam = 9f)
        }
    }
}
