package io.github.dadoga31.avisos.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import io.github.dadoga31.avisos.datos.Adjunto
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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
    val abrirAdjunto: (Adjunto) -> Unit = {},
    val borrarAdjunto: (Adjunto) -> Unit = {},
    val camara: () -> Unit = {},
    val galeria: () -> Unit = {},
    val responder: () -> Unit = {},
    val reenviar: () -> Unit = {}
)

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

private fun local(iso: String): LocalDateTime? = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()

/**
 * La ficha es una «hoja de misión»: los pasos del estado, el título en
 * grande, el cronómetro (o la cuenta atrás) en una tarjeta oscura, los datos
 * en rejilla, las acciones al pulgar, el registro con sus horas y, abajo,
 * deslizar para empezar o para cerrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var borrarAdj by remember { mutableStateOf<Adjunto?>(null) }
    var borrarNotaId by remember { mutableStateOf<String?>(null) }
    var menuFoto by remember { mutableStateOf(false) }
    val s = LocalColores.current.superficies
    val cristal = remember { HazeState() }

    Box(Modifier.fillMaxSize().fondoPagina()) {
        Column(Modifier.fillMaxSize().haze(cristal).verticalScroll(rememberScrollState())) {
            Box(Modifier.statusBarsPadding().fillMaxWidth().height(60.dp).padding(horizontal = 12.dp)) {
                BotonRedondo(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", acciones.volver, Modifier.align(Alignment.CenterStart))
                Dato(aviso.ref.ifEmpty { "Aviso" }, Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, tam = 12f, peso = FontWeight.SemiBold)
                Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonRedondo(Icons.Rounded.Edit, "Editar", acciones.editar)
                    Box {
                        BotonRedondo(Icons.Rounded.MoreHoriz, "Más", { menu = true })
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Cambiar estado") }, leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                                onClick = { menu = false; hojaEstado = true }
                            )
                            if (!Plantillas.deCorreo(aviso)) DropdownMenuItem(
                                text = { Text("Enviar por correo") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Forward, null) },
                                onClick = { menu = false; acciones.reenviar() }
                            )
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
            }

            Pasos(aviso.estadoE, Modifier.padding(start = 26.dp, end = 26.dp, top = 6.dp).entrada(0)) { hojaEstado = true }

            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp).entrada(1)) {
                Text(
                    aviso.titulo.ifBlank { "(sin título)" }, Modifier.compartido("titulo-${aviso.id}"),
                    style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 29.sp, lineHeight = 35.sp, letterSpacing = (-0.6).sp)
                )
                val donde = listOf(aviso.cliente.nombre, aviso.cliente.direccion).filter { it.isNotBlank() }.joinToString(" · ")
                if (donde.isNotEmpty()) Dato(donde, Modifier.padding(top = 10.dp))
            }

            Heroe(aviso, ahora, Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).entrada(2))

            Rejilla(aviso, tecnico, hoy, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).entrada(3),
                alFecha = { pedirFecha = true }, alTecnico = { pedirTecnico = true }, alEditar = acciones.editar, alHoras = acciones.horas)

            val c = aviso.cliente
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 20.dp).entrada(4)) {
                Accion(Icons.Rounded.Call, "Llamar", c.telefono.isNotBlank(), Modifier.weight(1f)) { acciones.llamar(c.telefono) }
                Accion(Icons.AutoMirrored.Rounded.Chat, "WhatsApp", c.telefono.isNotBlank(), Modifier.weight(1f)) { acciones.whatsapp(c.telefono) }
                Accion(Icons.Rounded.NearMe, "Ir", c.direccion.isNotBlank(), Modifier.weight(1f)) { acciones.irA(c.direccion) }
                Box(Modifier.weight(1f)) {
                    Accion(Icons.Rounded.PhotoCamera, "Foto", true, Modifier.fillMaxWidth()) { menuFoto = true }
                    DropdownMenu(menuFoto, { menuFoto = false }) {
                        DropdownMenuItem(text = { Text("Cámara") }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) }, onClick = { menuFoto = false; acciones.camara() })
                        DropdownMenuItem(text = { Text("Galería") }, leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null) }, onClick = { menuFoto = false; acciones.galeria() })
                    }
                }
            }

            if (Plantillas.deCorreo(aviso)) {
                CabeceraDato("Correo", extra = Plantillas.fechaCorreo(aviso))
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().tarjeta(RoundedCornerShape(20.dp)).padding(16.dp)) {
                    Dato("De", tam = 9.5f)
                    Text(
                        Plantillas.remitente(aviso), Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BotonPildora("Responder", Icons.AutoMirrored.Rounded.Reply, acciones.responder, Modifier.weight(1f), alto = 46.dp)
                        BotonLinea("Reenviar", Icons.AutoMirrored.Rounded.Forward, acciones.reenviar, Modifier.weight(1f))
                    }
                }
            }

            if (aviso.descripcion.isNotBlank()) {
                CabeceraDato("Descripción")
                Text(aviso.descripcion, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
            }

            val datos = listOf("Contacto" to c.contacto, "Teléfono" to c.telefono, "Dirección" to c.direccion).filter { it.second.isNotBlank() }
            if (datos.isNotEmpty()) {
                CabeceraDato("Cliente", extra = c.nombre)
                Column(Modifier.padding(horizontal = 24.dp)) {
                    datos.forEach { (k, v) ->
                        val accion: (() -> Unit)? = when (k) {
                            "Teléfono" -> ({ acciones.llamar(v) })
                            "Dirección" -> ({ acciones.irA(v) })
                            else -> null
                        }
                        LineaDato(k, v, accion)
                    }
                }
            }

            CabeceraDato("Fotos y adjuntos", extra = if (aviso.adjuntos.isEmpty()) "" else "${aviso.adjuntos.size}")
            val fotos = aviso.adjuntos.filter { it.esImagen }
            val otros = aviso.adjuntos.filterNot { it.esImagen }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fotos.size, key = { fotos[it].id }) { i ->
                    val adj = fotos[i]
                    Miniatura(adj, alPulsar = { acciones.abrirAdjunto(adj) }, alMantener = { borrarAdj = adj })
                }
                item(key = "camara") { Hueco(Icons.Rounded.PhotoCamera, "Cámara", acciones.camara) }
                item(key = "galeria") { Hueco(Icons.Rounded.PhotoLibrary, "Galería", acciones.galeria) }
            }
            if (otros.isNotEmpty()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    otros.forEach { adj ->
                        FilaSimple(
                            RoundedCornerShape(16.dp), if (adj.esPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.AttachFile,
                            adj.nombre, tamano(adj.tam), alPulsar = { acciones.abrirAdjunto(adj) }, alMantener = { borrarAdj = adj }
                        ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            val registro = registro(aviso)
            CabeceraDato("Registro", extra = "${registro.size}")
            Column(Modifier.padding(horizontal = 24.dp)) {
                registro.forEach { r -> LineaRegistro(r, hoy) { borrarNotaId = r.notaId } }
                Row(
                    Modifier.padding(top = 10.dp).discontinua(s.tenue, 14.dp).clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { pedirNota = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp), tint = RedOrange)
                    Dato("Añadir nota", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(150.dp))
        }

        BarraDeslizar(aviso, cristal, acciones, Modifier.align(Alignment.BottomCenter))
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
            alElegir = { h -> acciones.fecha(d.toString(), h?.format(HHMM) ?: "") },
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
    borrarNotaId?.let { id ->
        DialogoConfirmar("¿Borrar la nota?", "Se quita del registro. No se puede deshacer.", "Borrar",
            alAceptar = { acciones.borrarNota(id) }, alCerrar = { borrarNotaId = null })
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

// ---------------------------------------------------------------- pasos

/** Pendiente → Programado → En curso → Hecho. Tocar abre la hoja de estados. */
@Composable
private fun Pasos(e: Estado, modifier: Modifier, alPulsar: () -> Unit) {
    val c = LocalColores.current
    val s = c.superficies
    val actual = when (e) {
        Estado.PENDIENTE -> 0; Estado.PROGRAMADO -> 1
        Estado.EN_CURSO, Estado.EN_ESPERA -> 2
        Estado.RESUELTO, Estado.CANCELADO -> 3
    }
    val color by animateColorAsState(
        when (e) {
            Estado.EN_ESPERA -> c.de(Estado.EN_ESPERA).color
            Estado.CANCELADO -> s.tenue
            Estado.RESUELTO -> s.ok
            else -> RedOrange
        }, Muelles.suave(), label = "pasos"
    )
    val nombres = listOf(
        "Pendiente", "Programado", if (e == Estado.EN_ESPERA) "En espera" else "En curso",
        if (e == Estado.CANCELADO) "Cancelado" else "Hecho"
    )
    val avance by animateFloatAsState(actual / 3f, Muelles.suave(), label = "avance")
    Column(
        modifier.fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "Cambiar estado", onClick = alPulsar)
            .semantics { contentDescription = "Estado: " + e.nombre }
    ) {
        Box(
            Modifier.fillMaxWidth().height(16.dp).drawBehind {
                val r = 7.dp.toPx()
                val y = size.height / 2
                val g = 2.dp.toPx()
                drawLine(s.borde, Offset(r, y), Offset(size.width - r, y), g)
                drawLine(color, Offset(r, y), Offset(r + (size.width - 2 * r) * avance, y), g)
            }
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                for (n in 0..3) {
                    val hecho = n <= actual
                    Box(
                        Modifier.size(14.dp)
                            .then(if (n == actual) Modifier.drawBehind { drawCircle(color.copy(alpha = 0.22f), radius = size.minDimension / 2 + 5.dp.toPx()) } else Modifier)
                            .clip(CircleShape)
                            .background(if (hecho) color else s.item)
                            .border(2.dp, if (hecho) color else s.tenue, CircleShape)
                    )
                }
            }
        }
        /* Cada nombre bajo su punto, sin salirse por los lados. */
        Layout(
            content = {
                nombres.forEachIndexed { n, t ->
                    Dato(t, tam = 8.5f, color = if (n == actual) (if (e == Estado.EN_CURSO) s.acentoTexto else MaterialTheme.colorScheme.onSurface) else Color.Unspecified)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { medibles, limites ->
            val piezas = medibles.map { it.measure(limites.copy(minWidth = 0)) }
            val ancho = limites.maxWidth
            val r = 7.dp.roundToPx()
            layout(ancho, piezas.maxOf { it.height }) {
                piezas.forEachIndexed { n, p ->
                    val centro = r + (ancho - 2 * r) * n / 3
                    p.place((centro - p.width / 2).coerceIn(0, ancho - p.width), 0)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- tarjeta oscura

/**
 * En curso: el cronómetro desde que se empezó y la barra contra lo previsto.
 * Pendiente con cita: la cuenta atrás (o el tiempo que lleva pasada).
 * Hecho: las horas trabajadas. Sin cita y sin empezar no sale.
 */
@Composable
private fun Heroe(a: Aviso, ahora: LocalDateTime, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val naranja = Color(0xFFFF6A33)
    val verde = Color(0xFF3DDC84)
    val cita = Reglas.cita(a)?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    val e = a.estadoE
    val tipo = when {
        e == Estado.EN_CURSO -> 0
        e == Estado.RESUELTO -> 2
        a.abierto && cita != null && Duration.between(ahora, cita).toHours() < 24 -> 1
        else -> return
    }
    val forma = RoundedCornerShape(24.dp)
    val halo = if (tipo == 2) verde else RedOrange
    Column(
        modifier.fillMaxWidth().clip(forma).background(s.heroe)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(halo.copy(alpha = 0.36f), Color.Transparent), center = Offset(size.width * 0.92f, 0f), radius = size.width * 0.62f),
                    radius = size.width * 0.62f, center = Offset(size.width * 0.92f, 0f)
                )
            }
            .border(1.dp, if (s.oscuro) s.borde else Color.Transparent, forma)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        val grande = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-1).sp)
        when (tipo) {
            0 -> {
                val desde = runCatching { Instant.parse(a.empezado) }.getOrNull()
                val inicio = local(a.empezado)?.toLocalTime()?.format(HHMM)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PuntoVivo(RedOrange, vivo = true)
                    Dato("En curso" + if (inicio != null) " desde $inicio" else "", color = naranja, peso = FontWeight.SemiBold)
                }
                Cronometro(desde, ahora, grande, s.sobreHeroe, naranja, Modifier.padding(top = 12.dp))
                val t = ahoraVivo(ahora)
                val previsto = Reglas.duracionMin(a)
                val llevado = if (desde == null) 0L else Duration.between(desde, t.atZone(ZoneId.systemDefault()).toInstant()).toMinutes()
                val p = (llevado.toFloat() / previsto).coerceIn(0f, 1f)
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(s.sobreHeroe.copy(alpha = 0.14f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(RedOrange))
                    }
                    Dato(
                        if (llevado > previsto) "+" + Reglas.minutos(llevado - previsto) + " sobre lo previsto"
                        else "Previsto " + Reglas.horas(previsto / 60.0),
                        Modifier.padding(start = 10.dp), color = if (llevado > previsto) naranja else s.sobreHeroeSuave, tam = 9.5f
                    )
                }
            }
            1 -> {
                val falta = Duration.between(ahora, cita!!)
                val dia = Reglas.dia(a.fecha, ahora.toLocalDate())
                if (!falta.isNegative) {
                    Dato("Empieza en", color = naranja, peso = FontWeight.SemiBold)
                    CuentaAtras(cita, ahora, grande, Modifier.padding(top = 12.dp))
                } else {
                    Dato(if (Reglas.vencido(a, ahora.toLocalDate())) "Vencido" else "Sin empezar", color = naranja, peso = FontWeight.SemiBold)
                    Cronometro(cita.atZone(ZoneId.systemDefault()).toInstant(), ahora, grande, s.sobreHeroe, naranja, Modifier.padding(top = 12.dp))
                }
                Dato(
                    "$dia ${a.hora}" + if (a.duracion.isNotBlank()) " · previsto " + Reglas.horas(Reglas.duracionMin(a) / 60.0) else "",
                    Modifier.padding(top = 12.dp), color = s.sobreHeroeSuave, tam = 9.5f
                )
            }
            else -> {
                val cierre = local(a.cerrado)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PuntoVivo(verde)
                    Dato(
                        "Hecho" + (cierre?.let { " · " + Reglas.dia(it.toLocalDate().toString(), ahora.toLocalDate()) + " " + it.toLocalTime().format(HHMM) } ?: ""),
                        color = verde, peso = FontWeight.SemiBold
                    )
                }
                Text(
                    if (a.horas > 0) Reglas.horas(a.horas) else "—", Modifier.padding(top = 12.dp),
                    style = grande, color = s.sobreHeroe
                )
                Dato("Trabajadas", Modifier.padding(top = 6.dp), color = s.sobreHeroeSuave, tam = 9.5f)
            }
        }
    }
}

@Composable
private fun CuentaAtras(cita: LocalDateTime, base: LocalDateTime, estilo: TextStyle, modifier: Modifier) {
    val t = ahoraVivo(base)
    val texto = hhmmss(Duration.between(t, cita))
    val s = LocalColores.current.superficies
    TextoRodante(listOf(texto.dropLast(3) to s.sobreHeroe, texto.takeLast(3) to Color(0xFFFF6A33)), estilo, modifier)
}

// ---------------------------------------------------------------- datos

@Composable
private fun Rejilla(
    a: Aviso, tecnico: Tecnico?, hoy: LocalDate, modifier: Modifier,
    alFecha: () -> Unit, alTecnico: () -> Unit, alEditar: () -> Unit, alHoras: (Double) -> Unit
) {
    val s = LocalColores.current.superficies
    val cuando = when {
        a.fecha.isEmpty() -> "Sin fecha"
        else -> Reglas.dia(a.fecha, hoy) + if (a.hora.isNotEmpty()) " · ${a.hora}" else ""
    }
    Column(modifier.fillMaxWidth().tarjeta(RoundedCornerShape(20.dp))) {
        FilaRejilla {
            Celda("Cuándo", Modifier.weight(1f), alFecha) { Valor(cuando) }
            Separador()
            Celda("Técnico", Modifier.weight(1f), alTecnico) {
                Avatar(tecnico, 24)
                Valor(tecnico?.nombre ?: "Sin asignar", Modifier.padding(start = 8.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(s.borde))
        FilaRejilla {
            Celda("Tipo", Modifier.weight(1f), alEditar) { Valor(a.tipoE.nombre) }
            Separador()
            Celda("Sistema", Modifier.weight(1f), alEditar) { Valor(a.sistemaE.nombre) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(s.borde))
        FilaRejilla {
            Celda("Prioridad", Modifier.weight(1f), alEditar) {
                Valor(a.prioridadE.nombre)
                Box(Modifier.padding(start = 8.dp)) { EtiquetaPrioridad(a.prioridadE) }
            }
            Separador()
            Celda("Horas", Modifier.weight(1f), null) {
                BotonRedondo(Icons.Rounded.Remove, "Quitar media hora", { alHoras(-0.5) }, tam = 30.dp, tamIcono = 16.dp, habilitado = a.horas > 0)
                Valor(Reglas.horas(a.horas), Modifier.widthIn(min = 52.dp).padding(horizontal = 6.dp), mono = true)
                BotonRedondo(Icons.Rounded.Add, "Añadir media hora", { alHoras(0.5) }, tam = 30.dp, tamIcono = 16.dp)
            }
        }
    }
}

@Composable
private fun FilaRejilla(contenido: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), content = contenido)

@Composable
private fun Separador() = Box(Modifier.width(1.dp).fillMaxHeight().background(LocalColores.current.superficies.borde))

@Composable
private fun Celda(nombre: String, modifier: Modifier, alPulsar: (() -> Unit)?, valor: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(
        modifier.then(if (alPulsar != null) Modifier.clickable(onClick = alPulsar) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Dato(nombre, tam = 9.5f)
        Row(Modifier.padding(top = 7.dp).height(30.dp), verticalAlignment = Alignment.CenterVertically, content = valor)
    }
}

@Composable
private fun Valor(t: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Text(
        t, modifier, maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = if (mono) androidx.compose.ui.text.style.TextAlign.Center else null,
        style = if (mono) EstiloMono.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        else TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    )
}

@Composable
private fun Accion(icono: ImageVector, nombre: String, habilitado: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonRedondo(icono, nombre, alPulsar, tam = 58.dp, tamIcono = 24.dp, habilitado = habilitado)
        Dato(nombre, tam = 9.5f, color = if (habilitado) Color.Unspecified else LocalColores.current.superficies.tenue)
    }
}

@Composable
private fun LineaDato(nombre: String, valor: String, alPulsar: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .then(if (alPulsar != null) Modifier.clickable(onClick = alPulsar) else Modifier)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dato(nombre, Modifier.width(88.dp), tam = 9.5f)
        Text(valor, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (alPulsar != null) Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp), tint = LocalColores.current.superficies.tenue)
    }
}

// ---------------------------------------------------------------- registro

private data class Registro(val momento: LocalDateTime?, val texto: String, val notaId: String? = null)

/** Lo que ha pasado con el aviso, del principio al final: alta, inicio, notas y cierre. */
private fun registro(a: Aviso): List<Registro> {
    val r = mutableListOf<Registro>()
    local(a.creado)?.let { r += Registro(it, if (a.origen == "correo") "Llegó por correo" else "Creado") }
    local(a.empezado)?.let { r += Registro(it, "Empezado") }
    a.notas.forEach { n -> r += Registro(local(n.ts), n.texto, n.id) }
    if (!a.abierto) local(a.cerrado)?.let { r += Registro(it, if (a.estadoE == Estado.CANCELADO) "Cancelado" else "Hecho") }
    return r.sortedBy { it.momento ?: LocalDateTime.MIN }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LineaRegistro(r: Registro, hoy: LocalDate, alBorrar: () -> Unit) {
    val s = LocalColores.current.superficies
    val cuando = r.momento?.let { if (it.toLocalDate() == hoy) it.toLocalTime().format(HHMM) else Reglas.dia(it.toLocalDate().toString(), hoy) } ?: ""
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .then(if (r.notaId != null) Modifier.combinedClickable(onClick = {}, onLongClick = alBorrar, onLongClickLabel = "Borrar nota") else Modifier)
            .padding(vertical = 5.dp)
    ) {
        Text(cuando, Modifier.width(64.dp).padding(top = 2.dp), style = EstiloMono.copy(fontSize = 11.sp), color = s.tenue, maxLines = 1)
        Text(
            r.texto, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = if (r.notaId != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------- deslizar para cerrar

/**
 * Barra de cristal con un botón naranja que se arrastra: abierto y sin
 * empezar, empieza; en curso, cierra. Al pasar el 80 % vibra y, al soltar,
 * se aplica. Lo cerrado se puede reabrir con un toque.
 */
@Composable
private fun BarraDeslizar(aviso: Aviso, cristal: HazeState, acciones: AccionesFicha, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val e = aviso.estadoE
    val forma = CircleShape
    Box(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth().height(68.dp)
            .shadow(18.dp, forma, ambientColor = Color.Transparent, spotColor = Color.Black.copy(alpha = if (s.oscuro) 0.5f else 0.18f))
            .clip(forma).hazeChild(cristal, estiloCristal()).border(1.dp, s.borde, forma)
    ) {
        if (!aviso.abierto) {
            Row(
                Modifier.fillMaxSize().clickable(role = Role.Button) { acciones.estado(Estado.PENDIENTE) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Replay, null, Modifier.size(18.dp))
                Dato("Reabrir", Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurface, tam = 11.5f)
            }
            return@Box
        }
        val destino = if (e == Estado.EN_CURSO) Estado.RESUELTO else Estado.EN_CURSO
        val texto = if (destino == Estado.RESUELTO) "Desliza para cerrar" else "Desliza para empezar"
        val vibrar = LocalHapticFeedback.current
        val ambito = rememberCoroutineScope()
        val x = remember(e) { Animatable(0f) }
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val boton = 56.dp
            val maxPx = with(LocalDensity.current) { (maxWidth - boton).toPx() }
            val p = if (maxPx <= 0f) 0f else x.value / maxPx
            LaunchedEffect(x, maxPx) {
                var listo = false
                snapshotFlow { x.value >= maxPx * 0.8f }.collect { ahora ->
                    if (ahora && !listo) vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                    listo = ahora
                }
            }
            /* El rastro naranja que deja el botón. */
            Box(
                Modifier.fillMaxHeight().width(boton + with(LocalDensity.current) { x.value.toDp() }).clip(forma)
                    .background(RedOrange.copy(alpha = 0.10f + 0.25f * p))
            )
            Row(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - (p * 1.6f).coerceAtMost(1f) }, verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(boton))
                Dato(texto, Modifier.weight(1f).padding(start = 8.dp), color = MaterialTheme.colorScheme.onSurface, tam = 11.5f)
                Text("›››", Modifier.padding(end = 16.dp), style = EstiloMono.copy(fontSize = 16.sp, letterSpacing = (-2).sp), color = s.tenue)
            }
            Box(
                Modifier
                    .offset { IntOffset(x.value.roundToInt(), 0) }
                    .size(boton)
                    .shadow(10.dp, forma, ambientColor = RedOrange, spotColor = RedOrange)
                    .clip(forma)
                    .background(RedOrange)
                    .draggable(
                        rememberDraggableState { d -> ambito.launch { x.snapTo((x.value + d).coerceIn(0f, maxPx)) } },
                        Orientation.Horizontal,
                        onDragStopped = {
                            if (x.value >= maxPx * 0.8f) {
                                x.animateTo(maxPx, Muelles.suave())
                                acciones.estado(destino)
                            } else x.animateTo(0f, Muelles.soltar())
                        }
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = texto
                        onClick(if (destino == Estado.RESUELTO) "Cerrar el aviso" else "Empezar el aviso") { acciones.estado(destino); true }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (p > 0.8f) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null,
                    Modifier.size(26.dp), tint = Color(0xFF141416)
                )
            }
        }
    }
}

// ---------------------------------------------------------------- fotos

/** En las capturas las miniaturas se cargan al momento; en la app, en segundo plano. */
val LocalMiniaturasAlMomento = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Las ya cargadas, para no volver a leerlas del disco al volver a la ficha. */
private val cacheMiniaturas = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(32)

/** Miniatura de una foto: tocar la abre, mantener pulsado pregunta si borrarla. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Miniatura(adj: Adjunto, alPulsar: () -> Unit, alMantener: () -> Unit) {
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
    val s = LocalColores.current.superficies
    Box(
        Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(s.item).border(1.dp, s.borde, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = alPulsar, onLongClick = alMantener),
        contentAlignment = Alignment.Center
    ) {
        val b = img
        if (b != null) {
            Image(b, adj.nombre, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else Icon(Icons.Rounded.Image, adj.nombre, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Hueco discontinuo para añadir una foto. */
@Composable
private fun Hueco(icono: ImageVector, nombre: String, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val i = remember { MutableInteractionSource() }
    Column(
        Modifier.size(96.dp).presionable(i, 0.94f).discontinua(s.tenue, 16.dp).clip(RoundedCornerShape(16.dp))
            .clickable(i, LocalIndication.current, role = Role.Button, onClick = alPulsar),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Dato(nombre, Modifier.padding(top = 6.dp), tam = 9f)
    }
}

private fun tamano(b: Long): String = when {
    b >= 1_048_576 -> String.format(java.util.Locale("es", "ES"), "%.1f MB", b / 1_048_576.0)
    b >= 1024 -> "${b / 1024} KB"
    else -> "$b bytes"
}

// ---------------------------------------------------------------- hoja de estados

@Composable
fun HojaEstado(aviso: Aviso, alElegir: (Estado) -> Unit) {
    Column(Modifier.padding(bottom = 24.dp)) {
        Dato("Estado del aviso", Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.onSurface, tam = 11.5f)
        Dato(
            listOf(aviso.ref, aviso.cliente.nombre).filter { it.isNotBlank() }.joinToString(" · "),
            Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp), tam = 9.5f
        )
        Estado.entries.forEach { e ->
            val sel = e == aviso.estadoE
            Row(
                Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .then(if (sel) Modifier.background(MaterialTheme.colorScheme.secondaryContainer) else Modifier)
                    .clickable { alElegir(e) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CirculoEstado(e)
                Column(Modifier.weight(1f)) {
                    val tinta = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    Text(e.nombre, style = MaterialTheme.typography.titleMedium, color = tinta)
                    Text(
                        e.explicacion, style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) tinta.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (sel) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}
