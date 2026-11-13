package io.github.dadoga31.avisos.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Prioridad
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Sistema
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tipo
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Alta y edición en campos numerados con letra grande: qué pasa (con
 * micrófono), cliente (se rellena con su último aviso), cuándo, quién, tipo y
 * prioridad; el resto en «Más detalles». El botón de crear va abajo, al pulgar.
 * [ref]: la referencia que tendrá el aviso nuevo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormularioPantalla(
    inicial: Aviso,
    esNuevo: Boolean,
    tecnicos: List<Tecnico>,
    clientes: List<Pair<Cliente, String>>,
    hoy: LocalDate,
    alGuardar: (Aviso) -> Unit,
    alCerrar: () -> Unit,
    detallesAbiertos: Boolean = !esNuevo,
    ref: String = ""
) {
    var a by remember { mutableStateOf(inicial) }
    var detalles by remember { mutableStateOf(detallesAbiertos) }
    var deAviso by remember { mutableStateOf("") }
    var escribiendoCliente by remember { mutableStateOf(false) }
    var pedirFecha by remember { mutableStateOf(false) }
    var pedirHora by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val s = LocalColores.current.superficies

    val dictado = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { dicho ->
            a = a.copy(titulo = (a.titulo.trim() + " " + dicho.replaceFirstChar { it.uppercase() }).trim())
        }
    }

    fun guardar() {
        var final = a.copy(titulo = a.titulo.trim())
        /* Un aviso nuevo con fecha y técnico ya está programado. */
        if (esNuevo && final.estadoE == Estado.PENDIENTE && final.fecha.isNotEmpty() && final.asignadoA.isNotEmpty()) {
            final = final.copy(estado = Estado.PROGRAMADO.id)
        }
        alGuardar(final)
    }

    Column(Modifier.fillMaxSize().fondoPagina().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp)) {
            BotonRedondo(Icons.Rounded.Close, "Cerrar sin guardar", alCerrar, Modifier.align(Alignment.CenterStart))
            Dato(
                if (esNuevo) "Nuevo" + if (ref.isNotEmpty()) " · $ref" else "" else "Editar · ${a.ref}",
                Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, tam = 12f, peso = FontWeight.SemiBold
            )
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Campo(1, "Qué pasa") {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoTexto(
                        a.titulo, { a = a.copy(titulo = it) }, "Describe la avería",
                        TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 29.sp, lineHeight = 35.sp, letterSpacing = (-0.6).sp),
                        Modifier.weight(1f), lineas = 2, linea = false,
                        teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    BotonRedondo(
                        Icons.Rounded.Mic, "Dictar", {
                            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué pasa?")
                            try { dictado.launch(i) } catch (_: ActivityNotFoundException) {
                                Toast.makeText(contexto, "Este móvil no tiene dictado por voz", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tam = 48.dp, tamIcono = 22.dp, relleno = MaterialTheme.colorScheme.secondaryContainer, tinta = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Campo(2, "Cliente") {
                CampoTexto(
                    a.cliente.nombre,
                    { a = a.copy(cliente = a.cliente.copy(nombre = it)); deAviso = ""; escribiendoCliente = true },
                    "Nombre del cliente",
                    TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp),
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                val q = Reglas.normalizar(a.cliente.nombre)
                val sugerencias = if (!escribiendoCliente || q.length < 2) emptyList() else clientes.filter {
                    Reglas.normalizar(it.first.nombre).contains(q) && Reglas.normalizar(it.first.nombre) != q
                }.take(4)
                AnimatedVisibility(sugerencias.isNotEmpty()) {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sugerencias.forEach { (cl, r) ->
                            Row(
                                Modifier.fillMaxWidth().tarjeta(RoundedCornerShape(14.dp))
                                    .clickable { a = a.copy(cliente = cl); deAviso = r; escribiendoCliente = false }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(cl.nombre, style = MaterialTheme.typography.titleSmall)
                                    Dato(listOf(cl.direccion, cl.telefono).filter { it.isNotBlank() }.joinToString(" · "), tam = 9.5f)
                                }
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp), tint = s.tenue)
                            }
                        }
                    }
                }
                val datos = listOf(a.cliente.direccion, a.cliente.telefono).filter { it.isNotBlank() }
                if (datos.isNotEmpty() && !detalles && sugerencias.isEmpty()) {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (deAviso.isNotEmpty()) Icon(Icons.Rounded.AutoAwesome, null, Modifier.padding(end = 6.dp).size(14.dp), tint = RedOrange)
                        Dato(datos.joinToString(" · ") + if (deAviso.isNotEmpty()) " — del $deAviso" else "", tam = 9.5f)
                    }
                }
            }

            Campo(3, "Cuándo") {
                val d = runCatching { LocalDate.parse(a.fecha) }.getOrNull()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pastilla("Hoy", d == hoy) { a = a.copy(fecha = hoy.toString()) }
                    Pastilla("Mañana", d == hoy.plusDays(1)) { a = a.copy(fecha = hoy.plusDays(1).toString()) }
                    val otro = d != null && d != hoy && d != hoy.plusDays(1)
                    Pastilla(if (otro) Reglas.dia(a.fecha, hoy) else "Otro día", otro, icono = Icons.Rounded.CalendarMonth) { pedirFecha = true }
                    if (d != null) Pastilla(a.hora.ifEmpty { "Hora" }, a.hora.isNotEmpty(), icono = Icons.Rounded.Schedule, mono = a.hora.isNotEmpty()) { pedirHora = true }
                    Pastilla("Sin fecha", d == null) { a = a.copy(fecha = "", hora = "") }
                }
            }

            Campo(4, "Quién") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    tecnicos.forEach { t ->
                        OpcionQuien(t.iniciales, t.nombre.split(" ").first(), a.asignadoA == t.id) { a = a.copy(asignadoA = t.id) }
                    }
                    OpcionQuien("—", "Nadie", a.asignadoA.isEmpty()) { a = a.copy(asignadoA = "") }
                }
            }

            Campo(5, "Tipo") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tipo.entries.forEach { t -> Pastilla(t.nombre, a.tipo == t.id) { a = a.copy(tipo = t.id) } }
                }
            }

            Campo(6, "Prioridad") {
                Row(Modifier.fillMaxWidth()) {
                    Prioridad.entries.forEachIndexed { n, p ->
                        Senal(n, p, a.prioridad == p.id, Modifier.weight(1f)) { a = a.copy(prioridad = p.id) }
                    }
                }
            }

            /* 07: lo que no hace falta para crear el aviso. */
            val giro by animateFloatAsState(if (detalles) 180f else 0f, Muelles.suave(), label = "giro")
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).clickable(role = Role.Button) { detalles = !detalles }
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f)) {
                    Dato("07", color = s.acentoTexto)
                    Dato(" / Más detalles")
                }
                Icon(Icons.Rounded.ExpandMore, if (detalles) "Ocultar" else "Mostrar", Modifier.rotate(giro), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!detalles) {
                Dato("Sistema, descripción, dirección, teléfono, horas", Modifier.padding(horizontal = 24.dp), tam = 9f, color = s.tenue)
            }
            AnimatedVisibility(detalles) {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Spacer(Modifier.height(2.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Sistema.entries.forEach { st -> Pastilla(st.nombre, a.sistema == st.id) { a = a.copy(sistema = st.id) } }
                    }
                    Etiquetado("Descripción") {
                        CampoTexto(a.descripcion, { a = a.copy(descripcion = it) }, "Qué hay que hacer", MaterialTheme.typography.bodyLarge,
                            lineas = 3, teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                    }
                    Etiquetado("Dirección") {
                        CampoTexto(a.cliente.direccion, { a = a.copy(cliente = a.cliente.copy(direccion = it)) }, "Calle, número, población",
                            MaterialTheme.typography.bodyLarge, teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Etiquetado("Teléfono", Modifier.weight(1f)) {
                            CampoTexto(a.cliente.telefono, { a = a.copy(cliente = a.cliente.copy(telefono = it)) }, "600 00 00 00",
                                EstiloMono.copy(fontSize = 16.sp, lineHeight = 22.sp), teclado = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        }
                        Etiquetado("Contacto", Modifier.weight(1f)) {
                            CampoTexto(a.cliente.contacto, { a = a.copy(cliente = a.cliente.copy(contacto = it)) }, "Quién atiende",
                                MaterialTheme.typography.bodyLarge, teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                        }
                    }
                    /* Coma decimal: texto con teclado decimal, no número. */
                    Etiquetado("Horas previstas") {
                        CampoTexto(a.duracion, { a = a.copy(duracion = it.filter { c -> c.isDigit() || c == ',' || c == '.' }) }, "1,5",
                            EstiloMono.copy(fontSize = 16.sp, lineHeight = 22.sp), teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        val listo = a.titulo.isNotBlank()
        BotonPildora(
            if (esNuevo) "Crear aviso" else "Guardar cambios", Icons.AutoMirrored.Rounded.ArrowForward, { if (listo) guardar() },
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp).fillMaxWidth()
                .shadow(if (listo) 16.dp else 0.dp, CircleShape, ambientColor = RedOrange, spotColor = RedOrange)
                .alpha(if (listo) 1f else 0.45f),
            alto = 60.dp
        )
    }

    if (pedirFecha) {
        DialogoFecha(runCatching { LocalDate.parse(a.fecha) }.getOrNull() ?: hoy, alElegir = { a = a.copy(fecha = it.toString()) }, alCerrar = { pedirFecha = false })
    }
    if (pedirHora) {
        DialogoHora(
            runCatching { LocalTime.parse(a.hora) }.getOrNull(),
            alElegir = { a = a.copy(hora = it?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "") },
            alCerrar = { pedirHora = false }
        )
    }
}

/** «01 / QUÉ PASA» y su contenido. */
@Composable
private fun Campo(n: Int, titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    val s = LocalColores.current.superficies
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
        Row(Modifier.padding(bottom = 10.dp)) {
            Dato(n.toString().padStart(2, '0'), color = s.acentoTexto)
            Dato(" / $titulo")
        }
        contenido()
    }
}

@Composable
private fun Etiquetado(nombre: String, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Column(modifier) {
        Dato(nombre, Modifier.padding(bottom = 6.dp), tam = 9.5f)
        contenido()
    }
}

/** Campo sin caja: el texto en grande y una línea fina que se vuelve naranja al escribir. */
@Composable
internal fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    ayuda: String,
    estilo: TextStyle,
    modifier: Modifier = Modifier,
    lineas: Int = 1,
    linea: Boolean = true,
    teclado: KeyboardOptions = KeyboardOptions.Default
) {
    val s = LocalColores.current.superficies
    var foco by remember { mutableStateOf(false) }
    val color by animateColorAsState(if (foco) RedOrange else s.borde, Muelles.suave(), label = "linea")
    BasicTextField(
        valor, alCambiar, modifier.fillMaxWidth().onFocusChanged { foco = it.isFocused },
        textStyle = estilo.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(RedOrange),
        keyboardOptions = teclado,
        singleLine = lineas == 1,
        minLines = if (lineas > 1 && linea) lineas else 1,
        decorationBox = { campo ->
            Column {
                Box {
                    if (valor.isEmpty()) Text(ayuda, style = estilo, color = s.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    campo()
                }
                if (linea) Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(if (foco) 2.dp else 1.dp).background(color))
            }
        }
    )
}

/** Un técnico en círculo; el elegido, relleno y con anillo naranja. */
@Composable
private fun OpcionQuien(iniciales: String, nombre: String, elegida: Boolean, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val anillo by animateFloatAsState(if (elegida) 1f else 0f, Muelles.soltar(), label = "anillo")
    val fondo by animateColorAsState(if (elegida) MaterialTheme.colorScheme.secondaryContainer else s.item, Muelles.suave(), label = "fondo")
    val tinta by animateColorAsState(if (elegida) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface, Muelles.suave(), label = "tinta")
    val i = remember { MutableInteractionSource() }
    Column(
        Modifier.padding(6.dp).presionable(i, 0.92f)
            .clickable(i, null, role = Role.RadioButton, onClick = alPulsar)
            .semantics { contentDescription = nombre },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp)
                .drawBehind {
                    if (anillo > 0f) drawCircle(
                        RedOrange.copy(alpha = anillo), radius = size.minDimension / 2 + 5.dp.toPx() * anillo,
                        style = Stroke(2.dp.toPx())
                    )
                }
                .clip(CircleShape).background(fondo)
                .then(if (!elegida) Modifier.border(1.dp, s.borde, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(iniciales, color = tinta, style = EstiloMono.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
        Dato(nombre, Modifier.padding(top = 8.dp), tam = 9.5f, color = if (elegida) MaterialTheme.colorScheme.onSurface else Color.Unspecified)
    }
}

/** Prioridad como barras de señal: más barras, más prisa. */
@Composable
private fun Senal(nivel: Int, p: Prioridad, elegida: Boolean, modifier: Modifier, alPulsar: () -> Unit) {
    val s = LocalColores.current.superficies
    val urgente = p == Prioridad.URGENTE
    val i = remember { MutableInteractionSource() }
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).presionable(i, 0.92f)
            .clickable(i, LocalIndication.current, role = Role.RadioButton, onClick = alPulsar)
            .padding(vertical = 8.dp)
    ) {
        Row(Modifier.height(24.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (k in 0..3) {
                val color by animateColorAsState(
                    when {
                        k > nivel -> s.borde
                        elegida -> if (urgente) RedOrange else MaterialTheme.colorScheme.onSurface
                        else -> s.tenue.copy(alpha = 0.6f)
                    }, Muelles.suave(), label = "barra"
                )
                Box(Modifier.width(6.dp).height((8 + k * 5).dp).clip(RoundedCornerShape(2.dp)).background(color))
            }
        }
        Dato(
            p.nombre, Modifier.padding(top = 8.dp), tam = 9.5f,
            color = when {
                elegida && urgente -> s.acentoTexto
                elegida -> MaterialTheme.colorScheme.onSurface
                else -> Color.Unspecified
            },
            peso = if (elegida) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
