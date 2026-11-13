package io.github.dadoga31.avisos.ui

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.dadoga31.avisos.correo.AlmacenCorreo
import io.github.dadoga31.avisos.correo.ClienteCorreo
import io.github.dadoga31.avisos.correo.Cuenta
import io.github.dadoga31.avisos.correo.ServicioCorreo
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/** Lo que se sabe del buzón (sale de AlmacenCorreo.estado). */
data class EstadoBuzon(
    val configurada: Boolean = false,
    val usuario: String = "",
    val imap: Boolean = true,
    val activo: Boolean = false,
    val error: String = "",
    /** ISO de la última comprobación, o vacío. */
    val ultima: String = "",
    val pendientes: Int = 0,
    val caidas: Int = 0
)

data class FormCuenta(
    val usuario: String = "",
    val clave: String = "",
    val servidor: String = "",
    val imap: Boolean = true,
    val ssl: Boolean = true,
    val puerto: String = "993"
) {
    val valida get() = usuario.contains('@') && servidor.isNotBlank() && puerto.toIntOrNull() != null

    fun aCuenta() = Cuenta().also {
        it.usuario = usuario.trim(); it.clave = clave; it.servidor = servidor.trim()
        it.protocolo = if (imap) "imap" else "pop3"; it.seguridad = if (ssl) "ssl" else "starttls"
        it.puerto = puerto.toIntOrNull() ?: 993
    }

    companion object {
        fun puertoPorDefecto(imap: Boolean, ssl: Boolean) = when {
            imap && ssl -> "993"; imap -> "143"; ssl -> "995"; else -> "110"
        }

        fun de(c: Cuenta?) = if (c == null) FormCuenta() else FormCuenta(
            c.usuario, "", c.servidor, c.esImap(), c.esSSL(), c.puerto.toString()
        )
    }
}

class AccionesCorreo(
    val volver: () -> Unit = {},
    /** Prueba la conexión; contesta con null si va bien o con el error. */
    val probar: (FormCuenta, (String?) -> Unit) -> Unit = { _, _ -> },
    val guardar: (FormCuenta) -> Unit = {},
    val quitar: () -> Unit = {},
    val comprobar: () -> Unit = {},
    val releer: () -> Unit = {},
    val ignorados: (List<String>) -> Unit = {}
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorreoPantalla(
    estado: EstadoBuzon,
    inicial: FormCuenta,
    ignorados: List<String>,
    acciones: AccionesCorreo,
    ahora: Instant = Instant.now()
) {
    var f by remember(inicial) { mutableStateOf(inicial) }
    var verClave by remember { mutableStateOf(false) }
    var probando by remember { mutableStateOf(false) }
    var prueba by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var nuevoIgnorado by remember { mutableStateOf("") }
    var confirmarQuitar by remember { mutableStateOf(false) }
    val dominio = f.usuario.substringAfter('@', "").trim()

    Column(
        Modifier.fillMaxSize().fondoPagina()
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp)) {
            BotonRedondo(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", acciones.volver, Modifier.align(Alignment.CenterStart))
            Dato("Correo → aviso", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, tam = 12f, peso = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (estado.configurada) TarjetaEstado(estado, ahora, acciones)
            else {
                Text(
                    "Cada correo que llegue a esta cuenta se convierte en un aviso y te avisa con una notificación.",
                    Modifier.padding(horizontal = 24.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CabeceraSeccion("Cuenta")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    f.usuario, { f = f.copy(usuario = it.trim()) }, Modifier.fillMaxWidth(),
                    label = { Text("Correo") }, placeholder = { Text("avisos@empresa.es") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    f.clave, { f = f.copy(clave = it) }, Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") }, singleLine = true,
                    placeholder = { if (estado.configurada) Text("Sin cambios") },
                    visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { verClave = !verClave }) {
                            Icon(if (verClave) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (verClave) "Ocultar" else "Ver")
                        }
                    },
                    supportingText = { Text("Se guarda cifrada en este móvil y no sale de él.") }
                )
                OutlinedTextField(
                    f.servidor, { f = f.copy(servidor = it.trim()) }, Modifier.fillMaxWidth(),
                    label = { Text("Servidor") }, singleLine = true,
                    placeholder = { Text(if (dominio.isNotEmpty()) (if (f.imap) "imap." else "pop.") + dominio else "imap.empresa.es") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = if (f.servidor.isBlank() && dominio.isNotEmpty()) ({
                        TextButton(onClick = { f = f.copy(servidor = (if (f.imap) "imap." else "pop.") + dominio) }, Modifier.height(32.dp)) {
                            Text("Usar ${if (f.imap) "imap." else "pop."}$dominio")
                        }
                    }) else null
                )
                Text("Tipo de buzón", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmentos(
                    listOf("IMAP · al instante", "POP3 · cada 2 min"), if (f.imap) 0 else 1
                ) { i -> val imap = i == 0; f = f.copy(imap = imap, puerto = FormCuenta.puertoPorDefecto(imap, f.ssl)) }
                Text("Seguridad", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmentos(listOf("SSL/TLS", "STARTTLS"), if (f.ssl) 0 else 1) { i ->
                    val ssl = i == 0; f = f.copy(ssl = ssl, puerto = FormCuenta.puertoPorDefecto(f.imap, ssl))
                }
                OutlinedTextField(
                    f.puerto, { f = f.copy(puerto = it.filter { c -> c.isDigit() }.take(5)) }, Modifier.fillMaxWidth(),
                    label = { Text("Puerto") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                prueba?.let { (ok, texto) ->
                    val c = LocalColores.current.de(if (ok) Estado.RESUELTO else Estado.CANCELADO)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, null,
                            tint = if (ok) c.color else MaterialTheme.colorScheme.error
                        )
                        Text(texto, color = if (ok) c.color else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            probando = true; prueba = null
                            acciones.probar(f) { error ->
                                probando = false
                                prueba = if (error == null) true to "Conexión correcta" else false to error
                            }
                        },
                        enabled = f.valida && !probando, modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        if (probando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Probar")
                    }
                    Button(
                        onClick = { acciones.guardar(f); prueba = null },
                        enabled = f.valida && (estado.configurada || f.clave.isNotEmpty()),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Guardar") }
                }
            }

            CabeceraSeccion("Remitentes que no crean avisos")
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (ignorados.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ignorados.forEach { p ->
                            InputChip(
                                selected = false, onClick = { acciones.ignorados(ignorados - p) }, label = { Text(p) },
                                trailingIcon = { Icon(Icons.Rounded.Close, "Quitar $p", Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    nuevoIgnorado, { nuevoIgnorado = it.trim() }, Modifier.fillMaxWidth(),
                    label = { Text("Dirección o @dominio") }, placeholder = { Text("@boletines.es") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    trailingIcon = {
                        IconButton(
                            onClick = { acciones.ignorados((ignorados + nuevoIgnorado.lowercase()).distinct()); nuevoIgnorado = "" },
                            enabled = nuevoIgnorado.contains('@')
                        ) { Icon(Icons.Rounded.Add, "Añadir") }
                    }
                )
            }

            if (estado.configurada) {
                TextButton(onClick = { confirmarQuitar = true }, Modifier.padding(start = 12.dp, top = 16.dp)) {
                    Text("Quitar la cuenta", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (confirmarQuitar) {
        DialogoConfirmar(
            "¿Quitar la cuenta?", "La app deja de leer el buzón y olvida la contraseña. Los avisos ya creados se quedan.", "Quitar",
            alAceptar = acciones.quitar, alCerrar = { confirmarQuitar = false }
        )
    }
}

@Composable
private fun Segmentos(opciones: List<String>, elegida: Int, alElegir: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, t ->
            SegmentedButton(selected = i == elegida, onClick = { alElegir(i) }, shape = SegmentedButtonDefaults.itemShape(i, opciones.size)) {
                Text(t, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TarjetaEstado(e: EstadoBuzon, ahora: Instant, acciones: AccionesCorreo) {
    val s = LocalColores.current.superficies
    val bien = e.activo && e.error.isEmpty()
    val verde = Color(0xFF3DDC84)
    val naranja = Color(0xFFFF6A33)
    val (titulo, color) = when {
        e.error.isNotEmpty() -> e.error to naranja
        !e.activo -> "Conectando…" to s.sobreHeroeSuave
        e.caidas > 0 -> "Reconectando…" to s.sobreHeroeSuave
        e.imap -> "Escuchando en tiempo real" to verde
        else -> "Comprobando cada 2 minutos" to verde
    }
    val hace = runCatching { Duration.between(Instant.parse(e.ultima), ahora).toMinutes() }.getOrNull()
    val forma = RoundedCornerShape(24.dp)
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(forma).background(s.heroe)
            .drawBehind {
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(color.copy(alpha = 0.30f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, 0f), radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f, center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, 0f)
                )
            }
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuntoVivo(color, vivo = bien)
            Dato(titulo, color = color, peso = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Text(
            e.usuario, Modifier.padding(top = 10.dp), color = s.sobreHeroe,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        )
        Dato(
            listOfNotNull(
                if (e.imap) "IMAP" else "POP3",
                hace?.let { if (it < 1) "comprobado ahora" else "comprobado hace " + Reglas.minutos(it) },
                when (e.pendientes) { 0 -> null; 1 -> "1 correo esperando"; else -> "${e.pendientes} correos esperando" }
            ).joinToString(" · "),
            Modifier.padding(top = 6.dp), color = s.sobreHeroeSuave, tam = 9.5f
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotonPildora("Comprobar ahora", null, acciones.comprobar, Modifier.weight(1.4f), alto = 46.dp)
            BotonPildora("Releer 20", null, acciones.releer, Modifier.weight(1f), principal = false, alto = 46.dp)
        }
    }
}

// ---------------------------------------------------------------- conexión con el servicio

internal fun leerEstado(ctx: Context): EstadoBuzon {
    val cuenta = runCatching { Cuenta.cargar(ctx) }.getOrNull() ?: return EstadoBuzon()
    val o = AlmacenCorreo(ctx).estado(cuenta)
    return EstadoBuzon(
        configurada = cuenta.valida(), usuario = cuenta.usuario, imap = cuenta.esImap(),
        activo = o.optBoolean("activo"), error = o.optString("error"), ultima = o.optString("ultimaSync"),
        pendientes = o.optInt("pendientes"), caidas = o.optInt("caidas")
    )
}

/** La pantalla de verdad: lee la cuenta, sigue el estado del buzón y habla con el servicio. */
@Composable
fun CorreoRuta(vm: AvisosVM, ignorados: List<String>, alVolver: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var estado by remember { mutableStateOf(EstadoBuzon()) }
    var inicial by remember { mutableStateOf<FormCuenta?>(null) }
    var ahora by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        inicial = withContext(Dispatchers.IO) { FormCuenta.de(runCatching { Cuenta.cargar(ctx) }.getOrNull()) }
        while (true) {
            estado = withContext(Dispatchers.IO) { leerEstado(ctx) }
            ahora = Instant.now()
            delay(2000)
        }
    }

    val f = inicial ?: return
    CorreoPantalla(
        estado, f, ignorados,
        AccionesCorreo(
            volver = alVolver,
            probar = { form, contestar ->
                scope.launch {
                    val error = withContext(Dispatchers.IO) {
                        val c = form.aCuenta()
                        if (c.clave.isEmpty()) runCatching { Cuenta.cargar(ctx) }.getOrNull()?.let { c.clave = it.clave }   // «sin cambios»
                        try { ClienteCorreo(ctx, AlmacenCorreo(ctx)).probar(c) } catch (e: Exception) { ClienteCorreo.explicar(e) }
                    }
                    contestar(error)
                }
            },
            guardar = { form ->
                scope.launch(Dispatchers.IO) {
                    val c = form.aCuenta()
                    val anterior = runCatching { Cuenta.cargar(ctx) }.getOrNull()
                    val cambiaBuzon = anterior == null || anterior.usuario != c.usuario ||
                        anterior.servidor != c.servidor || anterior.protocolo != c.protocolo
                    Cuenta.guardar(ctx, c)
                    /* Buzón distinto: se olvida por dónde íbamos, para no mezclar dos servidores. */
                    if (cambiaBuzon) AlmacenCorreo(ctx).limpiarTodo()
                    ServicioCorreo.parar(ctx)
                    delay(300)
                    ServicioCorreo.arrancar(ctx)
                }
            },
            quitar = {
                scope.launch(Dispatchers.IO) {
                    ServicioCorreo.parar(ctx)
                    Cuenta.borrar(ctx)
                    AlmacenCorreo(ctx).limpiarTodo()
                    withContext(Dispatchers.Main) { inicial = FormCuenta() }
                }
            },
            comprobar = { ServicioCorreo.sincronizar(ctx); vm.procesarCorreo() },
            releer = {
                AlmacenCorreo(ctx).pedirRelectura(20)
                ServicioCorreo.parar(ctx)
                scope.launch { delay(300); ServicioCorreo.arrancar(ctx) }
            },
            ignorados = { lista -> vm.cambiarAjustes { it.copy(correoIgnorados = lista) } }
        )
    )
}
