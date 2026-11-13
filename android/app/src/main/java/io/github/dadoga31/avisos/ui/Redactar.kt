package io.github.dadoga31.avisos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dadoga31.avisos.correo.AdjuntoEnvio
import io.github.dadoga31.avisos.correo.EnviadorCorreo
import io.github.dadoga31.avisos.correo.Envio
import io.github.dadoga31.avisos.datos.Adjunto
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Reglas
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Respuestas que se rellenan con los datos del aviso. Se pueden cambiar antes de mandar. */
object Plantillas {
    private val ES = Locale("es", "ES")

    /** «hoy a las 11:30», «mañana», «el lunes 16 de noviembre a las 09:00»; null sin fecha. */
    fun cuando(a: Aviso, hoy: LocalDate): String? {
        val d = runCatching { LocalDate.parse(a.fecha) }.getOrNull() ?: return null
        val dia = when (d) {
            hoy -> "hoy"
            hoy.plusDays(1) -> "mañana"
            else -> "el " + Reglas.fechaLarga(d).replace(",", "")
        }
        return dia + if (a.hora.isNotEmpty()) " a las ${a.hora}" else ""
    }

    fun respuestas(a: Aviso, hoy: LocalDate): List<Pair<String, String>> {
        val hola = "Buenos días,\n\n"
        val adios = "\n\nUn saludo."
        val ref = if (a.ref.isNotBlank()) " (ref. ${a.ref})" else ""
        val visita = cuando(a, hoy)
        return buildList {
            add("Recibido" to hola + "Hemos recibido su aviso$ref. " +
                (visita?.let { "Le confirmamos la visita $it." } ?: "Le llamaremos para concretar la visita.") + adios)
            if (visita != null) add("Visita" to hola + "Le confirmamos que pasaremos $visita" +
                (if (a.cliente.direccion.isNotBlank()) " por ${a.cliente.direccion}" else "") + "." + adios)
            add("De camino" to hola + "El técnico va de camino y llegará en breve." + adios)
            val notas = a.notas.reversed().joinToString("\n") { "· " + it.texto }
            add("Terminado" to hola + "Le confirmamos que el trabajo está terminado$ref." + (if (notas.isNotBlank()) "\n\n$notas" else "") + adios)
        }
    }

    /** Para reenviar a un compañero: lo que necesita para ir. */
    fun reenvios(a: Aviso, hoy: LocalDate): List<Pair<String, String>> = listOf(
        "Datos del aviso" to "Te reenvío este aviso.\n\n" + resumen(a, hoy),
        "Para que lo veas" to "Te reenvío este aviso para que lo tengas en cuenta."
    )

    fun resumen(a: Aviso, hoy: LocalDate): String = listOfNotNull(
        listOf(a.ref, a.titulo).filter { it.isNotBlank() }.joinToString(" · "),
        a.cliente.nombre.takeIf { it.isNotBlank() }?.let { "Cliente: $it" },
        a.cliente.direccion.takeIf { it.isNotBlank() }?.let { "Dirección: $it" },
        a.cliente.telefono.takeIf { it.isNotBlank() }?.let { "Teléfono: $it" + if (a.cliente.contacto.isNotBlank() && !a.cliente.contacto.contains('@')) " (${a.cliente.contacto})" else "" },
        cuando(a, hoy)?.let { "Cuándo: $it" },
        "${a.tipoE.nombre} · ${a.sistemaE.nombre} · prioridad ${a.prioridadE.nombre.lowercase()}"
    ).joinToString("\n")

    private val FECHA = DateTimeFormatter.ofPattern("d MMM yyyy, H:mm", ES)

    fun fechaCorreo(a: Aviso): String =
        runCatching { Instant.parse(a.creado).atZone(ZoneId.systemDefault()).format(FECHA) }.getOrDefault("")

    /** El correo original: Message-ID (sin «m:»), quién lo mandó y cuándo. */
    fun deCorreo(a: Aviso) = a.origen == "correo" && a.cliente.contacto.contains('@')

    fun remitente(a: Aviso) = if (a.cliente.nombre.isNotBlank() && a.cliente.nombre != a.cliente.contacto) "${a.cliente.nombre} <${a.cliente.contacto}>" else a.cliente.contacto
}

/** Lo que se escribe, ya preparado para enviar. */
fun envioDe(
    a: Aviso, reenvio: Boolean, hoy: LocalDate, para: String, cc: String, asunto: String, texto: String,
    firma: String?, adjuntos: List<AdjuntoEnvio>, copiaParaMi: Boolean
): Envio {
    val correo = Plantillas.deCorreo(a)
    val cabecera = when {
        !correo && reenvio -> "---------- Aviso ${a.ref} ----------"
        reenvio -> "---------- Mensaje reenviado ----------\nDe: ${Plantillas.remitente(a)}\nFecha: ${Plantillas.fechaCorreo(a)}\nAsunto: ${a.titulo}"
        else -> "El ${Plantillas.fechaCorreo(a)}, ${Plantillas.remitente(a)} escribió:"
    }
    val cita = if (!correo && reenvio) Plantillas.resumen(a, hoy) + if (a.descripcion.isNotBlank()) "\n\n" + a.descripcion else "" else a.descripcion
    return Envio(
        para = EnviadorCorreo.direcciones(para), cc = EnviadorCorreo.direcciones(cc), asunto = asunto.trim(), texto = texto,
        firma = firma, cabeceraCita = cabecera, cita = cita, reenvio = reenvio,
        enRespuestaA = if (!reenvio && a.correoId.startsWith("m:")) a.correoId.removePrefix("m:") else "",
        adjuntos = adjuntos, copiaParaMi = copiaParaMi
    )
}

/**
 * Responder o reenviar el correo de un aviso. Para y asunto ya puestos,
 * respuestas rápidas con los datos del aviso, adjuntos del aviso, la firma y
 * el original debajo. [envioListo]: null mientras se lee la cuenta.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RedactarPantalla(
    a: Aviso?,
    reenvio: Boolean,
    hoy: LocalDate,
    firma: String?,
    envioListo: Boolean?,
    copiaInicial: Boolean,
    fichero: (Adjunto) -> File,
    enviando: Boolean,
    error: String?,
    alEnviar: (Envio, String) -> Unit,
    alCerrar: () -> Unit,
    alConfigurar: () -> Unit,
    extras: List<AdjuntoEnvio> = emptyList(),
    alAdjuntar: () -> Unit = {},
    alQuitarExtra: (AdjuntoEnvio) -> Unit = {},
    contactos: List<Pair<String, String>> = emptyList()
) {
    val s = LocalColores.current.superficies
    val correo = a != null && Plantillas.deCorreo(a)
    var para by remember { mutableStateOf(if (a == null || reenvio) "" else a.cliente.contacto) }
    var cc by remember { mutableStateOf("") }
    var verCc by remember { mutableStateOf(false) }
    var asunto by remember {
        mutableStateOf(
            when {
                a == null -> ""
                !reenvio -> EnviadorCorreo.conPrefijo("Re:", a.titulo)
                correo -> EnviadorCorreo.conPrefijo("RV:", a.titulo)
                else -> "Aviso ${a.ref}: ${a.titulo}"
            }
        )
    }
    var texto by remember { mutableStateOf("") }
    var conFirma by remember { mutableStateOf(true) }
    var copia by remember(copiaInicial) { mutableStateOf(copiaInicial) }
    var elegidos by remember { mutableStateOf(if (a != null && reenvio) a.adjuntos.map { it.id }.toSet() else emptySet()) }
    val plantillas = when {
        a == null -> emptyList()
        reenvio -> Plantillas.reenvios(a, hoy)
        else -> Plantillas.respuestas(a, hoy)
    }
    val listo = EnviadorCorreo.direcciones(para).isNotEmpty() && texto.isNotBlank() && envioListo == true && !enviando

    Column(Modifier.fillMaxSize().fondoPagina().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp)) {
            BotonRedondo(Icons.Rounded.Close, "Cerrar sin enviar", alCerrar, Modifier.align(Alignment.CenterStart))
            Dato(
                when {
                    a == null -> "Nuevo correo"
                    reenvio -> "Reenviar" + if (a.ref.isNotBlank()) " · ${a.ref}" else ""
                    else -> "Responder" + if (a.ref.isNotBlank()) " · ${a.ref}" else ""
                },
                Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, tam = 12f, peso = FontWeight.SemiBold
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (envioListo == false) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(s.tinte)
                        .border(1.dp, RedOrange.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp), tint = RedOrange)
                    Text("Falta configurar el envío (SMTP)", Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.TextButton(onClick = alConfigurar) { Text("Configurar", color = s.acentoTexto) }
                }
            }
            Campo("Para") {
                CampoTexto(
                    para, { para = it }, if (a == null || reenvio) "correo@ejemplo.es" else "Destinatario", MaterialTheme.typography.bodyLarge,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                /* Sugerencias de los clientes que ya han escrito, según lo que se teclea. */
                val trozo = para.substringAfterLast(',').substringAfterLast(';').trim()
                val q = Reglas.normalizar(trozo)
                val sugeridos = if (q.length < 2) emptyList() else contactos.filter { (n, c) ->
                    (Reglas.normalizar(n).contains(q) || c.lowercase().contains(q)) && !para.contains(c, ignoreCase = true)
                }.take(4)
                if (sugeridos.isNotEmpty()) Row(
                    Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sugeridos.forEach { (n, c) ->
                        Pastilla(if (n.isNotBlank() && n != c) "$n · $c" else c, false) {
                            /* Lo escrito antes del trozo que se está tecleando. */
                            val antes = para.substring(0, para.lastIndexOf(trozo).coerceAtLeast(0))
                            para = (if (antes.isBlank()) "" else antes.trimEnd(',', ';', ' ') + ", ") + c + ", "
                        }
                    }
                }
                if (!verCc) Pastilla("+ CC", false, Modifier.padding(top = 10.dp)) { verCc = true }
            }
            if (verCc) Campo("CC") {
                CampoTexto(cc, { cc = it }, "Con copia a…", MaterialTheme.typography.bodyLarge, teclado = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
            Campo("Asunto") {
                CampoTexto(asunto, { asunto = it }, "Asunto", MaterialTheme.typography.titleMedium, teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            }
            if (plantillas.isNotEmpty()) Campo(if (reenvio) "Rápidas" else "Respuestas rápidas") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    plantillas.forEach { (nombre, cuerpo) ->
                        Pastilla(nombre, texto.trim() == cuerpo.trim()) { texto = if (texto.isBlank() || plantillas.any { it.second.trim() == texto.trim() }) cuerpo else texto.trimEnd() + "\n\n" + cuerpo }
                    }
                }
            }
            Campo("Mensaje") {
                CampoTexto(
                    texto, { texto = it }, "Escribe aquí…", MaterialTheme.typography.bodyLarge, lineas = 6,
                    teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            Campo("Adjuntos") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    a?.adjuntos?.forEach { adj ->
                        val sel = adj.id in elegidos
                        Pastilla(adj.nombre.take(24), sel, icono = if (adj.esImagen) Icons.Rounded.Image else Icons.Rounded.AttachFile) {
                            elegidos = if (sel) elegidos - adj.id else elegidos + adj.id
                        }
                    }
                    extras.forEach { x ->
                        Pastilla(x.nombre.take(24) + "  ×", true, icono = if (x.mime.startsWith("image/")) Icons.Rounded.Image else Icons.Rounded.AttachFile) { alQuitarExtra(x) }
                    }
                    Pastilla("Adjuntar", false, icono = Icons.Rounded.AttachFile, alPulsar = alAdjuntar)
                }
            }
            Campo("Firma") {
                if (firma == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sin firma todavía", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.TextButton(onClick = alConfigurar) { Text("Añadirla", color = s.acentoTexto) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Incluir firma", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Interruptor(conFirma, { conFirma = it }, descripcion = "Incluir firma")
                    }
                    if (conFirma) VistaFirma(firma, Modifier.padding(top = 10.dp).fillMaxWidth().height(140.dp))
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Copia para mí", style = MaterialTheme.typography.bodyLarge)
                        Text("Te llega en copia oculta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Interruptor(copia, { copia = it }, descripcion = "Copia para mí")
                }
            }
            if (a != null) Campo(if (reenvio) "Lo que se reenvía" else "Mensaje original") {
                val envio = envioDe(a, reenvio, hoy, para, cc, asunto, texto, null, emptyList(), false)
                Column(Modifier.fillMaxWidth().tarjeta(RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Text(envio.cabeceraCita, style = EstiloMono.copy(fontSize = 11.sp, lineHeight = 16.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        envio.cita.ifBlank { "(sin texto)" }, Modifier.padding(top = 8.dp), maxLines = 8, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (error != null) {
                Text(error, Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp), color = s.acentoTexto, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
        Box(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp).fillMaxWidth()
                .shadow(if (listo) 16.dp else 0.dp, CircleShape, ambientColor = RedOrange, spotColor = RedOrange)
                .alpha(if (listo || enviando) 1f else 0.45f)
        ) {
            BotonPildora(
                if (enviando) "Enviando…" else "Enviar", if (enviando) null else Icons.AutoMirrored.Rounded.Send,
                {
                    if (!listo) return@BotonPildora
                    val delAviso = a?.adjuntos.orEmpty().filter { it.id in elegidos }.map { AdjuntoEnvio(fichero(it), it.nombre, it.mime) }
                    val f = firma?.takeIf { conFirma }
                    val envio = if (a == null) Envio(
                        para = EnviadorCorreo.direcciones(para), cc = EnviadorCorreo.direcciones(cc), asunto = asunto.trim(),
                        texto = texto, firma = f, adjuntos = extras, copiaParaMi = copia
                    ) else envioDe(a, reenvio, hoy, para, cc, asunto, texto, f, delAviso + extras, copia)
                    val nota = (if (reenvio) "Reenviado a " else "Respuesta enviada a ") + envio.para.joinToString(", ") + ":\n" + texto.trim()
                    alEnviar(envio, nota)
                },
                Modifier.fillMaxWidth(), alto = 60.dp
            )
            if (enviando) CircularProgressIndicator(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(22.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color(0xFF141416))
        }
    }
}

@Composable
private fun Campo(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
        Dato(titulo, Modifier.padding(bottom = 10.dp))
        contenido()
    }
}


/** Un archivo del móvil (documentos o galería) copiado a la caché para adjuntarlo. */
internal fun copiarParaEnviar(ctx: android.content.Context, uri: android.net.Uri): AdjuntoEnvio? = runCatching {
    val cr = ctx.contentResolver
    var nombre = "adjunto"
    cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) nombre = c.getString(0) ?: nombre
    }
    nombre = nombre.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val dir = File(ctx.cacheDir, "envio").apply { mkdirs() }
    val destino = File(dir, "${System.currentTimeMillis()}-$nombre")
    cr.openInputStream(uri)!!.use { entrada -> destino.outputStream().use { entrada.copyTo(it) } }
    AdjuntoEnvio(destino, nombre, cr.getType(uri) ?: "application/octet-stream")
}.getOrNull()
