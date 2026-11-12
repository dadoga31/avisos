package io.github.dadoga31.avisos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

/**
 * Menú flotante: cápsula con las cuatro secciones y, a su lado, el botón de
 * la pantalla. Sin botón, la cápsula va centrada. El contenido pasa por debajo.
 */
@Composable
fun MenuFlotante(
    actual: Seccion,
    alIr: (Seccion) -> Unit,
    avisoAgenda: Int,
    boton: BotonFlotante?,
    modifier: Modifier = Modifier
) {
    val sup = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = if (boton != null) Arrangement.SpaceBetween else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape, color = sup.flotante, shadowElevation = 8.dp, tonalElevation = 0.dp
        ) {
            Row(Modifier.height(68.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Seccion.entries.forEach { s ->
                    ElementoMenu(s, s == actual, if (s == Seccion.AGENDA) avisoAgenda else 0) { alIr(s) }
                }
            }
        }
        if (boton != null) {
            Surface(
                onClick = boton.alPulsar, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 8.dp, modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(boton.icono, boton.descripcion, Modifier.size(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ElementoMenu(s: Seccion, activo: Boolean, numero: Int, alPulsar: () -> Unit) {
    Column(
        Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(30.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                role = Role.Tab, onClick = alPulsar
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .then(if (activo) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(badge = { if (numero > 0) Badge { Text("$numero") } }) {
                Icon(
                    if (activo) s.iconoActivo else s.icono, null,
                    tint = if (activo) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            s.nombre, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelMedium,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
            color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Avisos de abajo («Hecho · Deshacer»), por encima del menú flotante. */
@Composable
fun AvisosAbajo(estado: SnackbarHostState, conMenu: Boolean, modifier: Modifier = Modifier) {
    SnackbarHost(
        estado,
        modifier.navigationBarsPadding().padding(bottom = if (conMenu) 92.dp else 96.dp)
    ) { datos ->
        Snackbar(datos, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(horizontal = 16.dp))
    }
}

/** Pantalla de primer nivel con su menú flotante. Lo usan la app y las capturas. */
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
    Box(Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina)) {
        contenido()
        AvisosAbajo(snackbar, conMenu = mostrarMenu, modifier = Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(
            mostrarMenu, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()
        ) {
            MenuFlotante(actual, alIr, avisoAgenda, boton)
        }
    }
}
