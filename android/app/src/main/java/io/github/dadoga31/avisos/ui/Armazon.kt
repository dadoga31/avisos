package io.github.dadoga31.avisos.ui

import androidx.compose.animation.AnimatedVisibility
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
fun Dock(actual: Seccion, alIr: (Seccion) -> Unit, alNuevo: () -> Unit, cristal: HazeState?, modifier: Modifier = Modifier) {
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
                BotonMas(alNuevo)
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

@Composable
private fun BotonMas(alPulsar: () -> Unit) {
    val i = remember { MutableInteractionSource() }
    val forma = RoundedCornerShape(18.dp)
    Box(Modifier.width(AnchoMas).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(52.dp)
                .presionable(i, 0.88f)
                .shadow(14.dp, forma, ambientColor = RedOrange, spotColor = RedOrange)
                .clip(forma)
                .background(RedOrange)
                .clickable(i, LocalIndication.current, role = Role.Button, onClickLabel = "Nuevo aviso", onClick = alPulsar)
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
    contenido: @Composable () -> Unit
) {
    val cristal = remember { HazeState() }
    Box(Modifier.fillMaxSize().fondoPagina()) {
        Box(Modifier.fillMaxSize().haze(cristal)) { contenido() }
        AvisosAbajo(snackbar, conMenu = mostrarMenu, modifier = Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(
            mostrarMenu, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Muelles.suave()) { it } + fadeIn(), exit = slideOutVertically(Muelles.suave()) { it } + fadeOut()
        ) {
            Dock(actual, alIr, alNuevo, cristal)
        }
    }
}
