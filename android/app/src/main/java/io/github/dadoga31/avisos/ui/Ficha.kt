package io.github.dadoga31.avisos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AccionesFicha(
    val volver: () -> Unit = {},
    val editar: () -> Unit = {},
    val estado: (Estado) -> Unit = {},
    val horas: (Double) -> Unit = {},
    val nota: (String) -> Unit = {},
    val borrarNota: (String) -> Unit = {},
    val fecha: (String, String) -> Unit = { _, _ -> },
    val asignar: (String) -> Unit = {},
    val duplicar: () -> Unit = {},
    val borrar: () -> Unit = {},
    val llamar: (String) -> Unit = {},
    val whatsapp: (String) -> Unit = {},
    val irA: (String) -> Unit = {},
    val abrirAdjunto: (io.github.dadoga31.avisos.datos.Adjunto) -> Unit = {},
    val borrarAdjunto: (io.github.dadoga31.avisos.datos.Adjunto) -> Unit = {},
    val camara: () -> Unit = {},
    val galeria: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FichaPantalla(
    aviso: Aviso,
    tecnicos: List<Tecnico>,
    ahora: LocalDateTime,
    acciones: AccionesFicha,
    hojaEstadoAbierta: Boolean = false
) {
    val hoy = ahora.toLocalDate()
    val tecnico = tecnicos.firstOrNull { it.id == aviso.asignadoA }
    var hojaEstado by remember { mutableStateOf(hojaEstadoAbierta) }
    var menu by remember { mutableStateOf(false) }
    var pedirFecha by remember { mutableStateOf(false) }
    var pedirHora by remember { mutableStateOf<LocalDate?>(null) }
    var pedirTecnico by remember { mutableStateOf(false) }
    var pedirNota by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    var borrarAdj by remember { mutableStateOf<io.github.dadoga31.avisos.datos.Adjunto?>(null) }
    val e = aviso.estadoE
    val ce = LocalColores.current.de(e)

    Box(Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.statusBarsPadding().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = acciones.volver) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
                Text(aviso.ref.ifEmpty { "Aviso" }, Modifier.weight(1f).padding(start = 4.dp), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = acciones.editar) { Icon(Icons.Rounded.Edit, "Editar") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Más") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Duplicar") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = { menu = false; acciones.duplicar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = { menu = false; confirmarBorrar = true }
                        )
                    }
                }
            }

            Column(Modifier.padding(horizontal = 24.dp)) {
                Surface(
                    onClick = { hojaEstado = true }, shape = CircleShape, color = ce.fondo, contentColor = ce.sobreFondo
                ) {
                    Row(Modifier.height(40.dp).padding(start = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(iconoEstado(e), null, Modifier.size(20.dp))
                        Text(e.nombre, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Rounded.ArrowDropDown, "Cambiar estado")
                    }
                }
                Text(
                    aviso.titulo.ifBlank { "(sin título)" }, Modifier.padding(top = 14.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (aviso.cliente.nombre.isNotBlank()) {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Apartment, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            aviso.cliente.nombre, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta(aviso.tipoE.nombre, iconoTipo(aviso.tipoE))
                    Etiqueta(aviso.sistemaE.nombre, iconoSistema(aviso.sistemaE))
                    Etiqueta("Prioridad " + aviso.prioridadE.nombre.lowercase(), Icons.Rounded.Flag)
                }
            }

            CabeceraSeccion("Cuándo y quién")
            val empezado = aviso.empezado.takeIf { e == Estado.EN_CURSO && it.isNotEmpty() }?.let {
                runCatching {
                    "Empezado a las " + java.time.Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrNull()
            }
            val prevision = aviso.duracion.takeIf { it.isNotBlank() }?.let { "$it h previstas" }
            Grupo(
                listOf(
                    { f ->
                        /* «Hoy · 16:00» arriba y la fecha larga debajo, para que quepa en una línea. */
                        val dia = Reglas.dia(aviso.fecha, hoy)
                        val larga = runCatching { Reglas.fechaLarga(LocalDate.parse(aviso.fecha)) }.getOrDefault(aviso.fecha)
                        val cercano = dia == "Hoy" || dia == "Mañana" || dia == "Ayer"
                        val titulo = when {
                            aviso.fecha.isEmpty() -> "Sin fecha"
                            cercano -> dia
                            else -> larga.replaceFirstChar { it.uppercase() }
                        } + if (aviso.hora.isNotEmpty()) " · ${aviso.hora}" else ""
                        FilaSimple(
                            f, Icons.Rounded.Event, titulo,
                            listOfNotNull(larga.takeIf { cercano }, empezado, prevision).joinToString(" · ").ifEmpty { "Toca para cambiar" },
                            alPulsar = { pedirFecha = true }
                        ) { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    { f ->
                        FilaSimple(f, Icons.Rounded.Person, tecnico?.nombre ?: "Sin asignar", if (tecnico != null) "Asignado" else "Toca para asignar",
                            alPulsar = { pedirTecnico = true }
                        ) { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                    { f ->
                        FilaSimple(f, Icons.Rounded.Timer, "Horas trabajadas") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(onClick = { acciones.horas(-0.5) }, enabled = aviso.horas > 0, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Remove, "Quitar media hora", Modifier.size(20.dp))
                                }
                                Text(
                                    Reglas.horas(aviso.horas), Modifier.widthIn(min = 52.dp), style = MaterialTheme.typography.titleMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                FilledTonalIconButton(onClick = { acciones.horas(0.5) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Add, "Añadir media hora", Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                )
            )

            val c = aviso.cliente
            if (c.direccion.isNotBlank() || c.telefono.isNotBlank() || c.contacto.isNotBlank()) {
                CabeceraSeccion("Cliente")
                val filas = mutableListOf<@Composable (androidx.compose.ui.graphics.Shape) -> Unit>()
                if (c.direccion.isNotBlank()) filas += { f ->
                    FilaSimple(f, Icons.Rounded.LocationOn, c.direccion, "Abrir en el mapa", alPulsar = { acciones.irA(c.direccion) }) {
                        Icon(Icons.Rounded.Map, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (c.telefono.isNotBlank() || c.contacto.isNotBlank()) filas += { f ->
                    FilaSimple(
                        f, Icons.Rounded.Phone, c.telefono.ifBlank { c.contacto }, if (c.telefono.isNotBlank()) c.contacto else null,
                        alPulsar = if (c.telefono.isNotBlank()) ({ acciones.llamar(c.telefono) }) else null
                    )
                }
                Grupo(filas)
            }

            if (aviso.descripcion.isNotBlank()) {
                CabeceraSeccion("Descripción")
                Grupo(listOf { f -> FilaSimple(f, Icons.AutoMirrored.Rounded.Notes, aviso.descripcion) })
            }

            CabeceraSeccion("Fotos y adjuntos", extra = if (aviso.adjuntos.isEmpty()) "" else "${aviso.adjuntos.size}")
            val fotos = aviso.adjuntos.filter { it.esImagen }
            val otros = aviso.adjuntos.filterNot { it.esImagen }
            if (fotos.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(fotos.size, key = { fotos[it].id }) { i ->
                        val adj = fotos[i]
                        Miniatura(adj, alPulsar = { acciones.abrirAdjunto(adj) }, alMantener = { borrarAdj = adj })
                    }
                }
            }
            if (otros.isNotEmpty()) {
                Grupo(
                    otros.map<io.github.dadoga31.avisos.datos.Adjunto, @Composable (androidx.compose.ui.graphics.Shape) -> Unit> { adj ->
                        { f ->
                            FilaSimple(
                                f, if (adj.esPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.AttachFile,
                                adj.nombre, tamano(adj.tam), alPulsar = { acciones.abrirAdjunto(adj) }, alMantener = { borrarAdj = adj }
                            ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                )
            }
            Row(
                Modifier.padding(horizontal = 16.dp).padding(top = if (otros.isNotEmpty()) 8.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.FilledTonalButton(onClick = acciones.camara, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Cámara")
                }
                androidx.compose.material3.FilledTonalButton(onClick = acciones.galeria, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Galería")
                }
            }

            CabeceraSeccion("Notas")
            Grupo(
                aviso.notas.map<io.github.dadoga31.avisos.datos.Nota, @Composable (androidx.compose.ui.graphics.Shape) -> Unit> { n ->
                    { f ->
                        FilaSimple(f, Icons.Rounded.EditNote, n.texto, momento(n.ts, hoy)) {
                            IconButton(onClick = { acciones.borrarNota(n.id) }) {
                                Icon(Icons.Rounded.Delete, "Borrar nota", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } + listOf { f -> FilaSimple(f, Icons.Rounded.AddCircle, "Añadir nota", alPulsar = { pedirNota = true }) }
            )
            Spacer(Modifier.height(140.dp))
        }

        BarraAcciones(aviso, acciones, Modifier.align(Alignment.BottomCenter))
    }

    if (hojaEstado) {
        ModalBottomSheet(onDismissRequest = { hojaEstado = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            HojaEstado(aviso) { acciones.estado(it); hojaEstado = false }
        }
    }
    if (pedirFecha) {
        DialogoFecha(
            runCatching { LocalDate.parse(aviso.fecha) }.getOrNull(),
            alElegir = { pedirHora = it }, alCerrar = { pedirFecha = false }
        )
    }
    pedirHora?.let { d ->
        DialogoHora(
            runCatching { LocalTime.parse(aviso.hora) }.getOrNull(),
            alElegir = { h -> acciones.fecha(d.toString(), h?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "") },
            alCerrar = { pedirHora = null }
        )
    }
    if (pedirTecnico) {
        AlertDialog(
            onDismissRequest = { pedirTecnico = false },
            title = { Text("Asignar a") },
            text = {
                Column {
                    (tecnicos.map { it.id to it.nombre } + ("" to "Sin asignar")).forEach { (id, nombre) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { acciones.asignar(id); pedirTecnico = false }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = aviso.asignadoA == id, onClick = { acciones.asignar(id); pedirTecnico = false })
                            Text(nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pedirTecnico = false }) { Text("Cerrar") } }
        )
    }
    if (pedirNota) {
        DialogoTexto("Nota", "Qué has hecho o qué falta", variasLineas = true, textoAceptar = "Añadir",
            alAceptar = acciones.nota, alCerrar = { pedirNota = false })
    }
    borrarAdj?.let { adj ->
        DialogoConfirmar(
            "¿Borrar ${adj.nombre}?", "Se borra de este móvil. No se puede deshacer.", "Borrar",
            alAceptar = { acciones.borrarAdjunto(adj) }, alCerrar = { borrarAdj = null }
        )
    }
    if (confirmarBorrar) {
        DialogoConfirmar("¿Borrar ${aviso.ref}?", "Se borra el aviso con sus notas. No se puede deshacer.", "Borrar",
            alAceptar = acciones.borrar, alCerrar = { confirmarBorrar = false })
    }
}

/** En las capturas las miniaturas se cargan al momento; en la app, en segundo plano. */
val LocalMiniaturasAlMomento = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Las ya cargadas, para no volver a leerlas del disco al volver a la ficha. */
private val cacheMiniaturas = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(32)

/** Miniatura de una foto: tocar la abre, mantener pulsado pregunta si borrarla. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Miniatura(adj: io.github.dadoga31.avisos.datos.Adjunto, alPulsar: () -> Unit, alMantener: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun cargar() = cacheMiniaturas.get(adj.ruta)
        ?: io.github.dadoga31.avisos.Fotos.miniatura(java.io.File(ctx.filesDir, adj.ruta), 320)?.asImageBitmap()
            ?.also { cacheMiniaturas.put(adj.ruta, it) }
    val alMomento = LocalMiniaturasAlMomento.current
    val img by androidx.compose.runtime.produceState(
        if (alMomento) cargar() else cacheMiniaturas.get(adj.ruta), adj.ruta
    ) {
        if (value == null) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { cargar() }
    }
    Box(
        Modifier.size(104.dp).clip(RoundedCornerShape(16.dp)).background(LocalColores.current.superficies.item)
            .combinedClickable(onClick = alPulsar, onLongClick = alMantener),
        contentAlignment = Alignment.Center
    ) {
        val b = img
        if (b != null) {
            androidx.compose.foundation.Image(
                b, adj.nombre, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else Icon(Icons.Rounded.Image, adj.nombre, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Etiqueta(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector) {
    AssistChip(
        onClick = {}, label = { Text(texto) },
        leadingIcon = { Icon(icono, null, Modifier.size(AssistChipDefaults.IconSize)) }
    )
}

private fun tamano(b: Long): String = when {
    b >= 1_048_576 -> String.format(java.util.Locale("es", "ES"), "%.1f MB", b / 1_048_576.0)
    b >= 1024 -> "${b / 1024} KB"
    else -> "$b bytes"
}

private fun momento(ts: String, hoy: LocalDate): String = runCatching {
    val t = java.time.Instant.parse(ts).atZone(ZoneId.systemDefault())
    Reglas.dia(t.toLocalDate().toString(), hoy) + " " + t.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault("")

/** La siguiente acción lógica según el estado. */
private fun accionPrincipal(e: Estado): Triple<String, androidx.compose.ui.graphics.vector.ImageVector, Estado> = when (e) {
    Estado.EN_CURSO -> Triple("Marcar resuelto", Icons.Rounded.Check, Estado.RESUELTO)
    Estado.RESUELTO, Estado.CANCELADO -> Triple("Reabrir", Icons.Rounded.Replay, Estado.PENDIENTE)
    else -> Triple("Empezar", Icons.Rounded.PlayArrow, Estado.EN_CURSO)
}

@Composable
private fun BarraAcciones(aviso: Aviso, acciones: AccionesFicha, modifier: Modifier) {
    val c = aviso.cliente
    val (texto, icono, destino) = accionPrincipal(aviso.estadoE)
    Surface(
        shape = CircleShape, color = LocalColores.current.superficies.flotante, shadowElevation = 8.dp,
        modifier = modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth()
    ) {
        Row(Modifier.height(72.dp).padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { acciones.llamar(c.telefono) }, enabled = c.telefono.isNotBlank()) { Icon(Icons.Rounded.Call, "Llamar") }
            IconButton(onClick = { acciones.whatsapp(c.telefono) }, enabled = c.telefono.isNotBlank()) {
                Icon(Icons.AutoMirrored.Rounded.Chat, "WhatsApp")
            }
            IconButton(onClick = { acciones.irA(c.direccion) }, enabled = c.direccion.isNotBlank()) { Icon(Icons.Rounded.Navigation, "Cómo llegar") }
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = { acciones.estado(destino) }, shape = CircleShape,
                color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(Modifier.height(56.dp).padding(start = 18.dp, end = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icono, null)
                    Text(texto, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun HojaEstado(aviso: Aviso, alElegir: (Estado) -> Unit) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text("Estado del aviso", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
        Text(
            listOf(aviso.ref, aviso.cliente.nombre).filter { it.isNotBlank() }.joinToString(" · "),
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Estado.entries.forEach { e ->
            val sel = e == aviso.estadoE
            Row(
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .then(if (sel) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
                    .clickable { alElegir(e) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CirculoEstado(e)
                Column(Modifier.weight(1f)) {
                    Text(e.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(e.explicacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (sel) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}
