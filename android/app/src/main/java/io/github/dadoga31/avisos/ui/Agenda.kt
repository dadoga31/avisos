package io.github.dadoga31.avisos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as EstiloFecha
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Lo que la Agenda puede pedir hacer. */
class AccionesAgenda(
    val abrir: (String) -> Unit = {},
    val nuevo: () -> Unit = {},
    val cambiarEstado: (Aviso, Estado) -> Unit = { _, _ -> },
    val llamar: (String) -> Unit = {},
    val irA: (String) -> Unit = {},
    val ejemplo: () -> Unit = {},
    val ocultarPista: () -> Unit = {},
    val correo: () -> Unit = {},
    val actualizar: () -> Unit = {}
)

private val ES = Locale("es", "ES")

/** Alto de una hora en la línea de tiempo. */
private val HORA = 72.dp
/** Margen izquierdo de la línea (donde van las horas). */
private val MARGEN = 62.dp

/**
 * La Agenda es la línea de tiempo de un día: fecha grande con el reloj, lo
 * hecho en un anillo, cifras, la semana y los avisos colocados en su hora.
 * Debajo, lo que no tiene hora ni fecha (para que todo aviso abierto salga
 * en algún sitio) y lo próximo. Buscar o tocar una cifra la convierte en
 * lista de resultados.
 *
 * [correo]: null si no hay buzón; true si escucha bien; false si falla.
 */
@Composable
fun AgendaPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ajustes: Ajustes,
    ahora: LocalDateTime,
    acciones: AccionesAgenda,
    filtroInicial: Reglas.Filtro = Reglas.Filtro(),
    correo: Boolean? = null,
    diaInicial: LocalDate? = null,
    actualizacion: io.github.dadoga31.avisos.EstadoActualizacion = io.github.dadoga31.avisos.EstadoActualizacion.Nada
) {
    val hoy = ahora.toLocalDate()
    var dia by remember { mutableStateOf(diaInicial ?: hoy) }
    var filtro by remember { mutableStateOf(filtroInicial) }
    var buscando by remember { mutableStateOf(filtroInicial.activo) }
    val porId = remember(tecnicos) { tecnicos.associateBy { it.id } }
    val resumen = remember(avisos, hoy) { Reglas.resumen(avisos, hoy) }
    val s = LocalColores.current.superficies

    /* Al cambiar de día (medianoche), la Agenda vuelve a hoy. */
    LaunchedEffect(hoy) { if (diaInicial == null) dia = hoy }
    /* Atrás: primero cierra la búsqueda, luego vuelve a hoy; después ya sale. */
    BackHandler(buscando || filtro.activo) { buscando = false; filtro = Reglas.Filtro() }
    BackHandler(!buscando && !filtro.activo && dia != hoy) { dia = hoy }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = EspacioMenu)) {
        if (buscando || filtro.activo) {
            item(key = "buscar") {
                Column {
                    BarraMarca("Avisos", "buscar") {
                        BotonRedondo(Icons.Rounded.Close, "Cerrar búsqueda", { buscando = false; filtro = Reglas.Filtro() })
                    }
                    val foco = remember { FocusRequester() }
                    Buscador(filtro.texto, { filtro = filtro.copy(texto = it) }, Modifier.padding(horizontal = 16.dp), foco = foco)
                    LaunchedEffect(Unit) { if (!filtroInicial.activo) runCatching { foco.requestFocus() } }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Pastilla(conCuenta("Vencidos", resumen.vencidos), filtro.vencidos) { filtro = filtro.copy(vencidos = !filtro.vencidos) }
                        Pastilla(conCuenta("Urgentes", resumen.urgentes), filtro.urgentes) { filtro = filtro.copy(urgentes = !filtro.urgentes) }
                        Pastilla(conCuenta("Sin asignar", resumen.sinAsignar), filtro.sinAsignar) { filtro = filtro.copy(sinAsignar = !filtro.sinAsignar) }
                        Pastilla(conCuenta("Esta semana", resumen.semana), filtro.semana) { filtro = filtro.copy(semana = !filtro.semana) }
                    }
                }
            }
            if (filtro.activo) {
                val res = Reglas.filtrar(avisos, tecnicos, filtro, hoy)
                item(key = "cab-res") {
                    CabeceraDato(
                        if (res.isEmpty()) "Nada coincide" else if (res.size == 1) "1 resultado" else "${res.size} resultados",
                        extra = if (filtro.texto.isNotBlank()) "abiertos y cerrados" else ""
                    )
                }
                itemsIndexed(res, key = { _, a -> "r-" + a.id }) { i, a ->
                    FilaAgenda(a, porId[a.asignadoA], hoy, acciones, Modifier.animateItem().entrada(i))
                }
            }
            return@LazyColumn
        }

        item(key = "barra") {
            BarraMarca("Avisos", if (dia == hoy) "hoy" else diaCorto(dia, hoy)) {
                if (correo != null) IndicadorCorreo(correo, acciones.correo)
                BotonRedondo(Icons.Rounded.Search, "Buscar", { buscando = true })
            }
        }
        if (actualizacion !is io.github.dadoga31.avisos.EstadoActualizacion.Nada) {
            item(key = "actualizar") {
                FranjaActualizacion(actualizacion, acciones.actualizar, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp).entrada(0))
            }
        }
        item(key = "fecha") {
            CabeceraFecha(dia, hoy, ahora, Reglas.progresoDia(avisos, dia), Modifier.entrada(0)) { dia = hoy }
        }

        if (avisos.isEmpty()) {
            item(key = "vacio") { Vacio(acciones, Modifier.entrada(1)) }
            return@LazyColumn
        }

        item(key = "kpi") {
            FilaKpi(
                listOf(
                    Kpi(dosCifras(resumen.hoy), "Por hacer", activo = false) { dia = hoy },
                    Kpi(dosCifras(resumen.vencidos), if (resumen.vencidos == 1) "Vencido" else "Vencidos", alerta = resumen.vencidos > 0) {
                        filtro = Reglas.Filtro(vencidos = true)
                    },
                    Kpi(dosCifras(resumen.sinAsignar), "Sin asignar") { filtro = Reglas.Filtro(sinAsignar = true) },
                    Kpi(dosCifras(resumen.semana), "Semana") { filtro = Reglas.Filtro(semana = true) }
                ),
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).entrada(1)
            )
        }
        item(key = "semana") { TiraSemana(dia, hoy, avisos, Modifier.padding(top = 10.dp).entrada(2)) { dia = it } }

        val vencidos = Reglas.grupos(avisos, hoy).firstOrNull { it.clave == "vencidos" }?.avisos.orEmpty()
        if (vencidos.isNotEmpty() && dia == hoy) {
            item(key = "vencidos") {
                BarraVencidos(vencidos, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).entrada(3)) {
                    if (vencidos.size == 1) acciones.abrir(vencidos.first().id) else filtro = Reglas.Filtro(vencidos = true)
                }
            }
        }
        if (ajustes.pistaGestos && avisos.any { it.abierto }) {
            item(key = "pista") { Pista(acciones.ocultarPista, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp).entrada(3)) }
        }

        val bloques = Reglas.linea(avisos, dia, if (dia == hoy) ahora.toLocalTime() else null)
        val siguiente = if (dia == hoy) Reglas.siguiente(avisos, hoy, ahora.toLocalTime())?.aviso?.id else null
        item(key = "linea-$dia") {
            if (bloques.isEmpty()) {
                SinCitas(dia == hoy, acciones.nuevo, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).entrada(4))
            } else {
                LineaTiempo(bloques, dia == hoy, ahora, porId, siguiente, hoy, acciones, Modifier.padding(top = 16.dp).entrada(4))
            }
        }

        val bandejas = listOf(
            Triple("sinhora", "Sin hora", Reglas.sinHora(avisos, dia)),
            Triple("sinfecha", "Sin fecha", Reglas.sinFecha(avisos)),
            Triple("proximos", if (dia == hoy) "Próximos" else "Después", Reglas.proximos(avisos, dia))
        )
        var orden = 5
        bandejas.forEach { (clave, titulo, lista) ->
            if (lista.isEmpty()) return@forEach
            val base = orden
            item(key = "cab-$clave") { CabeceraDato(titulo, Modifier.entrada(base), extra = "${lista.size}") }
            itemsIndexed(lista, key = { _, a -> "$clave-" + a.id }) { i, a ->
                FilaAgenda(a, porId[a.asignadoA], hoy, acciones, Modifier.animateItem().entrada(base + 1 + i))
            }
            orden += 1 + lista.size
        }
        if (avisos.none { it.abierto }) {
            item(key = "aldia") {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Dato("Todo al día", color = s.ok, tam = 11f)
                    Text("No queda ningún aviso abierto.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun conCuenta(t: String, n: Int) = if (n > 0) "$t  $n" else t

private fun dosCifras(n: Int) = n.toString().padStart(2, '0')

/** «MAÑANA», «AYER», «VIE 13». */
private fun diaCorto(d: LocalDate, hoy: LocalDate) = when (d) {
    hoy.plusDays(1) -> "mañana"
    hoy.minusDays(1) -> "ayer"
    else -> d.dayOfWeek.getDisplayName(EstiloFecha.SHORT, ES).replace(".", "") + " " + d.dayOfMonth
}

@Composable
private fun IndicadorCorreo(bien: Boolean, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    Row(
        Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Correo", onClick = alPulsar).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PuntoVivo(if (bien) s.ok else RedOrange, vivo = bien)
        Dato("Correo", color = if (bien) MaterialTheme.colorScheme.onSurfaceVariant else s.acentoTexto)
    }
}

/** El número del día en grande, el día y el mes, el reloj y el anillo de lo hecho. */
@Composable
private fun CabeceraFecha(dia: LocalDate, hoy: LocalDate, ahora: LocalDateTime, p: Reglas.Progreso, modifier: Modifier, alVolverAHoy: () -> Unit) {
    val s = LocalColores.current.superficies
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${dia.dayOfMonth}",
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = dia != hoy, onClickLabel = "Volver a hoy", onClick = alVolverAHoy),
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 96.sp, lineHeight = 90.sp, letterSpacing = (-5).sp)
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Dato(dia.dayOfWeek.getDisplayName(EstiloFecha.FULL, ES), color = MaterialTheme.colorScheme.onSurface, tam = 11.5f)
            Dato(dia.month.getDisplayName(EstiloFecha.SHORT, ES).replace(".", "") + " " + dia.year, tam = 11.5f)
            Box(Modifier.padding(top = 6.dp)) {
                if (dia == hoy) {
                    val t = ahoraVivo(ahora)
                    TextoRodante(
                        listOf(t.format(DateTimeFormatter.ofPattern("HH:mm")) to MaterialTheme.colorScheme.onSurface, t.format(DateTimeFormatter.ofPattern(":ss")) to s.tenue),
                        EstiloMono.copy(fontSize = 14.sp, lineHeight = 18.sp)
                    )
                } else {
                    val n = ChronoUnit.DAYS.between(hoy, dia)
                    Dato(
                        when {
                            n == 1L -> "Mañana"; n == -1L -> "Ayer"
                            n > 0 -> "En $n días"; else -> "Hace ${-n} días"
                        } + "  · hoy ›",
                        Modifier.clickable(onClick = alVolverAHoy), color = s.acentoTexto, tam = 10.5f
                    )
                }
            }
        }
        Anillo(p.hechos, p.total)
    }
}

/**
 * La semana del día elegido; se desliza a otras semanas. Cada día lleva un
 * punto por aviso abierto (hasta tres), naranjas si se quedaron sin hacer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TiraSemana(dia: LocalDate, hoy: LocalDate, avisos: List<Aviso>, modifier: Modifier, alElegir: (LocalDate) -> Unit) {
    val lunesHoy = hoy.with(DayOfWeek.MONDAY)
    val centro = 520
    fun paginaDe(d: LocalDate) = centro + ChronoUnit.WEEKS.between(lunesHoy, d.with(DayOfWeek.MONDAY)).toInt()
    val estado = rememberPagerState(initialPage = paginaDe(dia)) { centro * 2 + 1 }
    LaunchedEffect(dia) {
        val p = paginaDe(dia)
        if (estado.currentPage != p) estado.animateScrollToPage(p)
    }
    val porDia = remember(avisos) { avisos.filter { it.abierto && it.fecha.isNotEmpty() }.groupingBy { it.fecha }.eachCount() }
    HorizontalPager(estado, modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp), pageSpacing = 12.dp) { p ->
        val lunes = lunesHoy.plusWeeks((p - centro).toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (n in 0L..6L) {
                val d = lunes.plusDays(n)
                CeldaDia(d, d == dia, d == hoy, porDia[d.toString()] ?: 0, d < hoy, Modifier.weight(1f)) { alElegir(d) }
            }
        }
    }
}

@Composable
private fun CeldaDia(d: LocalDate, elegido: Boolean, esHoy: Boolean, abiertos: Int, pasado: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val fondo by animateColorAsState(if (elegido) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, Muelles.suave(), label = "dia")
    val tinta by animateColorAsState(if (elegido) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, Muelles.suave(), label = "tinta")
    val i = remember { MutableInteractionSource() }
    Column(
        modifier
            .height(64.dp)
            .presionable(i, 0.92f)
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .clickable(i, LocalIndication.current, role = Role.Tab, onClickLabel = d.toString(), onClick = alPulsar),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Dato(
            d.dayOfWeek.getDisplayName(EstiloFecha.NARROW, ES).let { if (d.dayOfWeek == DayOfWeek.WEDNESDAY) "X" else it }, tam = 10f,
            color = if (esHoy && !elegido) s.acentoTexto else tinta.copy(alpha = 0.7f)
        )
        Text(
            "${d.dayOfMonth}", Modifier.padding(top = 2.dp), color = tinta,
            style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp)
        )
        Row(Modifier.height(8.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(minOf(abiertos, 3)) {
                Box(
                    Modifier.size(4.dp).clip(CircleShape).background(
                        when {
                            pasado -> RedOrange
                            elegido -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> s.tenue
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun BarraVencidos(vencidos: List<Aviso>, modifier: Modifier, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val forma = RoundedCornerShape(14.dp)
    val i = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .presionable(i, 0.98f)
            .clip(forma)
            .background(s.tinte)
            .border(1.dp, RedOrange.copy(alpha = 0.45f), forma)
            .clickable(i, LocalIndication.current, onClick = alPulsar)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp), tint = RedOrange)
        Dato(if (vencidos.size == 1) "Vencido" else "Vencidos · ${vencidos.size}", color = s.acentoTexto, peso = FontWeight.SemiBold)
        Text(
            vencidos.first().titulo, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium)
        )
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Línea discontinua alrededor (huecos, pistas, cosas por hacer). */
fun Modifier.discontinua(color: Color, radio: Dp): Modifier = drawBehind {
    val g = 1.dp.toPx()
    drawRoundRect(
        color, Offset(g / 2, g / 2), Size(size.width - g, size.height - g), CornerRadius(radio.toPx()),
        style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
    )
}

@Composable
private fun Pista(alCerrar: () -> Unit, modifier: Modifier) {
    val s = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().discontinua(s.tenue, 14.dp).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.SwipeLeft, null, Modifier.size(18.dp), tint = RedOrange)
        Text(
            "Desliza un aviso: ← hecho · → en curso", Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = alCerrar) { Icon(Icons.Rounded.Close, "Entendido", Modifier.size(18.dp)) }
    }
}

@Composable
private fun SinCitas(esHoy: Boolean, alNuevo: () -> Unit, modifier: Modifier) {
    val s = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().discontinua(s.tenue, 18.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = alNuevo).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Dato(if (esHoy) "Nada con hora hoy" else "Nada con hora este día", color = MaterialTheme.colorScheme.onSurface)
            Text("Toca para crear un aviso", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = RedOrange)
    }
}

// ---------------------------------------------------------------- línea de tiempo

/**
 * Las horas en una rejilla discontinua y cada aviso en su sitio, con su
 * duración. Los que se pisan van en columnas. Hoy, una línea naranja marca
 * la hora; pasa por detrás de las tarjetas y su hora va en el margen.
 */
@Composable
private fun LineaTiempo(
    bloques: List<Reglas.Bloque>,
    esHoy: Boolean,
    ahora: LocalDateTime,
    tecnicos: Map<String, Tecnico>,
    siguiente: String?,
    hoy: LocalDate,
    acciones: AccionesAgenda,
    modifier: Modifier
) {
    val s = LocalColores.current.superficies
    val minimo = { b: Reglas.Bloque -> if (b.hecho) 45 else 60 }
    val carriles = remember(bloques) { Reglas.carriles(bloques, minimo) }
    val ahoraMin = ahora.hour * 60 + ahora.minute
    val desde = minOf(bloques.minOf { it.inicio }, if (esHoy) ahoraMin else Int.MAX_VALUE) / 60
    val hasta = minOf(24, (maxOf(bloques.maxOf { maxOf(it.fin, it.inicio + minimo(it)) }, if (esHoy) ahoraMin else 0) + 59) / 60 + 1)
    val alto = HORA * (hasta - desde) + 8.dp
    fun y(min: Int): Dp = HORA * ((min - desde * 60) / 60f)

    BoxWithConstraints(modifier.fillMaxWidth().height(alto)) {
        val ancho = maxWidth - MARGEN - 16.dp
        /* Rejilla de horas. */
        for (h in desde..hasta) {
            Row(Modifier.offset(y = y(h * 60) - 7.dp).fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%02d:00".format(h), Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = EstiloMono.copy(fontSize = 10.5.sp, lineHeight = 14.sp), color = s.tenue
                )
                Box(
                    Modifier.padding(start = 10.dp).weight(1f).height(1.dp).drawBehind {
                        drawLine(s.borde, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                    }
                )
            }
        }
        /* La hora de ahora, por detrás de las tarjetas. */
        if (esHoy && ahoraMin in desde * 60..hasta * 60) {
            Box(
                Modifier.offset(x = MARGEN - 10.dp, y = y(ahoraMin) - 1.dp).fillMaxWidth().height(2.dp).drawBehind {
                    drawRect(RedOrange.copy(alpha = 0.25f), Offset(0f, -3.dp.toPx()), Size(size.width, size.height + 6.dp.toPx()))
                    drawRect(RedOrange)
                }
            )
        }
        carriles.forEach { c ->
            val b = c.bloque
            val top = y(b.inicio) + 4.dp
            val h = HORA * (maxOf(b.fin - b.inicio, minimo(b)) / 60f) - 6.dp
            val col = (ancho - 6.dp * (c.columnas - 1)) / c.columnas
            BloqueLinea(
                b, tecnicos[b.aviso.asignadoA], h, b.aviso.id == siguiente, Reglas.vencido(b.aviso, hoy), ahora, acciones,
                Modifier.offset(x = MARGEN + (col + 6.dp) * c.columna, y = top).width(col).height(h)
            )
        }
        if (esHoy && ahoraMin in desde * 60..hasta * 60) {
            Text(
                ahora.format(DateTimeFormatter.ofPattern("HH:mm")),
                Modifier.offset(x = 6.dp, y = y(ahoraMin) - 9.dp).clip(RoundedCornerShape(6.dp)).background(RedOrange).padding(horizontal = 6.dp, vertical = 2.dp),
                style = EstiloMono.copy(fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFF141416)
            )
        }
    }
}

private fun hora(min: Int) = "%02d:%02d".format(min / 60, min % 60)

/** Un aviso en la línea de tiempo. Se desliza como las filas: ← hecho, → en curso. */
@Composable
private fun BloqueLinea(
    b: Reglas.Bloque,
    tecnico: Tecnico?,
    alto: Dp,
    siguiente: Boolean,
    vencido: Boolean,
    ahora: LocalDateTime,
    acciones: AccionesAgenda,
    modifier: Modifier
) {
    val a = b.aviso
    val s = LocalColores.current.superficies
    val forma = RoundedCornerShape(if (alto < 64.dp) 16.dp else 20.dp)
    val compacto = alto < 100.dp
    val enCurso = a.estadoE == Estado.EN_CURSO
    val i = remember { MutableInteractionSource() }
    FilaDeslizable(
        a, forma, modifier,
        alHecho = { acciones.cambiarEstado(a, Estado.RESUELTO) },
        alDerecha = { acciones.cambiarEstado(a, if (enCurso) Estado.EN_ESPERA else Estado.EN_CURSO) }
    ) {
        val barra = colorBarra(a, vencido)
        Box(
            Modifier
                .fillMaxSize()
                .presionable(i, 0.98f)
                .clip(forma)
                .background(
                    if (enCurso) Brush.verticalGradient(listOf(lerpColor(s.item, RedOrange, if (s.oscuro) 0.16f else 0.08f), s.item))
                    else SolidColor(s.itemOpaco())
                )
                .border(if (enCurso) 1.5.dp else 1.dp, if (enCurso) RedOrange else s.borde, forma)
                .clickable(i, LocalIndication.current, onClick = { acciones.abrir(a.id) })
                .alpha(if (b.hecho) 0.75f else 1f)
        ) {
            if (!b.hecho) Box(Modifier.width(4.dp).fillMaxHeight().background(barra))
            when {
                b.hecho -> ContenidoHecho(a, Modifier.padding(horizontal = 14.dp).align(Alignment.CenterStart))
                compacto -> ContenidoCompacto(b, tecnico, siguiente, vencido, ahora, acciones, Modifier.padding(start = 16.dp, end = 10.dp).align(Alignment.CenterStart))
                else -> ContenidoAmplio(b, tecnico, siguiente, vencido, ahora, acciones, Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp))
            }
        }
    }
}

/** Fondo opaco de las tarjetas que se deslizan (que no se transparente lo de detrás). */
private fun Superficies.itemOpaco() = item

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

@Composable
private fun ContenidoHecho(a: Aviso, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val cierre = runCatching { Instant.parse(a.cerrado).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("")
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.CheckCircle, "Hecho", Modifier.size(20.dp), tint = s.ok)
        Column(Modifier.weight(1f)) {
            Text(
                a.titulo, Modifier.compartido("titulo-${a.id}"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
            )
            Dato(listOf(a.cliente.nombre, if (cierre.isNotEmpty()) "Hecho $cierre" else "Hecho").filter { it.isNotBlank() }.joinToString(" · "), tam = 9.5f)
        }
    }
}

@Composable
private fun Encabezado(b: Reglas.Bloque, vencido: Boolean, ahora: LocalDateTime) {
    val a = b.aviso
    val s = LocalColores.current.superficies
    if (a.estadoE == Estado.EN_CURSO) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuntoVivo(RedOrange, vivo = true)
            Dato("En curso" + if (a.hora.isNotEmpty()) " · ${a.hora}" else "", color = s.acentoTexto, peso = FontWeight.SemiBold)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dato(
                when {
                    vencido -> "Vencido · " + hora(b.inicio)
                    a.estadoE == Estado.EN_ESPERA -> "En espera · " + hora(b.inicio)
                    else -> hora(b.inicio) + " — " + hora(b.fin)
                },
                color = if (vencido) s.acentoTexto else MaterialTheme.colorScheme.onSurface, peso = FontWeight.SemiBold
            )
            EtiquetaPrioridad(a.prioridadE)
        }
    }
}

@Composable
private fun CronometroBloque(a: Aviso, ahora: LocalDateTime, tam: Int) {
    val s = LocalColores.current.superficies
    val desde = runCatching { Instant.parse(a.empezado) }.getOrNull()
    Cronometro(desde, ahora, EstiloMono.copy(fontSize = tam.sp, lineHeight = (tam + 4).sp, fontWeight = FontWeight.SemiBold), MaterialTheme.colorScheme.onSurface, s.acentoTexto)
}

@Composable
private fun Final(a: Aviso, tecnico: Tecnico?, siguiente: Boolean, acciones: AccionesAgenda) {
    if (siguiente && (a.cliente.direccion.isNotBlank() || a.cliente.telefono.isNotBlank())) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (a.cliente.telefono.isNotBlank()) BotonRedondo(Icons.Rounded.Call, "Llamar", { acciones.llamar(a.cliente.telefono) }, tam = 34.dp, tamIcono = 17.dp)
            if (a.cliente.direccion.isNotBlank()) BotonRedondo(Icons.Rounded.NearMe, "Ir", { acciones.irA(a.cliente.direccion) }, tam = 34.dp, tamIcono = 17.dp, relleno = RedOrange, tinta = Color(0xFF141416))
        }
    } else Avatar(tecnico, 28)
}

@Composable
private fun ContenidoCompacto(
    b: Reglas.Bloque, tecnico: Tecnico?, siguiente: Boolean, vencido: Boolean, ahora: LocalDateTime, acciones: AccionesAgenda, modifier: Modifier
) {
    val a = b.aviso
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Encabezado(b, vencido, ahora) }
                if (a.estadoE == Estado.EN_CURSO) CronometroBloque(a, ahora, 13)
            }
            Text(
                a.titulo, Modifier.padding(top = 4.dp).compartido("titulo-${a.id}"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Final(a, tecnico, siguiente, acciones)
    }
}

@Composable
private fun ContenidoAmplio(
    b: Reglas.Bloque, tecnico: Tecnico?, siguiente: Boolean, vencido: Boolean, ahora: LocalDateTime, acciones: AccionesAgenda, modifier: Modifier
) {
    val a = b.aviso
    val enCurso = a.estadoE == Estado.EN_CURSO
    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Encabezado(b, vencido, ahora) }
            if (enCurso) CronometroBloque(a, ahora, 17) else if (!siguiente) Avatar(tecnico, 28)
        }
        Text(
            a.titulo, Modifier.padding(top = 8.dp).compartido("titulo-${a.id}"), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = if (enCurso) 18.sp else 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp)
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dato(
                listOf(a.cliente.nombre, if (enCurso) a.ref else a.sistemaE.nombre).filter { it.isNotBlank() }.joinToString(" · "),
                Modifier.weight(1f), tam = 10f
            )
            if (enCurso || siguiente) Final(a, tecnico, siguiente, acciones)
        }
    }
}

// ---------------------------------------------------------------- filas

@Composable
private fun FilaAgenda(a: Aviso, t: Tecnico?, hoy: LocalDate, acciones: AccionesAgenda, modifier: Modifier) {
    val forma = RoundedCornerShape(18.dp)
    FilaDeslizable(
        a, forma, modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp),
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
            val tinta = Color(0xFF141416)
            Box(
                Modifier.fillMaxSize().background(fondo).padding(horizontal = 20.dp),
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
fun Buscador(texto: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier, ayuda: String = "Cliente, dirección, referencia…", foco: FocusRequester? = null) {
    val s = LocalColores.current.superficies
    Row(
        modifier.fillMaxWidth().height(54.dp).tarjeta(CircleShape).padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (texto.isEmpty()) {
                Text(ayuda, color = s.tenue, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
            BasicTextField(
                texto, alCambiar, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(RedOrange),
                modifier = Modifier.fillMaxWidth().then(if (foco != null) Modifier.focusRequester(foco) else Modifier)
            )
        }
        if (texto.isNotEmpty()) {
            IconButton(onClick = { alCambiar("") }) { Icon(Icons.Rounded.Close, "Borrar búsqueda") }
        }
    }
}

@Composable
private fun Vacio(acciones: AccionesAgenda, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val forma = RoundedCornerShape(24.dp)
    Column(
        modifier.padding(16.dp).fillMaxWidth().clip(forma).background(s.heroe)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(RedOrange.copy(alpha = 0.38f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.7f),
                    radius = size.width * 0.7f, center = Offset(size.width, 0f)
                )
            }
            .padding(22.dp)
    ) {
        Dato("Sin avisos", color = Color(0xFFFF6A33), peso = FontWeight.SemiBold)
        Text(
            "00", Modifier.padding(top = 6.dp), color = s.sobreHeroe,
            style = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-2).sp)
        )
        Text(
            "Crea el primero con el + o carga unos de ejemplo para ver cómo funciona.",
            Modifier.padding(top = 4.dp), color = s.sobreHeroeSuave, style = MaterialTheme.typography.bodyMedium
        )
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonPildora("Nuevo aviso", Icons.AutoMirrored.Rounded.ArrowForward, acciones.nuevo, Modifier.weight(1f))
            BotonPildora("Ejemplo", null, acciones.ejemplo, Modifier.weight(1f), principal = false)
        }
    }
}

/** Botón en cápsula: naranja con letra casi negra, o con línea sobre fondo oscuro. */
@Composable
fun BotonPildora(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector?, alPulsar: () -> Unit, modifier: Modifier = Modifier, principal: Boolean = true, alto: Dp = 50.dp) {
    val s = LocalColores.current.superficies
    val i = remember { MutableInteractionSource() }
    Row(
        modifier
            .height(alto)
            .presionable(i, 0.96f)
            .clip(CircleShape)
            .then(if (principal) Modifier.background(RedOrange) else Modifier.border(1.dp, s.sobreHeroe.copy(alpha = 0.3f), CircleShape))
            .clickable(i, LocalIndication.current, role = Role.Button, onClick = alPulsar)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val tinta = if (principal) Color(0xFF141416) else s.sobreHeroe
        Text(texto, color = tinta, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        if (icono != null) Icon(icono, null, Modifier.padding(start = 8.dp).size(18.dp), tint = tinta)
    }
}
