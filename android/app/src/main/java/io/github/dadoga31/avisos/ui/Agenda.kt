package io.github.dadoga31.avisos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import java.time.LocalDateTime

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
        item(key = "buscador") {
            Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
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

        if (ajustes.pistaGestos && avisos.any { it.abierto }) {
            item(key = "pista") { Pista(acciones.ocultarPista) }
        }

        Reglas.siguiente(avisos, hoy, ahora.toLocalTime())?.let { s ->
            item(key = "siguiente") {
                TarjetaSiguiente(s, Reglas.progresoHoy(avisos, hoy), acciones, Modifier.padding(horizontal = 16.dp))
            }
        }

        val grupos = Reglas.grupos(avisos, hoy)
        grupos.forEach { g ->
            item(key = "cab-" + g.clave) {
                CabeceraSeccion(g.titulo, alerta = g.alerta, extra = if (g.avisos.isEmpty()) "" else "${g.avisos.size}")
            }
            if (g.avisos.isEmpty()) {
                item(key = "nada-" + g.clave) {
                    Elemento(RoundedCornerShape(20.dp), Modifier.padding(horizontal = 16.dp)) {
                        Text("Nada programado para hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            itemsIndexed(g.avisos, key = { _, a -> a.id }) { i, a ->
                FilaAgenda(a, porId[a.asignadoA], hoy, formaEnGrupo(i, g.avisos.size), acciones, Modifier.animateItem())
            }
        }
        if (avisos.none { it.abierto }) {
            item(key = "aldia") {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Todo al día", style = MaterialTheme.typography.titleMedium)
                    Text("No queda ningún aviso abierto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FilaAgenda(a: Aviso, t: Tecnico?, hoy: java.time.LocalDate, forma: Shape, acciones: AccionesAgenda, modifier: Modifier) {
    FilaDeslizable(
        a, forma, modifier.padding(horizontal = 16.dp).padding(bottom = 2.dp),
        alHecho = { acciones.cambiarEstado(a, Estado.RESUELTO) },
        alDerecha = { acciones.cambiarEstado(a, if (a.estadoE == Estado.EN_CURSO) Estado.EN_ESPERA else Estado.EN_CURSO) }
    ) {
        FilaAviso(
            a, t, Reglas.cuando(a, hoy), forma, alerta = Reglas.vencido(a, hoy),
            alPulsar = { acciones.abrir(a.id) }
        )
    }
}

/**
 * Deslizar a la izquierda = hecho; a la derecha = en curso (o en espera si ya
 * lo está). La fila vuelve a su sitio: el cambio lo pinta la lista.
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
    val colores = LocalColores.current
    SwipeToDismissBox(
        estado,
        modifier = modifier.clip(forma),
        enableDismissFromStartToEnd = aviso.abierto,
        enableDismissFromEndToStart = aviso.abierto,
        backgroundContent = {
            val dir = estado.dismissDirection
            val enCurso = aviso.estadoE == Estado.EN_CURSO
            val (fondo, icono, texto, alinear) = when (dir) {
                SwipeToDismissBoxValue.EndToStart ->
                    Cuatro(colores.de(Estado.RESUELTO).color, Icons.Rounded.CheckCircle, "Hecho", Alignment.CenterEnd)
                SwipeToDismissBoxValue.StartToEnd ->
                    if (enCurso) Cuatro(colores.de(Estado.EN_ESPERA).color, Icons.Rounded.PauseCircle, "En espera", Alignment.CenterStart)
                    else Cuatro(colores.de(Estado.EN_CURSO).color, Icons.Rounded.Construction, "En curso", Alignment.CenterStart)
                else -> Cuatro(Color.Transparent, Icons.Rounded.Check, "", Alignment.Center)
            }
            Box(Modifier.fillMaxSize().background(fondo).padding(horizontal = 24.dp), contentAlignment = alinear) {
                if (texto.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(icono, null, tint = MaterialTheme.colorScheme.surface)
                        Text(texto, color = MaterialTheme.colorScheme.surface, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    ) { contenido() }
}

private data class Cuatro<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun Buscador(texto: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier, ayuda: String = "Buscar cliente, dirección, ref.") {
    val oscuro = LocalColores.current.superficies.oscuro
    Surface(
        shape = CircleShape,
        color = if (oscuro) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun Ficha(texto: String, numero: Int, activa: Boolean, alerta: Boolean = false, alPulsar: () -> Unit) {
    FilterChip(
        selected = activa, onClick = alPulsar,
        label = { Text(if (numero > 0) "$texto  $numero" else texto) },
        leadingIcon = when {
            activa -> ({ Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) })
            alerta && numero > 0 -> ({ Icon(Icons.Rounded.Warning, null, Modifier.size(FilterChipDefaults.IconSize), tint = MaterialTheme.colorScheme.error) })
            else -> null
        }
    )
}

@Composable
private fun TarjetaSiguiente(s: Reglas.Siguiente, progreso: Reglas.Progreso, acciones: AccionesAgenda, modifier: Modifier) {
    val a = s.aviso
    val encabezado = when (s.momento) {
        Reglas.Momento.EN_CURSO -> "EN CURSO" + if (a.hora.isNotEmpty()) " · ${a.hora}" else ""
        Reglas.Momento.SIN_HORA -> "HOY"
        else -> "SIGUIENTE · ${a.hora}"
    }
    val cuanto = when (s.momento) {
        Reglas.Momento.PROXIMO -> if (s.minutos < 1) "ahora" else "en " + Reglas.minutos(s.minutos)
        Reglas.Momento.PASADO -> "hace " + Reglas.minutos(s.minutos)
        else -> ""
    }
    Surface(
        shape = RoundedCornerShape(28.dp), color = LocalColores.current.superficies.item,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(28.dp)).clickable { acciones.abrir(a.id) }
    ) {
        Column(Modifier.padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    encabezado, Modifier.weight(1f), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
                )
                if (cuanto.isNotEmpty()) {
                    Text(cuanto, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                a.titulo, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val donde = listOf(a.cliente.nombre, a.cliente.direccion).filter { it.isNotBlank() }.joinToString(" · ")
            if (donde.isNotEmpty()) {
                Text(donde, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { acciones.irA(a.cliente.direccion) }, enabled = a.cliente.direccion.isNotBlank(),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Icon(Icons.Rounded.Navigation, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Ir") }
                FilledTonalButton(
                    onClick = { acciones.llamar(a.cliente.telefono) }, enabled = a.cliente.telefono.isNotBlank(),
                    modifier = Modifier.height(48.dp)
                ) { Icon(Icons.Rounded.Call, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Llamar") }
                if (a.estadoE == Estado.EN_CURSO) {
                    FilledTonalButton(onClick = { acciones.cambiarEstado(a, Estado.RESUELTO) }, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Rounded.Check, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Hecho")
                    }
                } else {
                    FilledTonalButton(onClick = { acciones.cambiarEstado(a, Estado.EN_CURSO) }, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Empezar")
                    }
                }
            }
            if (progreso.total > 0) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hoy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { progreso.hechos.toFloat() / progreso.total },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = LocalColores.current.de(Estado.RESUELTO).color,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round, gapSize = 3.dp, drawStopIndicator = {}
                    )
                    Text(
                        "${progreso.hechos} de ${progreso.total} hechos", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Pista(alCerrar: () -> Unit) {
    Elemento(RoundedCornerShape(20.dp), Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Schedule, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Todavía no hay avisos", style = MaterialTheme.typography.titleLarge)
        Text(
            "Crea el primero con el botón + o carga unos de ejemplo para ver cómo funciona.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = acciones.nuevo, modifier = Modifier.padding(top = 8.dp)) { Text("Nuevo aviso") }
        FilledTonalButton(onClick = acciones.ejemplo) { Text("Cargar avisos de ejemplo") }
    }
}
