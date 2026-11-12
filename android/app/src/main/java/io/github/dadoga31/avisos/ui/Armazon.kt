package io.github.dadoga31.avisos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

enum class Seccion(val ruta: String, val nombre: String, val icono: ImageVector, val iconoActivo: ImageVector) {
    AGENDA("agenda", "Agenda", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    HECHOS("hechos", "Hechos", Icons.Outlined.TaskAlt, Icons.Rounded.TaskAlt),
    EQUIPO("equipo", "Equipo", Icons.Outlined.Group, Icons.Rounded.Group),
    AJUSTES("ajustes", "Ajustes", Icons.Outlined.Settings, Icons.Rounded.Settings);

    companion object {
        fun de(ruta: String?) = entries.firstOrNull { it.ruta == ruta }
    }
}

/** Botón de la pantalla, al lado del menú (el + en la Agenda). */
data class BotonFlotante(val icono: ImageVector, val descripcion: String, val alPulsar: () -> Unit)

private val AnchoElemento = 72.dp

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
 * Menú flotante: cápsula de cristal con las cuatro secciones; la pieza de la
 * activa se desliza con muelle. A su lado, el botón de la pantalla.
 */
@Composable
fun MenuFlotante(
    actual: Seccion,
    alIr: (Seccion) -> Unit,
    avisoAgenda: Int,
    boton: BotonFlotante?,
    cristal: HazeState?,
    modifier: Modifier = Modifier
) {
    val s = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = if (boton != null) Arrangement.SpaceBetween else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val forma = CircleShape
        Box(
            Modifier
                .clip(forma)
                .then(if (cristal != null) Modifier.hazeChild(cristal, estiloCristal()) else Modifier.background(s.flotante.copy(alpha = 0.96f)))
                .border(1.dp, s.borde, forma)
                .height(64.dp)
                .padding(horizontal = 6.dp)
        ) {
            val x by animateDpAsState(AnchoElemento * actual.ordinal, Muelles.deslizar(), label = "indicador")
            Box(
                Modifier
                    .offset(x = x + 4.dp)
                    .padding(vertical = 6.dp)
                    .width(AnchoElemento - 8.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                Seccion.entries.forEach { sec ->
                    ElementoMenu(sec, sec == actual, if (sec == Seccion.AGENDA) avisoAgenda else 0) { alIr(sec) }
                }
            }
        }
        if (boton != null) {
            val i = remember { MutableInteractionSource() }
            Surface(
                onClick = boton.alPulsar, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary, interactionSource = i,
                modifier = Modifier.size(64.dp).presionable(i, 0.92f)
            ) {
                AnimatedContent(
                    boton.icono,
                    transitionSpec = { (scaleIn(Muelles.soltar(), initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut()) },
                    contentAlignment = Alignment.Center, label = "boton"
                ) { icono ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icono, boton.descripcion, Modifier.size(30.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ElementoMenu(s: Seccion, activo: Boolean, numero: Int, alPulsar: () -> Unit) {
    val color by animateColorAsState(
        if (activo) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        Muelles.suave(), label = "color"
    )
    Column(
        Modifier
            .width(AnchoElemento)
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = alPulsar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(if (activo) s.iconoActivo else s.icono, null, tint = color, modifier = Modifier.size(22.dp))
            if (numero > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp).size(16.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$numero", color = MaterialTheme.colorScheme.onPrimary, style = EstiloEtiqueta.copy(fontSize = 9.sp, letterSpacing = 0.sp))
                }
            }
        }
        Text(
            s.nombre, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelMedium, color = color
        )
    }
}

/** Avisos de abajo («Hecho · Deshacer»), por encima del menú flotante. */
@Composable
fun AvisosAbajo(estado: SnackbarHostState, conMenu: Boolean, modifier: Modifier = Modifier) {
    val oscuro = LocalColores.current.superficies.oscuro
    SnackbarHost(estado, modifier.navigationBarsPadding().padding(bottom = if (conMenu) 92.dp else 96.dp)) { datos ->
        Snackbar(
            datos, shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = if (oscuro) Color(0xFFD63800) else Color(0xFFFF6A33)
        )
    }
}

/** Pantalla de primer nivel con su menú flotante de cristal. Lo usan la app y las capturas. */
@Composable
fun MarcoSeccion(
    actual: Seccion,
    alIr: (Seccion) -> Unit,
    avisoAgenda: Int,
    boton: BotonFlotante?,
    snackbar: SnackbarHostState,
    mostrarMenu: Boolean = true,
    contenido: @Composable () -> Unit
) {
    val cristal = remember { HazeState() }
    Box(Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina)) {
        Box(Modifier.fillMaxSize().haze(cristal)) { contenido() }
        AvisosAbajo(snackbar, conMenu = mostrarMenu, modifier = Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(
            mostrarMenu, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(Muelles.suave()) { it } + fadeIn(), exit = slideOutVertically(Muelles.suave()) { it } + fadeOut()
        ) {
            MenuFlotante(actual, alIr, avisoAgenda, boton, cristal)
        }
    }
}
