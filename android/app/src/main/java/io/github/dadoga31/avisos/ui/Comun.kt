package io.github.dadoga31.avisos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HomeRepairService
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Prioridad
import io.github.dadoga31.avisos.datos.Sistema
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tipo

/** Espacio para que lo último de una lista no quede bajo el menú flotante. */
val EspacioMenu = 132.dp

fun iconoEstado(e: Estado): ImageVector = when (e) {
    Estado.PENDIENTE -> Icons.Rounded.RadioButtonUnchecked
    Estado.PROGRAMADO -> Icons.Rounded.Event
    Estado.EN_CURSO -> Icons.Rounded.Construction
    Estado.EN_ESPERA -> Icons.Rounded.PauseCircle
    Estado.RESUELTO -> Icons.Rounded.CheckCircle
    Estado.CANCELADO -> Icons.Rounded.Cancel
}

fun iconoTipo(t: Tipo): ImageVector = when (t) {
    Tipo.AVERIA -> Icons.Rounded.Build
    Tipo.INSTALACION -> Icons.Rounded.HomeRepairService
    Tipo.MANTENIMIENTO -> Icons.Rounded.Construction
    Tipo.REVISION -> Icons.Rounded.Visibility
    Tipo.PRESUPUESTO -> Icons.Rounded.Description
}

fun iconoSistema(s: Sistema): ImageVector = when (s) {
    Sistema.ALARMA -> Icons.Rounded.Security
    Sistema.CCTV -> Icons.Rounded.Videocam
    Sistema.ACCESOS -> Icons.Rounded.Key
    Sistema.INCENDIOS -> Icons.Rounded.LocalFireDepartment
    Sistema.INTERFONO -> Icons.Rounded.MeetingRoom
    Sistema.OTROS -> Icons.Rounded.Category
}

/** Forma de un elemento de lista agrupada: esquinas grandes por fuera, pequeñas por dentro. */
fun formaEnGrupo(indice: Int, total: Int): Shape {
    val g = 20.dp
    val p = 4.dp
    return when {
        total == 1 -> RoundedCornerShape(g)
        indice == 0 -> RoundedCornerShape(topStart = g, topEnd = g, bottomStart = p, bottomEnd = p)
        indice == total - 1 -> RoundedCornerShape(topStart = p, topEnd = p, bottomStart = g, bottomEnd = g)
        else -> RoundedCornerShape(p)
    }
}

/** Lista agrupada a mano: para pocos elementos dentro de una columna con scroll. */
@Composable
fun Grupo(elementos: List<@Composable (Shape) -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        elementos.forEachIndexed { i, e -> e(formaEnGrupo(i, elementos.size)) }
    }
}

/** Fila de lista con fondo de elemento y su forma en el grupo. */
@Composable
fun Elemento(
    forma: Shape,
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    contenido: @Composable RowScope.() -> Unit
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(LocalColores.current.superficies.item)
            .then(if (alPulsar != null) Modifier.clickable(onClick = alPulsar) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = contenido
    )
}

/** Fila estándar: icono, título, subtítulo y algo al final. */
@Composable
fun FilaSimple(
    forma: Shape,
    icono: ImageVector?,
    titulo: String,
    subtitulo: String? = null,
    alPulsar: (() -> Unit)? = null,
    colorSubtitulo: Color = Color.Unspecified,
    final: (@Composable () -> Unit)? = null
) {
    Elemento(forma, alPulsar = alPulsar) {
        if (icono != null) Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (!subtitulo.isNullOrEmpty()) {
                Text(
                    subtitulo, style = MaterialTheme.typography.bodyMedium,
                    color = if (colorSubtitulo == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else colorSubtitulo
                )
            }
        }
        final?.invoke()
    }
}

@Composable
fun CabeceraSeccion(titulo: String, modifier: Modifier = Modifier, alerta: Boolean = false, extra: String = "") {
    Row(
        modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
            color = if (alerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (extra.isNotEmpty()) {
            Text(extra, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TituloGrande(titulo: String, subtitulo: String = "", modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium))
        if (subtitulo.isNotEmpty()) {
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CirculoEstado(e: Estado, tam: Int = 40) {
    val c = LocalColores.current.de(e)
    Box(Modifier.size(tam.dp).clip(CircleShape).background(c.fondo), contentAlignment = Alignment.Center) {
        Icon(iconoEstado(e), e.nombre, tint = c.sobreFondo, modifier = Modifier.size((tam * 0.55f).dp))
    }
}

@Composable
fun Avatar(tecnico: Tecnico?, tam: Int = 28) {
    val oscuro = LocalColores.current.superficies.oscuro
    if (tecnico == null) {
        Box(
            Modifier.size(tam.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, "Sin asignar", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size((tam * 0.6f).dp))
        }
        return
    }
    val (fondo, letra) = colorTecnico(tecnico.color, oscuro)
    Box(Modifier.size(tam.dp).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
        Text(
            tecnico.iniciales, color = letra, fontWeight = FontWeight.Medium,
            fontSize = (tam * 0.38f).sp, maxLines = 1
        )
    }
}

@Composable
fun EtiquetaPrioridad(p: Prioridad) {
    val (fondo, letra) = when (p) {
        Prioridad.URGENTE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Prioridad.ALTA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> return
    }
    Surface(color = fondo, shape = RoundedCornerShape(6.dp)) {
        Text(
            p.nombre, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = letra,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Fila de aviso en tres líneas: título y hora; cliente, tipo y sistema;
 * estado, referencia y prioridad. A la derecha, el técnico.
 */
@Composable
fun FilaAviso(
    aviso: Aviso,
    tecnico: Tecnico?,
    cuando: String,
    forma: Shape,
    modifier: Modifier = Modifier,
    alerta: Boolean = false,
    textoEstado: String? = null,
    alPulsar: () -> Unit
) {
    val e = aviso.estadoE
    val c = LocalColores.current.de(e)
    Row(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(LocalColores.current.superficies.item)
            .clickable(onClick = alPulsar)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CirculoEstado(e)
        Column(Modifier.weight(1f)) {
            Text(
                aviso.titulo.ifBlank { "(sin título)" }, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val linea2 = listOf(aviso.cliente.nombre, aviso.tipoE.nombre, aviso.sistemaE.nombre).filter { it.isNotBlank() }
            Text(
                linea2.joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(textoEstado ?: e.nombre, color = c.color, style = MaterialTheme.typography.labelMedium)
                if (aviso.ref.isNotEmpty()) {
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(aviso.ref, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                EtiquetaPrioridad(aviso.prioridadE)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                cuando, style = MaterialTheme.typography.labelMedium,
                color = if (alerta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            Avatar(tecnico)
        }
    }
}

/** Contenedor de pantalla de primer nivel: fondo de página y relleno arriba. */
@Composable
fun Pagina(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(LocalColores.current.superficies.pagina), content = contenido)
}

val RellenoLista = PaddingValues(bottom = EspacioMenu)

@Composable
fun Hueco(alto: Int) = Spacer(Modifier.height(alto.dp).width(1.dp))
