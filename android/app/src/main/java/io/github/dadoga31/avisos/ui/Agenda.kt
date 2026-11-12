package io.github.dadoga31.avisos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Lo que la Agenda puede pedir hacer. */
class AccionesAgenda(
    val abrir: (String) -> Unit = {},
    val nuevo: () -> Unit = {},
    val cambiarEstado: (Aviso, Estado) -> Unit = { _, _ -> },
    val llamar: (String) -> Unit = {},
    val irA: (String) -> Unit = {},
    val ejemplo: () -> Unit = {},
    val ocultarPista: () -> Unit = {}
)

private val ES = Locale("es", "ES")

@Composable
fun AgendaPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ajustes: Ajustes,
    ahora: LocalDateTime,
    acciones: AccionesAgenda,
    filtroInicial: Reglas.Filtro = Reglas.Filtro()
) {
    var filtro by remember { mutableStateOf(filtroInicial) }
    val hoy = ahora.toLocalDate()
    val porId = remember(tecnicos) { tecnicos.associateBy { it.id } }
    val resumen = remember(avisos, hoy) { Reglas.resumen(avisos, hoy) }

    LazyColumn(
        Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina),
        contentPadding = RellenoLista
    ) {
        item(key = "cabecera") {
            Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                Row(Modifier.padding(horizontal = 24.dp).padding(bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Agenda", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    Text(fechaCorta(hoy), Modifier.padding(bottom = 6.dp), style = EstiloEtiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Buscador(filtro.texto, { filtro = filtro.copy(texto = it) }, Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Ficha("Vencidos", resumen.vencidos, filtro.vencidos, alerta = true) { filtro = filtro.copy(vencidos = !filtro.vencidos) }
                    Ficha("Urgentes", resumen.urgentes, filtro.urgentes) { filtro = filtro.copy(urgentes = !filtro.urgentes) }
                    Ficha("Sin asignar", resumen.sinAsignar, filtro.sinAsignar) { filtro = filtro.copy(sinAsignar = !filtro.sinAsignar) }
                    Ficha("Esta semana", resumen.semana, filtro.semana) { filtro = filtro.copy(semana = !filtro.semana) }
                }
            }
        }

        if (filtro.activo) {
            val res = Reglas.filtrar(avisos, tecnicos, filtro, hoy)
            item(key = "cab-res") {
                CabeceraSeccion(
                    if (res.isEmpty()) "Nada coincide" else if (res.size == 1) "1 resultado" else "${res.size} resultados",
                    extra = if (filtro.texto.isNotBlank()) "abiertos y cerrados" else ""
                )
            }
            itemsIndexed(res, key = { _, a -> "r-" + a.id }) { i, a ->
                FilaAgenda(a, porId[a.asignadoA], hoy, formaEnGrupo(i, res.size), acciones, Modifier.animateItem())
            }
            return@LazyColumn
        }

        if (avisos.isEmpty()) {
            item(key = "vacio") { Vacio(acciones) }
            return@LazyColumn
        }

        Reglas.siguiente(avisos, hoy, ahora.toLocalTime())?.let { s ->
            item(key = "siguiente") {
                TarjetaSiguiente(s, Reglas.progresoHoy(avisos, hoy), acciones, Modifier.padding(horizontal = 16.dp).entrada(0))
            }
        }

        if (ajustes.pistaGestos && avisos.any { it.abierto }) {
            item(key = "pista") { Pista(acciones.ocultarPista) }
        }

        var orden = 1
        Reglas.grupos(avisos, hoy).forEach { g ->
            val base = orden
            item(key = "cab-" + g.clave) {
                CabeceraSeccion(g.titulo, Modifier.entrada(base), alerta = g.alerta, extra = if (g.avisos.isEmpty()) "" else "${g.avisos.size}")
            }
            if (g.avisos.isEmpty()) {
                item(key = "nada-" + g.clave) {
                    Elemento(RoundedCornerShape(18.dp), Modifier.padding(horizontal = 16.dp).entrada(base + 1)) {
                        Text("Nada programado para hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            itemsIndexed(g.avisos, key = { _, a -> a.id }) { i, a ->
                FilaAgenda(a, porId[a.asignadoA], hoy, formaEnGrupo(i, g.avisos.size), acciones, Modifier.animateItem().entrada(base + 1 + i))
            }
            orden += 1 + g.avisos.size
        }
        if (avisos.none { it.abierto }) {
            item(key = "aldia") {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Todo al día", style = MaterialTheme.typography.titleLarge)
                    Text("No queda ningún aviso abierto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** «JUE 12 NOV». */
private fun fechaCorta(d: LocalDate) =
    (d.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " " + d.dayOfMonth + " " +
        d.month.getDisplayName(TextStyle.SHORT, ES).replace(".", "")).uppercase(ES)

@Composable
private fun FilaAgenda(a: Aviso, t: Tecnico?, hoy: LocalDate, forma: Shape, acciones: AccionesAgenda, modifier: Modifier) {
    FilaDeslizable(
        a, forma, modifier.padding(horizontal = 16.dp).padding(bottom = 3.dp),
        alHecho = { acciones.cambiarEstado(a, Estado.RESUELTO) },
        alDerecha = { acciones.cambiarEstado(a, if (a.estadoE == Estado.EN_CURSO) Estado.EN_ESPERA else Estado.EN_CURSO) }
    ) {
        FilaAviso(a, t, Reglas.cuando(a, hoy), forma, alerta = Reglas.vencido(a, hoy), alPulsar = { acciones.abrir(a.id) })
    }
}

/**
 * Deslizar a la izquierda = hecho; a la derecha = en curso (o en espera si ya
 * lo está). El icono de detrás crece con el gesto y, al pasar el punto en que
 * se aplica, el móvil da un toque. La fila vuelve a su sitio: el cambio lo
 * pinta la lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilaDeslizable(
    aviso: Aviso,
    forma: Shape,
    modifier: Modifier = Modifier,
    alHecho: () -> Unit,
    alDerecha: () -> Unit,
    contenido: @Composable () -> Unit
) {
    val hecho by rememberUpdatedState(alHecho)
    val derecha by rememberUpdatedState(alDerecha)
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.EndToStart -> hecho()
                SwipeToDismissBoxValue.StartToEnd -> derecha()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            v == SwipeToDismissBoxValue.Settled
        },
        positionalThreshold = { it * 0.3f }
    )
    val vibrar = LocalHapticFeedback.current
    LaunchedEffect(estado.targetValue) {
        if (estado.targetValue != SwipeToDismissBoxValue.Settled) vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val colores = LocalColores.current
    SwipeToDismissBox(
        estado,
        modifier = modifier.clip(forma),
        enableDismissFromStartToEnd = aviso.abierto,
        enableDismissFromEndToStart = aviso.abierto,
        backgroundContent = {
            val dir = estado.dismissDirection
            val enCurso = aviso.estadoE == Estado.EN_CURSO
            val (e, icono, texto) = when (dir) {
                SwipeToDismissBoxValue.EndToStart -> Triple(Estado.RESUELTO, Icons.Rounded.CheckCircle, "HECHO")
                SwipeToDismissBoxValue.StartToEnd ->
                    if (enCurso) Triple(Estado.EN_ESPERA, Icons.Rounded.PauseCircle, "EN ESPERA")
                    else Triple(Estado.EN_CURSO, Icons.Rounded.Construction, "EN CURSO")
                else -> Triple(Estado.PENDIENTE, Icons.Rounded.Check, "")
            }
            val listo = estado.targetValue != SwipeToDismissBoxValue.Settled
            val fondo by animateColorAsState(
                if (texto.isEmpty()) Color.Transparent else colores.de(e).color.copy(alpha = if (listo) 1f else 0.55f),
                Muelles.suave(), label = "fondo"
            )
            val escala by animateFloatAsState(if (listo) 1.15f else 0.85f, Muelles.soltar(), label = "escala")
            val tinta = if (e == Estado.RESUELTO || e == Estado.EN_ESPERA || e == Estado.EN_CURSO) Color(0xFF141416) else Color.White
            Box(
                Modifier.fillMaxSize().background(fondo).padding(horizontal = 24.dp),
                contentAlignment = if (dir == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (texto.isNotEmpty()) {
                    Row(
                        Modifier.graphicsLayer { scaleX = escala; scaleY = escala },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(icono, null, tint = tinta)
                        Text(texto, color = tinta, style = EstiloEtiqueta)
                    }
                }
            }
        }
    ) { contenido() }
}

@Composable
fun Buscador(texto: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier, ayuda: String = "Buscar cliente, dirección, ref.") {
    val forma = CircleShape
    Row(
        modifier.fillMaxWidth().height(52.dp).tarjeta(forma).padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (texto.isEmpty()) {
                Text(ayuda, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
            BasicTextField(
                texto, alCambiar, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (texto.isNotEmpty()) {
            IconButton(onClick = { alCambiar("") }) { Icon(Icons.Rounded.Close, "Borrar búsqueda") }
        }
    }
}

@Composable
private fun Ficha(texto: String, numero: Int, activa: Boolean, alerta: Boolean = false, alPulsar: () -> Unit) {
    FilterChip(
        selected = activa, onClick = alPulsar,
        label = { Text(if (numero > 0) "$texto  $numero" else texto) },
        shape = CircleShape,
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = activa, borderColor = LocalColores.current.superficies.borde, borderWidth = 1.dp
        ),
        colors = FilterChipDefaults.filterChipColors(containerColor = LocalColores.current.superficies.item),
        leadingIcon = when {
            activa -> ({ Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) })
            alerta && numero > 0 -> ({ Icon(Icons.Rounded.Warning, null, Modifier.size(FilterChipDefaults.IconSize), tint = MaterialTheme.colorScheme.primary) })
            else -> null
        }
    )
}

/**
 * Tarjeta del siguiente aviso: oscura en los dos modos, con un halo naranja,
 * el punto que respira si está en curso y el día en tramos que se llenan.
 */
@Composable
private fun TarjetaSiguiente(s: Reglas.Siguiente, progreso: Reglas.Progreso, acciones: AccionesAgenda, modifier: Modifier) {
    val a = s.aviso
    val sup = LocalColores.current.superficies
    val naranjaTexto = Color(0xFFFF6A33)   // legible sobre el fondo oscuro de la tarjeta
    val enCurso = s.momento == Reglas.Momento.EN_CURSO
    val encabezado = when (s.momento) {
        Reglas.Momento.EN_CURSO -> "EN CURSO" + if (a.hora.isNotEmpty()) " · ${a.hora}" else ""
        Reglas.Momento.SIN_HORA -> "HOY"
        else -> "SIGUIENTE · ${a.hora}"
    }
    val cuanto = when (s.momento) {
        Reglas.Momento.PROXIMO -> if (s.minutos < 1) "AHORA" else "EN " + Reglas.minutos(s.minutos).uppercase()
        Reglas.Momento.PASADO -> "HACE " + Reglas.minutos(s.minutos).uppercase()
        else -> ""
    }
    val forma = RoundedCornerShape(28.dp)
    val i = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .presionable(i, 0.985f)
            .clip(forma)
            .background(sup.heroe)
            .drawBehind {
                /* Halo naranja arriba a la derecha: el toque «tech». */
                drawCircle(
                    Brush.radialGradient(
                        listOf(RedOrange.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.95f, 0f), radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f, center = Offset(size.width * 0.95f, 0f)
                )
            }
            .border(1.dp, if (sup.oscuro) sup.borde else Color.Transparent, forma)
            .clickable(i, null) { acciones.abrir(a.id) }
            .padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).pulso(if (enCurso) RedOrange else sup.sobreHeroeSuave, activo = enCurso, radio = 4f)
                    .clip(CircleShape).background(if (enCurso) RedOrange else sup.sobreHeroeSuave)
            )
            Text(encabezado, Modifier.weight(1f).padding(start = 10.dp), color = naranjaTexto, style = EstiloEtiqueta)
            if (cuanto.isNotEmpty()) Text(cuanto, style = EstiloEtiqueta, color = sup.sobreHeroeSuave)
        }
        Text(
            a.titulo, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.headlineSmall,
            color = sup.sobreHeroe, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        val donde = listOf(a.cliente.nombre, a.cliente.direccion).filter { it.isNotBlank() }.joinToString(" · ")
        if (donde.isNotEmpty()) {
            Text(donde, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = sup.sobreHeroeSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val relleno = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            Button(
                onClick = { acciones.irA(a.cliente.direccion) }, enabled = a.cliente.direccion.isNotBlank(),
                contentPadding = relleno, modifier = Modifier.weight(1f).height(48.dp)
            ) { Icon(Icons.Rounded.Navigation, null, Modifier.size(20.dp)); Spacer(Modifier.size(6.dp)); Text("Ir", maxLines = 1) }
            val claro = ButtonDefaults.outlinedButtonColors(contentColor = sup.sobreHeroe)
            val linea = androidx.compose.foundation.BorderStroke(1.dp, sup.sobreHeroe.copy(alpha = 0.25f))
            OutlinedButton(
                onClick = { acciones.llamar(a.cliente.telefono) }, enabled = a.cliente.telefono.isNotBlank(),
                colors = claro, border = linea, contentPadding = relleno, modifier = Modifier.weight(1f).height(48.dp)
            ) { Icon(Icons.Rounded.Call, null, Modifier.size(20.dp)); Spacer(Modifier.size(6.dp)); Text("Llamar", maxLines = 1) }
            if (enCurso) {
                OutlinedButton(onClick = { acciones.cambiarEstado(a, Estado.RESUELTO) }, colors = claro, border = linea, contentPadding = relleno, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(20.dp)); Spacer(Modifier.size(6.dp)); Text("Hecho", maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = { acciones.cambiarEstado(a, Estado.EN_CURSO) }, colors = claro, border = linea, contentPadding = relleno, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.size(6.dp)); Text("Empezar", maxLines = 1)
                }
            }
        }
        if (progreso.total > 0) {
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Tramos(progreso, Modifier.weight(1f))
                Text(
                    "${progreso.hechos}/${progreso.total} HOY", Modifier.padding(start = 12.dp),
                    style = EstiloEtiqueta, color = sup.sobreHeroeSuave
                )
            }
        }
    }
}

/** El día en tramos: uno por aviso de hoy; los hechos se encienden en naranja, uno tras otro. */
@Composable
private fun Tramos(p: Reglas.Progreso, modifier: Modifier) {
    val sup = LocalColores.current.superficies
    Row(modifier.height(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(p.total) { n ->
            val encendido by animateFloatAsState(
                if (n < p.hechos) 1f else 0f,
                androidx.compose.animation.core.tween(500, delayMillis = if (LocalMovimiento.current) 250 + n * 120 else 0), label = "tramo"
            )
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(sup.sobreHeroe.copy(alpha = 0.16f))
            ) {
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = encendido; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }.background(RedOrange))
            }
        }
    }
}

@Composable
private fun Pista(alCerrar: () -> Unit) {
    Elemento(RoundedCornerShape(18.dp), Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
        Icon(Icons.Rounded.SwipeLeft, null, tint = MaterialTheme.colorScheme.primary)
        Text(
            "Desliza un aviso a la izquierda para darlo por hecho, o a la derecha para ponerlo en curso.",
            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = alCerrar) { Icon(Icons.Rounded.Close, "Entendido") }
    }
}

@Composable
private fun Vacio(acciones: AccionesAgenda) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp).entrada(0),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(72.dp).pulso(RedOrange, radio = 22f).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Schedule, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        Text("Todavía no hay avisos", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge)
        Text(
            "Crea el primero con el botón + o carga unos de ejemplo para ver cómo funciona.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = acciones.nuevo, modifier = Modifier.padding(top = 8.dp)) { Text("Nuevo aviso") }
        FilledTonalButton(onClick = acciones.ejemplo) { Text("Cargar avisos de ejemplo") }
    }
}
