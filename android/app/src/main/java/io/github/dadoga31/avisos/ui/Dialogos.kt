package io.github.dadoga31.avisos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(inicial: LocalDate?, alElegir: (LocalDate) -> Unit, alCerrar: () -> Unit) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = (inicial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = alCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                alCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    ) { DatePicker(estado) }
}

/** Hora en 24 h, con «Sin hora» para quitarla. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(inicial: LocalTime?, alElegir: (LocalTime?) -> Unit, alCerrar: () -> Unit) {
    val h = inicial ?: LocalTime.of(9, 0)
    val estado = rememberTimePickerState(h.hour, h.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Hora") },
        text = { TimePicker(estado) },
        confirmButton = { TextButton(onClick = { alElegir(LocalTime.of(estado.hour, estado.minute)); alCerrar() }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = { alElegir(null); alCerrar() }) { Text("Sin hora") } }
    )
}

@Composable
fun DialogoTexto(
    titulo: String,
    etiqueta: String,
    inicial: String = "",
    variasLineas: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    textoAceptar: String = "Guardar",
    alAceptar: (String) -> Unit,
    alCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf(inicial) }
    val foco = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                texto, { texto = it }, label = { Text(etiqueta) }, singleLine = !variasLineas,
                minLines = if (variasLineas) 3 else 1,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = teclado),
                modifier = Modifier.fillMaxWidth().focusRequester(foco)
            )
            LaunchedEffect(Unit) { foco.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = { alAceptar(texto); alCerrar() }, enabled = texto.isNotBlank()) { Text(textoAceptar) }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}

/** Dos campos: nombre y teléfono del técnico. */
@Composable
fun DialogoTecnico(
    titulo: String,
    nombre: String,
    telefono: String,
    puedeBorrar: Boolean,
    alGuardar: (String, String) -> Unit,
    alBorrar: () -> Unit,
    alCerrar: () -> Unit
) {
    var n by remember { mutableStateOf(nombre) }
    var t by remember { mutableStateOf(telefono) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    n, { n = it }, label = { Text("Nombre") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    t, { t = it }, label = { Text("Teléfono") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { alGuardar(n, t); alCerrar() }, enabled = n.isNotBlank()) { Text("Guardar") } },
        dismissButton = {
            if (puedeBorrar) TextButton(onClick = { alBorrar(); alCerrar() }) { Text("Borrar") }
            else TextButton(onClick = alCerrar) { Text("Cancelar") }
        }
    )
}

@Composable
fun DialogoConfirmar(titulo: String, texto: String, accion: String, alAceptar: () -> Unit, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = { alAceptar(); alCerrar() }) { Text(accion) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } }
    )
}
