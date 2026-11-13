package io.github.dadoga31.avisos.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.BuildConfig
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tema
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun dos(n: Int) = n.toString().padStart(2, '0')

/** Línea fina arriba de una fila (las listas de Hechos y Ajustes van sin tarjetas). */
private fun Modifier.lineaArriba(color: Color) = drawBehind { drawRect(color, Offset.Zero, androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx())) }

// ---------------------------------------------------------------- Hechos

/**
 * Panel de lo hecho: horas del periodo en grande con la diferencia con el
 * anterior, barras por día (o por mes), cifras y la lista por días.
 */
@Composable
fun HechosPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ahora: LocalDateTime,
    alAbrir: (String) -> Unit
) {
    val hoy = ahora.toLocalDate()
    val zona = ZoneId.systemDefault()
    val s = LocalColores.current.superficies
    var periodo by remember { mutableStateOf(Reglas.Periodo.SEMANA) }
    var filtro by remember { mutableStateOf<Estado?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    val panel = remember(avisos, periodo, hoy) { Reglas.panel(avisos, periodo, hoy, zona) }
    val lista = (if (texto.isBlank()) panel.cerrados else avisos.filter { !it.abierto }.sortedByDescending { it.cerrado })
        .filter { filtro == null || it.estadoE == filtro }
        .filter { texto.isBlank() || Reglas.filtrar(listOf(it), tecnicos, Reglas.Filtro(texto = texto), hoy).isNotEmpty() }
    val porDia = lista.groupBy { Reglas.diaDe(it.cerrado, zona) }
    androidx.activity.compose.BackHandler(buscando) { buscando = false; texto = "" }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = EspacioMenu)) {
        item(key = "barra") {
            BarraMarca("Hechos") {
                BotonRedondo(if (buscando) Icons.Rounded.Close else Icons.Rounded.Search, "Buscar en Hechos", { buscando = !buscando; if (!buscando) texto = "" })
                Segmentado(Reglas.Periodo.entries.map { it.nombre }, periodo.ordinal, { periodo = Reglas.Periodo.entries[it] }, ancho = 50.dp)
            }
        }
        if (buscando) item(key = "buscar") { Buscador(texto, { texto = it }, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), "Buscar en lo cerrado") }
        item(key = "cifra") {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp).entrada(0)) {
                Dato(
                    "Horas trabajadas · " + when (periodo) {
                        Reglas.Periodo.SEMANA -> "7 días"; Reglas.Periodo.MES -> "30 días"; Reglas.Periodo.ANO -> "12 meses"
                    }
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Reglas.horas(panel.horas).removeSuffix(" h"),
                        style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 76.sp, lineHeight = 80.sp, letterSpacing = (-3).sp)
                    )
                    Text("h", Modifier.padding(start = 8.dp, bottom = 14.dp), style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 22.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    val dif = panel.horas - panel.horasAntes
                    Column(Modifier.padding(bottom = 14.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            when {
                                dif > 0.05 -> "▲ +" + Reglas.horas(dif)
                                dif < -0.05 -> "▼ −" + Reglas.horas(-dif)
                                else -> "= igual"
                            },
                            style = EstiloMono.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = if (dif > 0.05) s.ok else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Dato("vs anterior", tam = 8.5f, color = s.tenue)
                    }
                }
            }
        }
        item(key = "barras-$periodo") { Barras(panel.barras, Modifier.padding(top = 4.dp).entrada(1)) }
        item(key = "kpi") {
            FilaKpi(
                listOf(
                    Kpi(dos(panel.resueltos), "Resueltos", activo = filtro == Estado.RESUELTO) { filtro = if (filtro == Estado.RESUELTO) null else Estado.RESUELTO },
                    Kpi(dos(panel.cancelados), if (panel.cancelados == 1) "Cancelado" else "Cancelados", activo = filtro == Estado.CANCELADO) {
                        filtro = if (filtro == Estado.CANCELADO) null else Estado.CANCELADO
                    },
                    Kpi(Reglas.horas(panel.porAviso).removeSuffix(" h"), "h / aviso")
                ),
                Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).entrada(2)
            )
        }
        if (lista.isEmpty()) {
            item(key = "vacio") {
                Text(
                    if (avisos.none { !it.abierto }) "Aquí irán los avisos que des por hechos." else "Nada en este periodo.",
                    Modifier.fillMaxWidth().padding(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        var orden = 3
        porDia.forEach { (dia, avs) ->
            val base = orden
            item(key = "dia-$dia") {
                CabeceraDato(
                    dia?.let { Reglas.dia(it.toString(), hoy).ifEmpty { it.toString() } } ?: "Sin fecha", Modifier.entrada(base),
                    extra = if (avs.any { it.horas > 0 }) Reglas.horas(avs.sumOf { it.horas }) else ""
                )
            }
            itemsIndexed(avs, key = { _, a -> a.id }) { i, a -> FilaHecho(a, Modifier.entrada(base + 1 + i)) { alAbrir(a.id) } }
            orden += 1 + avs.size
        }
    }
}

/** Barras que crecen desde abajo una tras otra; la de hoy (o este mes), en naranja. */
@Composable
private fun Barras(barras: List<Reglas.Barra>, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val max = maxOf(barras.maxOf { it.horas }, 1.0)
    val finas = barras.size > 12
    val mov = LocalMovimiento.current
    Row(
        modifier.fillMaxWidth().height(186.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (finas) 3.dp else 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barras.forEachIndexed { n, b ->
            val p = remember(barras) { Animatable(if (mov) 0f else 1f) }
            LaunchedEffect(barras) { if (mov) p.animateTo(1f, tween(600, delayMillis = 80 + n * (if (finas) 12 else 50), easing = FastOutSlowInEasing)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (!finas && b.horas > 0) {
                    Text(
                        Reglas.horas(b.horas).removeSuffix(" h"), style = EstiloMono.copy(fontSize = 10.sp),
                        color = if (b.actual) s.acentoTexto else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val alto = maxOf(3f, (130 * b.horas / max).toFloat())
                Box(
                    Modifier.padding(top = 6.dp).widthIn(max = 26.dp).fillMaxWidth().height(alto.dp)
                        .graphicsLayer { scaleY = p.value; transformOrigin = TransformOrigin(0.5f, 1f) }
                        .clip(RoundedCornerShape(if (finas) 3.dp else 8.dp))
                        .background(
                            when {
                                b.actual -> RedOrange
                                b.horas > 0 -> MaterialTheme.colorScheme.onSurface
                                else -> s.borde
                            }
                        )
                )
                Dato(
                    if (!finas || n % 5 == 4 || b.actual) b.etiqueta else "", Modifier.padding(top = 8.dp), tam = 9.5f,
                    color = if (b.actual) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun FilaHecho(a: Aviso, modifier: Modifier, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val cancelado = a.estadoE == Estado.CANCELADO
    val hora = runCatching { Instant.parse(a.cerrado).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrDefault("")
    Row(
        modifier.fillMaxWidth().clickable(onClick = alPulsar).padding(horizontal = 24.dp).lineaArriba(s.borde).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(if (cancelado) Icons.Rounded.Cancel else Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = if (cancelado) s.tenue else s.ok)
        Column(Modifier.weight(1f)) {
            Text(
                a.titulo, Modifier.compartido("titulo-${a.id}"), maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
                textDecoration = if (cancelado) TextDecoration.LineThrough else null,
                color = if (cancelado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Dato(listOf(a.cliente.nombre, a.ref).filter { it.isNotBlank() }.joinToString(" · "), Modifier.padding(top = 3.dp), tam = 9.5f)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (a.horas > 0) Reglas.horas(a.horas) else "—", style = EstiloMono.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Dato(hora, Modifier.padding(top = 3.dp), tam = 9.5f)
        }
    }
}

// ---------------------------------------------------------------- Equipo

/** Cada técnico con su carga (vencidos en naranja) y lo siguiente que tiene. */
@Composable
fun EquipoPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ahora: LocalDateTime,
    alAbrir: (String) -> Unit,
    alLlamar: (String) -> Unit,
    alGuardar: (String, String, String?) -> Unit,
    alBorrar: (String) -> Unit,
    nuevoAbierto: Boolean = false,
    alCerrarNuevo: () -> Unit = {}
) {
    val hoy = ahora.toLocalDate()
    var editando by remember { mutableStateOf<Tecnico?>(null) }
    var nuevo by remember { mutableStateOf(nuevoAbierto) }
    val abiertos = avisos.filter { it.abierto }
    val sinAsignar = abiertos.filter { it.asignadoA.isEmpty() }.sortedWith(Reglas.porFecha)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = EspacioMenu)) {
        item(key = "barra") { BarraMarca("Equipo") { BotonRedondo(Icons.Rounded.PersonAdd, "Añadir técnico", { nuevo = true }) } }
        item(key = "cifra") {
            Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp).entrada(0), verticalAlignment = Alignment.Bottom) {
                Cifra(
                    tecnicos.size, TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-3).sp),
                    relleno = 2
                )
                Column(Modifier.padding(start = 14.dp, bottom = 10.dp)) {
                    Dato(if (tecnicos.size == 1) "Técnico" else "Técnicos", color = MaterialTheme.colorScheme.onSurface)
                    Dato(if (abiertos.size == 1) "1 aviso abierto" else "${abiertos.size} avisos abiertos", Modifier.padding(top = 3.dp))
                }
            }
        }
        itemsIndexed(tecnicos, key = { _, t -> t.id }) { i, t ->
            val suyos = abiertos.filter { it.asignadoA == t.id }.sortedWith(Reglas.porFecha)
            TarjetaTecnico(
                t, suyos, hoy, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp).entrada(1 + i),
                alEditar = { editando = t }, alLlamar = alLlamar, alAbrir = alAbrir
            )
        }
        if (sinAsignar.isNotEmpty()) {
            item(key = "sin") { SinTecnico(sinAsignar, hoy, Modifier.padding(horizontal = 16.dp).entrada(1 + tecnicos.size), alAbrir) }
        }
    }

    editando?.let { t ->
        DialogoTecnico(
            "Técnico", t.nombre, t.telefono, puedeBorrar = t.id != Tecnico.YO,
            alGuardar = { n, tel -> alGuardar(n, tel, t.id) }, alBorrar = { alBorrar(t.id) }, alCerrar = { editando = null }
        )
    }
    if (nuevo) {
        DialogoTecnico(
            "Nuevo técnico", "", "", puedeBorrar = false,
            alGuardar = { n, tel -> alGuardar(n, tel, null) }, alBorrar = {}, alCerrar = { nuevo = false; alCerrarNuevo() }
        )
    }
}

@Composable
private fun TarjetaTecnico(
    t: Tecnico, suyos: List<Aviso>, hoy: LocalDate, modifier: Modifier,
    alEditar: () -> Unit, alLlamar: (String) -> Unit, alAbrir: (String) -> Unit
) {
    val s = LocalColores.current.superficies
    val (fondo, letra) = colorTecnico(t.color, s.oscuro)
    val vencidos = suyos.count { Reglas.vencido(it, hoy) }
    val siguiente = suyos.firstOrNull()
    val i = remember { MutableInteractionSource() }
    Column(
        modifier.fillMaxWidth().presionable(i, 0.985f).tarjeta(RoundedCornerShape(20.dp))
            .clickable(i, LocalIndication.current, onClick = alEditar).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(fondo), contentAlignment = Alignment.Center) {
                Text(t.iniciales, color = letra, style = EstiloMono.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
            Column(Modifier.weight(1f)) {
                Text(t.nombre, style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp))
                Dato(
                    if (t.id == Tecnico.YO) "Este móvil" else t.telefono.ifBlank { "Sin teléfono" },
                    Modifier.padding(top = 4.dp), tam = 9.5f
                )
            }
            if (t.telefono.isNotBlank()) BotonRedondo(Icons.Rounded.Call, "Llamar a ${t.nombre}", { alLlamar(t.telefono) }, tam = 44.dp)
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Dato("Carga", Modifier.width(80.dp), tam = 9.5f)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (n in 0 until 5) {
                    val color = when {
                        n < vencidos -> RedOrange
                        n < suyos.size -> MaterialTheme.colorScheme.onSurface
                        else -> null
                    }
                    Segmento(color, n, Modifier.weight(1f))
                }
            }
            Text(
                "${suyos.size}", Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                style = EstiloMono.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        Row(
            Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp))
                .then(if (siguiente != null) Modifier.clickable { alAbrir(siguiente.id) } else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Dato("Siguiente", Modifier.width(80.dp), tam = 9.5f)
            val (texto, color) = when {
                siguiente == null -> "Nada pendiente" to s.tenue
                Reglas.vencido(siguiente, hoy) -> "Vencido · " + siguiente.cliente.nombre.ifBlank { siguiente.titulo } to s.acentoTexto
                else -> listOf(Reglas.cuando(siguiente, hoy).ifBlank { "Sin fecha" }, siguiente.cliente.nombre.ifBlank { siguiente.titulo })
                    .joinToString(" · ") to MaterialTheme.colorScheme.onSurface
            }
            Text(
                texto, Modifier.weight(1f), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
            )
        }
    }
}

/** Un tramo de la barra de carga: se enciende de izquierda a derecha al entrar. */
@Composable
private fun Segmento(color: Color?, n: Int, modifier: Modifier) {
    val s = LocalColores.current.superficies
    val mov = LocalMovimiento.current
    val p = remember { Animatable(if (mov) 0f else 1f) }
    LaunchedEffect(color) { if (mov && color != null) p.animateTo(1f, tween(380, delayMillis = 200 + n * 90)) }
    Box(Modifier.height(6.dp).then(modifier).clip(CircleShape).background(s.borde)) {
        if (color != null) Box(
            Modifier.fillMaxSize().graphicsLayer { scaleX = p.value; transformOrigin = TransformOrigin(0f, 0.5f) }.background(color)
        )
    }
}

@Composable
private fun SinTecnico(avisos: List<Aviso>, hoy: LocalDate, modifier: Modifier, alAbrir: (String) -> Unit) {
    val s = LocalColores.current.superficies
    Column(modifier.fillMaxWidth().discontinua(s.tenue, 20.dp).padding(vertical = 6.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PersonAdd, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (avisos.size == 1) "1 aviso sin técnico" else "${avisos.size} avisos sin técnico", Modifier.padding(start = 12.dp),
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            )
        }
        avisos.forEach { a ->
            Row(
                Modifier.fillMaxWidth().clickable { alAbrir(a.id) }.padding(start = 48.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Dato(listOf(a.tipoE.nombre, Reglas.cuando(a, hoy).ifBlank { "Sin fecha" }).joinToString(" · "), Modifier.padding(top = 2.dp), tam = 9f)
                }
                Dato("Asignar", Modifier.padding(start = 8.dp), color = s.acentoTexto, peso = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------- Ajustes

class AccionesAjustes(
    val cambiar: ((Ajustes) -> Ajustes) -> Unit = {},
    val abrirCorreo: () -> Unit = {},
    val guardarCopia: () -> Unit = {},
    val recuperarCopia: () -> Unit = {},
    val ejemplo: () -> Unit = {},
    val borrarTodo: () -> Unit = {},
    val buscarActualizacion: () -> Unit = {},
    val actualizar: () -> Unit = {}
)

@Composable
fun AjustesPantalla(
    ajustes: Ajustes, avisos: List<Aviso>, correo: EstadoBuzon?, acciones: AccionesAjustes, ahora: Instant = Instant.now(),
    actualizacion: io.github.dadoga31.avisos.EstadoActualizacion = io.github.dadoga31.avisos.EstadoActualizacion.Nada
) {
    var prefijo by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var antelacion by remember { mutableStateOf(false) }
    val (siguiente, _) = Reglas.siguienteRef(ajustes.prefijoRef, ajustes.contadorRef, avisos)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BarraMarca("Ajustes") { Dato("v" + BuildConfig.VERSION_NAME, Modifier.padding(end = 6.dp)) }
        TarjetaCorreo(correo, ahora, Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).entrada(0), acciones.abrirCorreo)

        Column(Modifier.padding(horizontal = 24.dp)) {
            Seccion("Apariencia")
            FilaAjuste("Tema") {
                Segmentado(Tema.entries.map { if (it == Tema.AUTO) "Auto" else it.nombre }, ajustes.tema.ordinal, { n -> acciones.cambiar { it.copy(tema = Tema.entries[n]) } }, ancho = 64.dp)
            }
            val puede = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            FilaAjuste("Colores del fondo de pantalla", if (puede) "Material You" else "Necesita Android 12") {
                Interruptor(ajustes.materialYou && puede, { v -> acciones.cambiar { it.copy(materialYou = v) } }, habilitado = puede, descripcion = "Colores del fondo de pantalla")
            }

            Seccion("Avisos")
            FilaAjuste(
                "Recordatorio", if (ajustes.recordatorio <= 0) "Sin recordatorio" else "${ajustes.recordatorio} min antes de cada cita",
                alPulsar = { antelacion = true }
            ) {
                Text(
                    if (ajustes.recordatorio <= 0) "—" else "${ajustes.recordatorio}′",
                    style = EstiloMono.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                )
            }
            FilaAjuste("Numeración", "Próximo $siguiente", alPulsar = { prefijo = true })
            FilaAjuste("Pista de deslizar", "Recordatorio en la Agenda") {
                Interruptor(ajustes.pistaGestos, { v -> acciones.cambiar { it.copy(pistaGestos = v) } }, descripcion = "Pista de deslizar")
            }

            Seccion("Datos")
            FilaAjuste("Guardar copia de seguridad", "Avisos, técnicos, notas y fotos", alPulsar = acciones.guardarCopia)
            FilaAjuste("Recuperar una copia", "También las de la app anterior", alPulsar = acciones.recuperarCopia)
            FilaAjuste("Cargar avisos de ejemplo", "Siete avisos y dos técnicos", alPulsar = acciones.ejemplo)
            FilaAjuste("Borrar todos los avisos", "${avisos.size} guardados en este móvil", alPulsar = { borrar = true }, peligro = true)

            Seccion("Acerca de")
            if (actualizacion is io.github.dadoga31.avisos.EstadoActualizacion.Nada) {
                FilaAjuste("Avisos ${BuildConfig.VERSION_NAME}", "Toca para buscar actualizaciones", alPulsar = acciones.buscarActualizacion)
            } else {
                FilaAjuste("Avisos ${BuildConfig.VERSION_NAME}", "App nativa de Android")
                FranjaActualizacion(actualizacion, acciones.actualizar, Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(EspacioMenu))
    }

    if (antelacion) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { antelacion = false },
            title = { Text("Recordatorio") },
            text = {
                Column {
                    listOf(0, 10, 20, 30, 60).forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { acciones.cambiar { it.copy(recordatorio = m) }; antelacion = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = ajustes.recordatorio == m,
                                onClick = { acciones.cambiar { it.copy(recordatorio = m) }; antelacion = false }
                            )
                            Text(if (m == 0) "Sin recordatorio" else if (m == 60) "1 hora antes" else "$m minutos antes")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { antelacion = false }) { Text("Cerrar") } }
        )
    }
    if (prefijo) {
        DialogoTexto(
            "Numeración", "Prefijo de la referencia", ajustes.prefijoRef,
            alAceptar = { p -> acciones.cambiar { it.copy(prefijoRef = p.trim().uppercase().take(6)) } }, alCerrar = { prefijo = false }
        )
    }
    if (borrar) {
        DialogoConfirmar(
            "¿Borrar todos los avisos?", "Se borran los ${avisos.size} avisos de este móvil. Los técnicos se quedan. No se puede deshacer.",
            "Borrar todo", alAceptar = acciones.borrarTodo, alCerrar = { borrar = false }
        )
    }
}

@Composable
private fun Seccion(titulo: String) = Dato(titulo, Modifier.padding(top = 26.dp, bottom = 8.dp))

@Composable
private fun FilaAjuste(
    titulo: String,
    valor: String = "",
    alPulsar: (() -> Unit)? = null,
    peligro: Boolean = false,
    final: (@Composable () -> Unit)? = null
) {
    val s = LocalColores.current.superficies
    Row(
        Modifier.fillMaxWidth().lineaArriba(s.borde)
            .then(if (alPulsar != null) Modifier.clickable(role = Role.Button, onClick = alPulsar) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                titulo, color = if (peligro) s.acentoTexto else MaterialTheme.colorScheme.onSurface,
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp)
            )
            if (valor.isNotEmpty()) Dato(valor, Modifier.padding(top = 4.dp), tam = 9.5f)
        }
        when {
            final != null -> final()
            alPulsar != null -> Icon(Icons.Rounded.ChevronRight, null, tint = s.tenue)
        }
    }
}

/** El buzón en una tarjeta oscura: verde si escucha, naranja si falla. */
@Composable
private fun TarjetaCorreo(e: EstadoBuzon?, ahora: Instant, modifier: Modifier, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val verde = Color(0xFF3DDC84)
    val naranja = Color(0xFFFF6A33)
    val configurado = e?.configurada == true
    val falla = configurado && e!!.error.isNotEmpty()
    val bien = configurado && !falla && e!!.activo
    val color = when {
        !configurado -> naranja
        falla -> naranja
        bien -> verde
        else -> s.sobreHeroeSuave
    }
    val forma = RoundedCornerShape(24.dp)
    val i = remember { MutableInteractionSource() }
    Column(
        modifier.fillMaxWidth().presionable(i, 0.985f).clip(forma).background(s.heroe)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent), center = Offset(size.width * 0.95f, 0f), radius = size.width * 0.55f),
                    radius = size.width * 0.55f, center = Offset(size.width * 0.95f, 0f)
                )
            }
            .clickable(i, LocalIndication.current, onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuntoVivo(color, vivo = bien)
            Dato(
                "Correo · " + when {
                    !configurado -> "sin conectar"
                    falla -> "error"
                    !e!!.activo -> "conectando"
                    e.imap -> "escuchando"
                    else -> "cada 2 min"
                },
                color = color, peso = FontWeight.SemiBold
            )
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (configurado) e!!.usuario else "Correo → aviso", Modifier.weight(1f), color = s.sobreHeroe, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
            )
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = s.sobreHeroeSuave)
        }
        val detalle = when {
            !configurado -> "Conecta el buzón de la empresa: cada correo será un aviso"
            falla -> e!!.error
            else -> {
                val hace = runCatching { Duration.between(Instant.parse(e!!.ultima), ahora).toMinutes() }.getOrNull()
                listOfNotNull(
                    if (e!!.imap) "IMAP" else "POP3",
                    hace?.let { if (it < 1) "comprobado ahora" else "comprobado hace " + Reglas.minutos(it) },
                    when (e.pendientes) { 0 -> null; 1 -> "1 esperando"; else -> "${e.pendientes} esperando" }
                ).joinToString(" · ")
            }
        }
        Dato(detalle, Modifier.padding(top = 6.dp), color = s.sobreHeroeSuave, tam = 9.5f)
    }
}
