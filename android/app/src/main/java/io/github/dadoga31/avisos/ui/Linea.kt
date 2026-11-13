package io.github.dadoga31.avisos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/*
 * Lo que está en curso, la línea del día y el deslizar de las filas (3.6.1).
 *
 * Antes lo que estaba en curso iba dentro de la línea de tiempo, a escala:
 * con varios a la vez salían en columnas estrechas donde no se leía nada.
 * Ahora va arriba en tarjetas grandes, y la línea es un raíl con cada aviso a
 * todo el ancho, los huecos libres y la hora de ahora.
 */

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
private val TINTA = Color(0xFF141416)

private fun hora(min: Int) = "%02d:%02d".format(min / 60, min % 60)

private fun horaDe(iso: String): String? =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalTime().format(HHMM) }.getOrNull()

// ---------------------------------------------------------------- en curso

/** «● EN CURSO · 4» y una tarjeta grande por aviso; si hay varios, se pasan de lado. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeccionEnCurso(lista: List<Aviso>, tecnicos: Map<String, Tecnico>, ahora: LocalDateTime, acciones: AccionesAgenda, modifier: Modifier = Modifier) {
    val estado = rememberPagerState { lista.size }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PuntoVivo(RedOrange, vivo = true)
            Dato(
                "En curso" + if (lista.size > 1) " · ${lista.size}" else "", Modifier.padding(start = 10.dp).weight(1f),
                color = MaterialTheme.colorScheme.onSurface, peso = FontWeight.SemiBold
            )
            if (lista.size > 1) Dato("${estado.currentPage + 1}/${lista.size}  ← desliza")
        }
        if (lista.size == 1) {
            TarjetaEnCurso(lista[0], tecnicos[lista[0].asignadoA], ahora, acciones, Modifier.padding(horizontal = 16.dp))
        } else {
            HorizontalPager(
                estado, contentPadding = PaddingValues(start = 16.dp, end = 36.dp), pageSpacing = 10.dp,
                verticalAlignment = Alignment.Top, key = { lista[it].id }
            ) { n -> TarjetaEnCurso(lista[n], tecnicos[lista[n].asignadoA], ahora, acciones) }
        }
    }
}

/**
 * Tarjeta oscura con todo a la vista: desde cuándo, el cronómetro en grande,
 * qué es, dónde, lo previsto y los botones (llamar, ir, hecho y más).
 */
@Composable
fun TarjetaEnCurso(a: Aviso, tecnico: Tecnico?, ahora: LocalDateTime, acciones: AccionesAgenda, modifier: Modifier = Modifier) {
    val s = LocalColores.current.superficies
    val naranja = Color(0xFFFF6A33)
    val forma = RoundedCornerShape(24.dp)
    val i = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    val desde = runCatching { Instant.parse(a.empezado) }.getOrNull()
    Column(
        modifier
            .fillMaxWidth()
            .presionable(i, 0.985f)
            .clip(forma)
            .background(s.heroe)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(RedOrange.copy(alpha = 0.36f), Color.Transparent), center = Offset(size.width * 0.95f, 0f), radius = size.width * 0.6f),
                    radius = size.width * 0.6f, center = Offset(size.width * 0.95f, 0f)
                )
            }
            .border(1.dp, if (s.oscuro) s.borde else Color.Transparent, forma)
            .clickable(i, LocalIndication.current, onClickLabel = "Abrir el aviso") { acciones.abrir(a.id) }
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dato(
                listOfNotNull(horaDe(a.empezado)?.let { "Desde $it" }, a.ref.ifBlank { null }).joinToString(" · ").ifEmpty { "En curso" },
                Modifier.weight(1f), color = naranja, peso = FontWeight.SemiBold
            )
            Avatar(tecnico, 28)
        }
        Cronometro(
            desde, ahora, TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
            s.sobreHeroe, naranja, Modifier.padding(top = 8.dp)
        )
        Text(
            a.titulo.ifBlank { "(sin título)" }, Modifier.padding(top = 10.dp).compartido("titulo-${a.id}"),
            color = s.sobreHeroe, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp)
        )
        Text(
            listOf(a.cliente.nombre, a.cliente.direccion).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { "Sin cliente" },
            Modifier.padding(top = 4.dp), color = s.sobreHeroeSuave, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        if (a.duracion.isNotBlank() && desde != null) {
            val t = ahoraVivo(ahora)
            val previsto = Reglas.duracionMin(a)
            val llevado = Duration.between(desde, t.atZone(ZoneId.systemDefault()).toInstant()).toMinutes()
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(s.sobreHeroe.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((llevado.toFloat() / previsto).coerceIn(0f, 1f)).background(RedOrange))
                }
                Dato(
                    if (llevado > previsto) "+" + Reglas.minutos(llevado - previsto) else "Previsto " + Reglas.horas(previsto / 60.0),
                    Modifier.padding(start = 10.dp), color = if (llevado > previsto) naranja else s.sobreHeroeSuave, tam = 9.5f
                )
            }
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val boton = s.sobreHeroe.copy(alpha = 0.10f)
            BotonRedondo(
                Icons.Rounded.Call, "Llamar", { acciones.llamar(a.cliente.telefono) }, tam = 44.dp,
                habilitado = a.cliente.telefono.isNotBlank(), relleno = boton, tinta = s.sobreHeroe
            )
            BotonRedondo(
                Icons.Rounded.NearMe, "Ir", { acciones.irA(a.cliente.direccion) }, tam = 44.dp,
                habilitado = a.cliente.direccion.isNotBlank(), relleno = boton, tinta = s.sobreHeroe
            )
            Box {
                BotonRedondo(Icons.Rounded.MoreHoriz, "Más acciones", { menu = true }, tam = 44.dp, relleno = boton, tinta = s.sobreHeroe)
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Poner en espera") }, leadingIcon = { Icon(Icons.Rounded.PauseCircle, null) },
                        onClick = { menu = false; acciones.cambiarEstado(a, Estado.EN_ESPERA) }
                    )
                    DropdownMenuItem(
                        text = { Text("Cancelar y borrar") }, leadingIcon = { Icon(Icons.Rounded.Cancel, null) },
                        onClick = { menu = false; acciones.cancelar(a) }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BotonPildora("Hecho", Icons.Rounded.Check, { acciones.cambiarEstado(a, Estado.RESUELTO) }, alto = 44.dp)
        }
    }
}

// ---------------------------------------------------------------- línea del día

private sealed interface Paso {
    data class Cita(val b: Reglas.Bloque) : Paso
    data object Ahora : Paso
    data class Libre(val minutos: Int) : Paso
}

/** Huecos de 45 min o más entre una cita y la siguiente. */
private const val HUECO = 45

private fun pasos(bloques: List<Reglas.Bloque>, ahoraMin: Int?): List<Paso> {
    val r = mutableListOf<Paso>()
    var fin: Int? = null
    var ahoraPuesto = ahoraMin == null
    for (b in bloques.sortedBy { it.inicio }) {
        if (!ahoraPuesto && b.inicio > ahoraMin!!) {
            r += Paso.Ahora
            ahoraPuesto = true
            fin = maxOf(fin ?: ahoraMin, ahoraMin)
        }
        fin?.let { if (b.inicio - it >= HUECO) r += Paso.Libre(b.inicio - it) }
        r += Paso.Cita(b)
        fin = maxOf(fin ?: b.fin, b.fin)
    }
    if (!ahoraPuesto) r += Paso.Ahora
    return r
}

private val GUTTER = 62.dp
private val RAIL = 50.dp

/**
 * La línea del día como un raíl: la hora a la izquierda, un punto por aviso y
 * cada aviso a todo el ancho. Entre medias, los huecos libres y, hoy, la hora
 * de ahora en naranja.
 */
@Composable
fun LineaTiempo(
    bloques: List<Reglas.Bloque>,
    esHoy: Boolean,
    ahora: LocalDateTime,
    tecnicos: Map<String, Tecnico>,
    siguiente: String?,
    hoy: LocalDate,
    acciones: AccionesAgenda,
    modifier: Modifier = Modifier
) {
    val s = LocalColores.current.superficies
    val lista = pasos(bloques, if (esHoy) ahora.hour * 60 + ahora.minute else null)
    Column(modifier.fillMaxWidth()) {
        lista.forEachIndexed { n, p ->
            val primero = n == 0
            val ultimo = n == lista.lastIndex
            when (p) {
                is Paso.Ahora -> Row(
                    Modifier.fillMaxWidth().height(30.dp).drawBehind {
                        val x = RAIL.toPx()
                        val y = size.height / 2
                        if (!primero) drawLine(s.borde, Offset(x, 0f), Offset(x, y), 2.dp.toPx())
                        if (!ultimo) drawLine(s.borde, Offset(x, y), Offset(x, size.height), 2.dp.toPx())
                        drawRect(RedOrange.copy(alpha = 0.22f), Offset(x, y - 3.dp.toPx()), Size(size.width - x, 6.dp.toPx()))
                        drawRect(RedOrange, Offset(x, y - 1.dp.toPx()), Size(size.width - x, 2.dp.toPx()))
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        ahora.format(HHMM), Modifier.padding(start = 8.dp).clip(RoundedCornerShape(6.dp)).background(RedOrange).padding(horizontal = 6.dp, vertical = 2.dp),
                        style = EstiloMono.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold), color = TINTA
                    )
                }
                is Paso.Libre -> Row(
                    Modifier.fillMaxWidth().height(34.dp).drawBehind {
                        val x = RAIL.toPx()
                        drawLine(
                            s.tenue.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(GUTTER))
                    Dato("Libre · " + Reglas.minutos(p.minutos.toLong()), tam = 9.5f, color = s.tenue)
                }
                is Paso.Cita -> FilaRail(
                    p.b, primero, ultimo, tecnicos[p.b.aviso.asignadoA], p.b.aviso.id == siguiente,
                    Reglas.vencido(p.b.aviso, hoy), ahora, acciones
                )
            }
        }
    }
}

@Composable
private fun FilaRail(
    b: Reglas.Bloque, primero: Boolean, ultimo: Boolean, tecnico: Tecnico?, siguiente: Boolean,
    vencido: Boolean, ahora: LocalDateTime, acciones: AccionesAgenda
) {
    val s = LocalColores.current.superficies
    val a = b.aviso
    val punto = when {
        b.hecho -> s.ok
        else -> colorBarra(a, vencido)
    }
    val nodo = 22.dp
    Row(
        Modifier.fillMaxWidth().padding(end = 16.dp).drawBehind {
            val x = RAIL.toPx()
            val y = nodo.toPx()
            if (!primero) drawLine(s.borde, Offset(x, 0f), Offset(x, y), 2.dp.toPx())
            if (!ultimo) drawLine(s.borde, Offset(x, y), Offset(x, size.height), 2.dp.toPx())
            drawCircle(s.pagina, radius = 8.dp.toPx(), center = Offset(x, y))
            if (b.hecho || a.estadoE == Estado.PROGRAMADO || a.estadoE == Estado.EN_CURSO || vencido) {
                drawCircle(punto, radius = 5.dp.toPx(), center = Offset(x, y))
            } else {
                drawCircle(punto, radius = 4.5.dp.toPx(), center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
    ) {
        Column(Modifier.width(GUTTER - 20.dp).padding(top = 14.dp), horizontalAlignment = Alignment.End) {
            Text(hora(b.inicio), style = EstiloMono.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = if (b.hecho) s.tenue else MaterialTheme.colorScheme.onSurface)
            if (!b.hecho) Text(hora(b.fin), style = EstiloMono.copy(fontSize = 10.sp), color = s.tenue)
        }
        Spacer(Modifier.width(20.dp))
        val forma = RoundedCornerShape(18.dp)
        Box(Modifier.weight(1f).padding(bottom = 10.dp)) {
            Deslizable(
                a, forma,
                alHecho = { acciones.cambiarEstado(a, Estado.RESUELTO) },
                alDerecha = { acciones.cambiarEstado(a, if (a.estadoE == Estado.EN_CURSO) Estado.EN_ESPERA else Estado.EN_CURSO) },
                alCancelar = { acciones.cancelar(a) }
            ) {
                TarjetaLinea(b, tecnico, siguiente, vencido, ahora, acciones, forma)
            }
        }
    }
}

@Composable
private fun TarjetaLinea(
    b: Reglas.Bloque, tecnico: Tecnico?, siguiente: Boolean, vencido: Boolean, ahora: LocalDateTime,
    acciones: AccionesAgenda, forma: Shape
) {
    val a = b.aviso
    val s = LocalColores.current.superficies
    val i = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).presionable(i, 0.985f).tarjeta(forma)
            .clickable(i, LocalIndication.current) { acciones.abrir(a.id) }
            .then(if (b.hecho) Modifier.alpha(0.72f) else Modifier)
    ) {
        if (!b.hecho) Box(Modifier.width(4.dp).fillMaxHeight().background(colorBarra(a, vencido)))
        if (b.hecho) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, "Hecho", Modifier.size(20.dp), tint = s.ok)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(a.titulo, Modifier.compartido("titulo-${a.id}"), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                    Dato(listOfNotNull(a.cliente.nombre.ifBlank { null }, horaDe(a.cerrado)?.let { "Hecho $it" } ?: "Hecho").joinToString(" · "), tam = 9.5f)
                }
            }
            return@Row
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dato(
                    when {
                        vencido -> "Vencido · " + hora(b.inicio)
                        a.estadoE == Estado.EN_CURSO -> "En curso"
                        a.estadoE == Estado.EN_ESPERA -> "En espera"
                        else -> hora(b.inicio) + " — " + hora(b.fin)
                    },
                    color = if (vencido) s.acentoTexto else MaterialTheme.colorScheme.onSurface, peso = FontWeight.SemiBold
                )
                EtiquetaPrioridad(a.prioridadE)
                Spacer(Modifier.weight(1f))
                Avatar(tecnico, 26)
            }
            Text(
                a.titulo.ifBlank { "(sin título)" }, Modifier.padding(top = 6.dp).compartido("titulo-${a.id}"),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
            )
            Text(
                listOf(a.cliente.nombre, a.cliente.direccion).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { a.sistemaE.nombre },
                Modifier.padding(top = 3.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (siguiente) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val falta = (b.inicio - (ahora.hour * 60 + ahora.minute)).toLong()
                    Dato(if (falta > 0) "Siguiente · en " + Reglas.minutos(falta) else "Siguiente", Modifier.weight(1f), color = s.acentoTexto, tam = 9.5f, peso = FontWeight.SemiBold)
                    if (a.cliente.telefono.isNotBlank()) BotonRedondo(Icons.Rounded.Call, "Llamar", { acciones.llamar(a.cliente.telefono) }, tam = 38.dp, tamIcono = 18.dp)
                    if (a.cliente.direccion.isNotBlank()) BotonRedondo(Icons.Rounded.NearMe, "Ir", { acciones.irA(a.cliente.direccion) }, tam = 38.dp, tamIcono = 18.dp, relleno = RedOrange, tinta = TINTA)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- deslizar

/**
 * Deslizar una fila. A la izquierda descubre «Cancelar» y «Hecho»; si se
 * desliza del todo, es hecho sin más (criterio 2: cerrar, un gesto). A la
 * derecha, en curso (o en espera si ya lo está). Al pasar cada umbral el
 * móvil da un toque. Con TalkBack, las tres cosas son acciones de la fila.
 */
@Composable
fun Deslizable(
    aviso: Aviso,
    forma: Shape,
    modifier: Modifier = Modifier,
    alHecho: () -> Unit,
    alDerecha: () -> Unit,
    alCancelar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    val hecho by rememberUpdatedState(alHecho)
    val derecha by rememberUpdatedState(alDerecha)
    val cancelar by rememberUpdatedState(alCancelar)
    val colores = LocalColores.current
    val s = colores.superficies
    val habilitado = aviso.abierto
    val enCurso = aviso.estadoE == Estado.EN_CURSO
    val x = remember(aviso.id) { Animatable(0f) }
    var ancho by remember { mutableIntStateOf(0) }
    var arrastrando by remember { mutableStateOf(false) }
    val densidad = LocalDensity.current
    val botones = with(densidad) { 180.dp.toPx() }
    val lleno = maxOf(botones + with(densidad) { 40.dp.toPx() }, ancho * 0.62f)
    val umbralDerecha = ancho * 0.3f
    val vibrar = LocalHapticFeedback.current
    val ambito = rememberCoroutineScope()
    fun cerrar() { ambito.launch { x.animateTo(0f, Muelles.soltar()) } }

    LaunchedEffect(ancho) {
        if (ancho == 0) return@LaunchedEffect
        snapshotFlow { x.value <= -lleno || x.value >= umbralDerecha }.collect { if (it) vibrar.performHapticFeedback(HapticFeedbackType.LongPress) }
    }

    Box(
        modifier
            .clip(forma)
            .onSizeChanged { ancho = it.width }
            .then(
                if (habilitado) Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Hecho") { hecho(); true },
                        CustomAccessibilityAction(if (enCurso) "En espera" else "Empezar") { derecha(); true },
                        CustomAccessibilityAction("Cancelar y borrar") { cancelar(); true }
                    )
                } else Modifier
            )
    ) {
        val v = x.value
        if (v > 0f) {
            val e = if (enCurso) Estado.EN_ESPERA else Estado.EN_CURSO
            val listo = v >= umbralDerecha
            Row(
                Modifier.matchParentSize().background(colores.de(e).color.copy(alpha = if (listo) 1f else 0.55f)).padding(start = 22.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(if (enCurso) Icons.Rounded.PauseCircle else Icons.Rounded.Construction, null, tint = TINTA)
                Text(if (enCurso) "EN ESPERA" else "EN CURSO", color = TINTA, style = EstiloEtiqueta)
            }
        } else if (v < 0f) {
            val abierto = -v
            /* Hasta los botones, mitad y mitad; más allá, Hecho se come a Cancelar. */
            val anchoCancelar = if (abierto <= botones) abierto / 2 else (botones / 2) * (1f - ((abierto - botones) / (lleno - botones)).coerceIn(0f, 1f))
            val anchoHecho = abierto - anchoCancelar
            Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier.fillMaxHeight().width(with(densidad) { anchoCancelar.toDp() })
                        .background(if (s.oscuro) Color(0xFF5A5A61) else Color(0xFF6B6B72))
                        .clickable(role = Role.Button, onClickLabel = "Cancelar y borrar") { cancelar(); cerrar() }
                        .semantics { contentDescription = "Cancelar aviso" },
                    contentAlignment = Alignment.Center
                ) {
                    if (anchoCancelar > with(densidad) { 56.dp.toPx() }) BotonFondo(Icons.Rounded.Cancel, "CANCELAR", Color.White)
                }
                Box(
                    Modifier.fillMaxHeight().width(with(densidad) { anchoHecho.toDp() }).background(s.ok)
                        .clickable(role = Role.Button, onClickLabel = "Dar por hecho") { hecho(); cerrar() }
                        .semantics { contentDescription = "Dar por hecho" },
                    contentAlignment = if (abierto > botones) Alignment.CenterEnd else Alignment.Center
                ) {
                    if (anchoHecho > with(densidad) { 56.dp.toPx() }) Box(Modifier.padding(end = if (abierto > botones) 26.dp else 0.dp)) {
                        BotonFondo(Icons.Rounded.CheckCircle, "HECHO", TINTA)
                    }
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .draggable(
                    rememberDraggableState { d ->
                        ambito.launch { x.snapTo((x.value + d).coerceIn(-ancho.toFloat(), ancho.toFloat())) }
                    },
                    Orientation.Horizontal,
                    enabled = habilitado,
                    onDragStarted = { arrastrando = true },
                    onDragStopped = {
                        arrastrando = false
                        val fin = x.value
                        when {
                            fin >= umbralDerecha -> { derecha(); x.animateTo(0f, Muelles.soltar()) }
                            fin > 0f -> x.animateTo(0f, Muelles.soltar())
                            fin <= -lleno -> { hecho(); x.animateTo(0f, Muelles.suave()) }
                            fin <= -botones / 2 -> x.animateTo(-botones, Muelles.soltar())
                            else -> x.animateTo(0f, Muelles.soltar())
                        }
                    }
                )
        ) {
            contenido()
            /* Abierta, tocar la fila la cierra (en vez de abrir el aviso). */
            if (x.value < -1f && !arrastrando) {
                Box(Modifier.matchParentSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { cerrar() })
            }
        }
    }
}

@Composable
private fun BotonFondo(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, tinta: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icono, null, tint = tinta, modifier = Modifier.size(22.dp))
        Text(texto, color = tinta, style = EstiloEtiqueta.copy(fontSize = 10.sp))
    }
}
