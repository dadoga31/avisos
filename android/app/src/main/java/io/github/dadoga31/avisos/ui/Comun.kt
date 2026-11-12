package io.github.dadoga31.avisos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val g = 18.dp
    val p = 6.dp
    return when {
        total == 1 -> RoundedCornerShape(g)
        indice == 0 -> RoundedCornerShape(topStart = g, topEnd = g, bottomStart = p, bottomEnd = p)
        indice == total - 1 -> RoundedCornerShape(topStart = p, topEnd = p, bottomStart = g, bottomEnd = g)
        else -> RoundedCornerShape(p)
    }
}

/** Tarjeta de la app: fondo de elemento y línea fina (aquí no hay sombras). */
@Composable
fun Modifier.tarjeta(forma: Shape): Modifier {
    val s = LocalColores.current.superficies
    return this.clip(forma).background(s.item).border(1.dp, s.borde, forma)
}

/** Lista agrupada a mano: para pocos elementos dentro de una columna con scroll. */
@Composable
fun Grupo(elementos: List<@Composable (Shape) -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        elementos.forEachIndexed { i, e -> Box(Modifier.entrada(i)) { e(formaEnGrupo(i, elementos.size)) } }
    }
}

/** Fila de lista con su forma en el grupo; encoge al pulsar. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Elemento(
    forma: Shape,
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    alMantener: (() -> Unit)? = null,
    contenido: @Composable RowScope.() -> Unit
) {
    val i = remember { MutableInteractionSource() }
    val toca = alPulsar != null || alMantener != null
    Row(
        modifier
            .fillMaxWidth()
            .then(if (toca) Modifier.presionable(i) else Modifier)
            .tarjeta(forma)
            .then(
                when {
                    alMantener != null -> Modifier.combinedClickable(i, LocalIndication.current, onClick = alPulsar ?: {}, onLongClick = alMantener)
                    alPulsar != null -> Modifier.clickable(i, LocalIndication.current, onClick = alPulsar)
                    else -> Modifier
                }
            )
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
    alMantener: (() -> Unit)? = null,
    final: (@Composable () -> Unit)? = null
) {
    Elemento(forma, alPulsar = alPulsar, alMantener = alMantener) {
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

/** Cabecera de sección técnica: mayúsculas monoespaciadas y la cuenta a la derecha. */
@Composable
fun CabeceraSeccion(titulo: String, modifier: Modifier = Modifier, alerta: Boolean = false, extra: String = "") {
    val s = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alerta) {
            Box(Modifier.padding(end = 8.dp).size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Text(
            titulo.uppercase(), Modifier.weight(1f), style = EstiloEtiqueta,
            color = if (alerta) s.acentoTexto else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (extra.isNotEmpty()) {
            Text(extra.uppercase(), style = EstiloEtiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TituloGrande(titulo: String, subtitulo: String = "", modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)) {
        Text(titulo, style = MaterialTheme.typography.headlineLarge)
        if (subtitulo.isNotEmpty()) {
            Text(subtitulo, Modifier.padding(top = 2.dp), style = EstiloMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Recuadro del estado: fondo tenue, línea y el icono en su color. Lo que
 * está en curso va relleno de naranja y respira.
 */
@Composable
fun CirculoEstado(e: Estado, tam: Int = 40) {
    val c = LocalColores.current.de(e)
    val fondo by animateColorAsState(c.fondo, Muelles.suave(), label = "fondo")
    val forma = RoundedCornerShape((tam * 0.3f).dp)
    Box(
        Modifier
            .size(tam.dp)
            .pulso(c.color, activo = e == Estado.EN_CURSO, radio = tam * 0.3f)
            .clip(forma)
            .background(fondo)
            .border(1.dp, c.color.copy(alpha = if (e == Estado.EN_CURSO) 0f else 0.28f), forma),
        contentAlignment = Alignment.Center
    ) {
        Icon(iconoEstado(e), e.nombre, tint = c.sobreFondo, modifier = Modifier.size((tam * 0.5f).dp))
    }
}

@Composable
fun Avatar(tecnico: Tecnico?, tam: Int = 28) {
    val oscuro = LocalColores.current.superficies.oscuro
    if (tecnico == null) {
        Box(
            Modifier.size(tam.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, "Sin asignar", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size((tam * 0.6f).dp))
        }
        return
    }
    val (fondo, letra) = colorTecnico(tecnico.color, oscuro)
    Box(Modifier.size(tam.dp).clip(CircleShape).background(fondo), contentAlignment = Alignment.Center) {
        Text(
            tecnico.iniciales, color = letra, fontFamily = Mono, fontWeight = FontWeight.SemiBold,
            fontSize = (tam * 0.36f).sp, maxLines = 1
        )
    }
}

/** URGENTE en naranja relleno; ALTA con línea. El resto no lleva marca. */
@Composable
fun EtiquetaPrioridad(p: Prioridad) {
    val forma = RoundedCornerShape(5.dp)
    when (p) {
        Prioridad.URGENTE -> Text(
            "URGENTE", Modifier.clip(forma).background(RedOrange).padding(horizontal = 6.dp, vertical = 2.dp),
            style = EstiloDato.copy(fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFF141416)
        )
        Prioridad.ALTA -> Text(
            "ALTA", Modifier.border(1.dp, LocalColores.current.de(Estado.EN_ESPERA).color, forma).padding(horizontal = 6.dp, vertical = 2.dp),
            style = EstiloDato.copy(fontSize = 9.5.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold), color = LocalColores.current.de(Estado.EN_ESPERA).texto
        )
        else -> Unit
    }
}

/** Color de la barra lateral de un aviso: naranja lo que está en marcha o vencido. */
@Composable
fun colorBarra(a: Aviso, vencido: Boolean): Color {
    val c = LocalColores.current
    return when {
        a.estadoE == Estado.EN_CURSO || vencido -> RedOrange
        a.estadoE == Estado.PROGRAMADO -> MaterialTheme.colorScheme.onSurface
        a.estadoE == Estado.EN_ESPERA -> c.de(Estado.EN_ESPERA).color
        a.estadoE == Estado.RESUELTO -> c.superficies.ok
        else -> c.superficies.tenue
    }
}

/**
 * Fila de aviso: barra de color a la izquierda; estado, prioridad y título;
 * cliente y referencia en mono. A la derecha, cuándo y el técnico. El título
 * es el que viaja hasta la ficha al abrirla.
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
    val c = LocalColores.current
    val i = remember { MutableInteractionSource() }
    val cerrado = !aviso.abierto
    Row(
        modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .presionable(i)
            .tarjeta(forma)
            .clickable(i, LocalIndication.current, onClick = alPulsar)
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(colorBarra(aviso, alerta)))
        Column(Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (e == Estado.EN_CURSO) PuntoVivo(RedOrange, vivo = true, tam = 6.dp)
                Text(
                    (textoEstado ?: if (alerta) "Vencido" else e.nombre).uppercase(), style = EstiloDato,
                    color = if (alerta) c.superficies.acentoTexto else c.de(e).texto, maxLines = 1
                )
                EtiquetaPrioridad(aviso.prioridadE)
            }
            Text(
                aviso.titulo.ifBlank { "(sin título)" }, Modifier.padding(top = 5.dp).compartido("titulo-${aviso.id}"),
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textDecoration = if (e == Estado.CANCELADO) TextDecoration.LineThrough else null,
                color = if (cerrado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                listOf(aviso.cliente.nombre, aviso.ref).filter { it.isNotBlank() }.joinToString(" · ").uppercase(),
                Modifier.padding(top = 4.dp), style = EstiloDato.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Column(Modifier.padding(top = 12.dp, bottom = 12.dp, end = 14.dp), horizontalAlignment = Alignment.End) {
            Text(
                cuando, style = EstiloMono.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                color = if (alerta) c.superficies.acentoTexto else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Avatar(tecnico, 26)
        }
    }
}

/** Contenedor de pantalla de primer nivel: fondo de página. */
@Composable
fun Pagina(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(LocalColores.current.superficies.pagina), content = contenido)
}

val RellenoLista = PaddingValues(bottom = EspacioMenu)

@Composable
fun Hueco(alto: Int) = Spacer(Modifier.height(alto.dp).width(1.dp))
