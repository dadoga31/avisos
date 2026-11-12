package io.github.dadoga31.avisos.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
 * Alta y edición. Lo justo a la vista (qué pasa, cliente, cuándo, quién, tipo,
 * prioridad); el resto en «Más detalles». El cliente se autocompleta con los
 * datos del último aviso suyo.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormularioPantalla(
    inicial: Aviso,
    esNuevo: Boolean,
    tecnicos: List<Tecnico>,
    clientes: List<Pair<Cliente, String>>,
    hoy: LocalDate,
    alGuardar: (Aviso) -> Unit,
    alCerrar: () -> Unit,
    detallesAbiertos: Boolean = !esNuevo
) {
    var a by remember { mutableStateOf(inicial) }
    var detalles by remember { mutableStateOf(detallesAbiertos) }
    var menuClientes by remember { mutableStateOf(false) }
    var deAviso by remember { mutableStateOf("") }
    var pedirFecha by remember { mutableStateOf(false) }
    var pedirHora by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

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

    Column(
        Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina)
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(Modifier.height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alCerrar) { Icon(Icons.Rounded.Close, "Cerrar sin guardar") }
            Text(
                if (esNuevo) "Nuevo aviso" else "Editar ${a.ref}", Modifier.weight(1f).padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = ::guardar, enabled = a.titulo.isNotBlank(), modifier = Modifier.padding(end = 8.dp)) { Text("Guardar") }
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(0.dp))
            OutlinedTextField(
                a.titulo, { a = a.copy(titulo = it) }, Modifier.fillMaxWidth(),
                label = { Text("Qué pasa *") },
                placeholder = { Text("Ej.: Central de alarma no comunica") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                trailingIcon = {
                    IconButton(onClick = {
                        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué pasa?")
                        try { dictado.launch(i) } catch (_: ActivityNotFoundException) {
                            Toast.makeText(contexto, "Este móvil no tiene dictado por voz", Toast.LENGTH_SHORT).show()
                        }
                    }) { Icon(Icons.Rounded.Mic, "Dictar") }
                }
            )

            val q = Reglas.normalizar(a.cliente.nombre)
            val sugerencias = if (q.length < 2) emptyList() else clientes.filter {
                Reglas.normalizar(it.first.nombre).contains(q) && Reglas.normalizar(it.first.nombre) != q
            }.take(5)
            Column {
                ExposedDropdownMenuBox(menuClientes && sugerencias.isNotEmpty(), { menuClientes = it }) {
                    OutlinedTextField(
                        a.cliente.nombre,
                        { a = a.copy(cliente = a.cliente.copy(nombre = it)); deAviso = ""; menuClientes = true },
                        Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                        label = { Text("Cliente") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Apartment, null) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        trailingIcon = if (a.cliente.nombre.isNotEmpty()) ({
                            IconButton(onClick = { a = a.copy(cliente = Cliente()); deAviso = "" }) { Icon(Icons.Rounded.Cancel, "Quitar cliente") }
                        }) else null
                    )
                    ExposedDropdownMenu(menuClientes && sugerencias.isNotEmpty(), { menuClientes = false }) {
                        sugerencias.forEach { (cl, ref) ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cl.nombre, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            listOf(cl.direccion, cl.telefono).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { a = a.copy(cliente = cl); deAviso = ref; menuClientes = false }
                            )
                        }
                    }
                }
                val datos = listOf(a.cliente.direccion, a.cliente.telefono).filter { it.isNotBlank() }
                if (datos.isNotEmpty() && !detalles) {
                    Row(Modifier.padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (deAviso.isNotEmpty()) Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            datos.joinToString(" · ") + if (deAviso.isNotEmpty()) " — del aviso $deAviso" else "",
                            Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Bloque("Cuándo") {
                val d = runCatching { LocalDate.parse(a.fecha) }.getOrNull()
                Opcion("Hoy", d == hoy) { a = a.copy(fecha = hoy.toString()) }
                Opcion("Mañana", d == hoy.plusDays(1)) { a = a.copy(fecha = hoy.plusDays(1).toString()) }
                val otro = d != null && d != hoy && d != hoy.plusDays(1)
                Opcion(if (otro) Reglas.dia(a.fecha, hoy) else "Otro día", otro, Icons.Rounded.CalendarMonth) { pedirFecha = true }
                Opcion("Sin fecha", d == null) { a = a.copy(fecha = "", hora = "") }
                if (d != null) Opcion(a.hora.ifEmpty { "Sin hora" }, a.hora.isNotEmpty(), Icons.Rounded.Schedule) { pedirHora = true }
            }

            Bloque("Quién") {
                tecnicos.forEach { t ->
                    FilterChip(
                        selected = a.asignadoA == t.id, onClick = { a = a.copy(asignadoA = t.id) },
                        label = { Text(t.nombre.split(" ").first()) },
                        leadingIcon = { Avatar(t, 24) }
                    )
                }
                Opcion("Sin asignar", a.asignadoA.isEmpty()) { a = a.copy(asignadoA = "") }
            }

            Bloque("Tipo") {
                Tipo.entries.forEach { t -> Opcion(t.nombre, a.tipo == t.id) { a = a.copy(tipo = t.id) } }
            }

            Column {
                Text("Prioridad", Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Prioridad.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = a.prioridad == p.id, onClick = { a = a.copy(prioridad = p.id) },
                            shape = SegmentedButtonDefaults.itemShape(i, Prioridad.entries.size)
                        ) { Text(p.nombre, maxLines = 1) }
                    }
                }
            }

            Elemento(RoundedCornerShape(20.dp), alPulsar = { detalles = !detalles }) {
                Icon(if (detalles) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Más detalles", style = MaterialTheme.typography.bodyLarge)
                    Text("Sistema, descripción, dirección, teléfono", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AnimatedVisibility(detalles) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Bloque("Sistema") {
                        Sistema.entries.forEach { s -> Opcion(s.nombre, a.sistema == s.id) { a = a.copy(sistema = s.id) } }
                    }
                    OutlinedTextField(
                        a.descripcion, { a = a.copy(descripcion = it) }, Modifier.fillMaxWidth(),
                        label = { Text("Descripción") }, minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    OutlinedTextField(
                        a.cliente.direccion, { a = a.copy(cliente = a.cliente.copy(direccion = it)) }, Modifier.fillMaxWidth(),
                        label = { Text("Dirección") }, leadingIcon = { Icon(Icons.Rounded.LocationOn, null) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            a.cliente.telefono, { a = a.copy(cliente = a.cliente.copy(telefono = it)) }, Modifier.weight(1f),
                            label = { Text("Teléfono") }, leadingIcon = { Icon(Icons.Rounded.Phone, null) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            a.cliente.contacto, { a = a.copy(cliente = a.cliente.copy(contacto = it)) }, Modifier.weight(1f),
                            label = { Text("Contacto") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                    }
                    /* Coma decimal: texto con teclado decimal, no número. */
                    OutlinedTextField(
                        a.duracion, { a = a.copy(duracion = it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
                        Modifier.fillMaxWidth(), label = { Text("Horas previstas") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bloque(titulo: String, contenido: @Composable () -> Unit) {
    Column {
        Text(titulo, Modifier.padding(start = 4.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { contenido() }
    }
}

@Composable
private fun Opcion(texto: String, elegida: Boolean, icono: androidx.compose.ui.graphics.vector.ImageVector? = null, alPulsar: () -> Unit) {
    FilterChip(
        selected = elegida, onClick = alPulsar, label = { Text(texto) },
        leadingIcon = when {
            elegida -> ({ Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) })
            icono != null -> ({ Icon(icono, null, Modifier.size(FilterChipDefaults.IconSize)) })
            else -> null
        }
    )
}
