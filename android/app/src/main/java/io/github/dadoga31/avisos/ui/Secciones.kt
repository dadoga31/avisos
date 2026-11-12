package io.github.dadoga31.avisos.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.dadoga31.avisos.BuildConfig
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.datos.Tema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Barra de arriba de las pantallas con título grande: solo acciones a la derecha. */
@Composable
private fun BarraSuperior(acciones: @Composable () -> Unit = {}) {
    Row(Modifier.statusBarsPadding().height(64.dp).fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f))
        acciones()
    }
}

// ---------------------------------------------------------------- Hechos

@Composable
fun HechosPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ahora: LocalDateTime,
    alAbrir: (String) -> Unit
) {
    val hoy = ahora.toLocalDate()
    val zona = ZoneId.systemDefault()
    var filtro by remember { mutableStateOf<Estado?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    val porId = tecnicos.associateBy { it.id }
    val cerrados = avisos.filter { !it.abierto }
    val semana = cerrados.filter { (Reglas.diaDe(it.cerrado, zona) ?: LocalDate.MIN) > hoy.minusDays(7) }
    val q = Reglas.normalizar(texto)
    val lista = cerrados
        .filter { filtro == null || it.estadoE == filtro }
        .filter { q.isEmpty() || Reglas.filtrar(listOf(it), tecnicos, Reglas.Filtro(texto = texto), hoy).isNotEmpty() }
        .sortedByDescending { it.cerrado }
    val porDia = lista.groupBy { Reglas.diaDe(it.cerrado, zona) }

    LazyColumn(Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina), contentPadding = RellenoLista) {
        item(key = "cab") {
            BarraSuperior {
                IconButton(onClick = { buscando = !buscando; if (!buscando) texto = "" }) {
                    Icon(if (buscando) Icons.Rounded.Close else Icons.Rounded.Search, "Buscar en Hechos")
                }
            }
            TituloGrande(
                "Hechos",
                semana.count { it.estadoE == Estado.RESUELTO }.let { if (it == 1) "1 resuelto" else "$it resueltos" } + " · " +
                    semana.count { it.estadoE == Estado.CANCELADO }.let { if (it == 1) "1 cancelado" else "$it cancelados" } + " esta semana"
            )
            if (buscando) Buscador(texto, { texto = it }, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), "Buscar en lo cerrado")
            Cifras(
                listOf(
                    "Hoy" to cerrados.count { it.estadoE == Estado.RESUELTO && Reglas.diaDe(it.cerrado, zona) == hoy }.toString(),
                    "7 días" to semana.count { it.estadoE == Estado.RESUELTO }.toString(),
                    "Horas 7 d" to Reglas.horas(semana.sumOf { it.horas }).removeSuffix(" h"),
                    "Este mes" to cerrados.count {
                        it.estadoE == Estado.RESUELTO && Reglas.diaDe(it.cerrado, zona)?.let { d -> d.year == hoy.year && d.month == hoy.month } == true
                    }.toString()
                )
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(null to "Todos", Estado.RESUELTO to "Resueltos", Estado.CANCELADO to "Cancelados").forEach { (e, nombre) ->
                    val n = if (e == null) cerrados.size else cerrados.count { it.estadoE == e }
                    FilterChip(
                        selected = filtro == e, onClick = { filtro = e }, label = { Text("$nombre  $n") },
                        leadingIcon = if (filtro == e) ({ Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }) else null
                    )
                }
            }
        }
        if (lista.isEmpty()) {
            item(key = "vacio") {
                Text(
                    if (cerrados.isEmpty()) "Aquí irán los avisos que des por hechos." else "Nada coincide.",
                    Modifier.fillMaxWidth().padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        porDia.forEach { (dia, avs) ->
            item(key = "dia-$dia") {
                CabeceraSeccion(
                    dia?.let { Reglas.dia(it.toString(), hoy).let { d -> if (d.isEmpty()) it.toString() else d } } ?: "Sin fecha",
                    extra = Reglas.horas(avs.sumOf { it.horas }).takeIf { avs.any { a -> a.horas > 0 } } ?: ""
                )
            }
            itemsIndexed(avs, key = { _, a -> a.id }) { i, a ->
                val hora = runCatching {
                    java.time.Instant.parse(a.cerrado).atZone(zona).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault("")
                FilaAviso(
                    a, porId[a.asignadoA], hora, formaEnGrupo(i, avs.size),
                    Modifier.padding(horizontal = 16.dp).padding(bottom = 2.dp),
                    textoEstado = a.estadoE.nombre + if (a.horas > 0) " · " + Reglas.horas(a.horas) else "",
                    alPulsar = { alAbrir(a.id) }
                )
            }
        }
    }
}

@Composable
private fun Cifras(datos: List<Pair<String, String>>) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = LocalColores.current.superficies.item,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(vertical = 16.dp, horizontal = 4.dp).height(IntrinsicAlto)) {
            datos.forEachIndexed { i, (nombre, valor) ->
                if (i > 0) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(valor, style = MaterialTheme.typography.headlineMedium)
                    Text(nombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val IntrinsicAlto = 56.dp

// ---------------------------------------------------------------- Equipo

@Composable
fun EquipoPantalla(
    avisos: List<Aviso>,
    tecnicos: List<Tecnico>,
    ahora: LocalDateTime,
    alAbrir: (String) -> Unit,
    alLlamar: (String) -> Unit,
    alGuardar: (String, String, String?) -> Unit,
    alBorrar: (String) -> Unit,
    nuevoAbierto: Boolean,
    alCerrarNuevo: () -> Unit
) {
    val hoy = ahora.toLocalDate()
    var editando by remember { mutableStateOf<Tecnico?>(null) }
    val abiertos = avisos.filter { it.abierto }
    val sinAsignar = abiertos.filter { it.asignadoA.isEmpty() }.sortedWith(Reglas.porFecha)

    LazyColumn(Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina), contentPadding = RellenoLista) {
        item(key = "cab") {
            BarraSuperior()
            TituloGrande(
                "Equipo",
                "${tecnicos.size} ${if (tecnicos.size == 1) "técnico" else "técnicos"} · ${abiertos.count { it.asignadoA.isNotEmpty() }} avisos abiertos asignados"
            )
        }
        itemsIndexed(tecnicos, key = { _, t -> t.id }) { i, t ->
            val suyos = abiertos.filter { it.asignadoA == t.id }.sortedWith(Reglas.porFecha)
            val vencidos = suyos.count { Reglas.vencido(it, hoy) }
            val proximo = suyos.firstOrNull { !Reglas.vencido(it, hoy) && it.fecha.isNotEmpty() }
            val detalle = buildList {
                add(if (suyos.size == 1) "1 abierto" else "${suyos.size} abiertos")
                if (vencidos > 0) add(if (vencidos == 1) "1 vencido" else "$vencidos vencidos")
                else proximo?.let { add(Reglas.cuando(it, hoy).replaceFirstChar { c -> c.lowercase() }) }
            }.joinToString(" · ")
            Elemento(formaEnGrupo(i, tecnicos.size), Modifier.padding(horizontal = 16.dp).padding(bottom = 2.dp), alPulsar = { editando = t }) {
                Avatar(t, 40)
                Column(Modifier.weight(1f)) {
                    Text(t.nombre, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        detalle, style = MaterialTheme.typography.bodyMedium,
                        color = if (vencidos > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (t.telefono.isNotBlank()) {
                    FilledTonalIconButton(onClick = { alLlamar(t.telefono) }) { Icon(Icons.Rounded.Call, "Llamar a ${t.nombre}") }
                } else {
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (sinAsignar.isNotEmpty()) {
            item(key = "cab-sin") { CabeceraSeccion("Sin asignar", extra = "${sinAsignar.size}") }
            itemsIndexed(sinAsignar, key = { _, a -> "s-" + a.id }) { i, a ->
                Elemento(formaEnGrupo(i, sinAsignar.size), Modifier.padding(horizontal = 16.dp).padding(bottom = 2.dp), alPulsar = { alAbrir(a.id) }) {
                    Avatar(null, 40)
                    Column(Modifier.weight(1f)) {
                        Text(a.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        Text(
                            listOf(a.cliente.nombre, Reglas.cuando(a, hoy)).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { alAbrir(a.id) }) { Text("Asignar") }
                }
            }
        }
    }

    editando?.let { t ->
        DialogoTecnico(
            "Técnico", t.nombre, t.telefono, puedeBorrar = t.id != Tecnico.YO,
            alGuardar = { n, tel -> alGuardar(n, tel, t.id) }, alBorrar = { alBorrar(t.id) }, alCerrar = { editando = null }
        )
    }
    if (nuevoAbierto) {
        DialogoTecnico(
            "Nuevo técnico", "", "", puedeBorrar = false,
            alGuardar = { n, tel -> alGuardar(n, tel, null) }, alBorrar = {}, alCerrar = alCerrarNuevo
        )
    }
}

// ---------------------------------------------------------------- Ajustes

class AccionesAjustes(
    val cambiar: ((Ajustes) -> Ajustes) -> Unit = {},
    val ejemplo: () -> Unit = {},
    val borrarTodo: () -> Unit = {}
)

@Composable
fun AjustesPantalla(ajustes: Ajustes, avisos: List<Aviso>, estadoCorreo: String?, acciones: AccionesAjustes) {
    var prefijo by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    val (siguiente, _) = Reglas.siguienteRef(ajustes.prefijoRef, ajustes.contadorRef, avisos)

    Column(
        Modifier.fillMaxSize().background(LocalColores.current.superficies.pagina).verticalScroll(rememberScrollState())
    ) {
        BarraSuperior()
        TituloGrande("Ajustes")

        CabeceraSeccion("Apariencia")
        Grupo(
            listOf(
                { f ->
                    Elemento(f) {
                        Icon(Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text("Tema", style = MaterialTheme.typography.bodyLarge)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Tema.entries.forEachIndexed { i, t ->
                                    SegmentedButton(
                                        selected = ajustes.tema == t, onClick = { acciones.cambiar { it.copy(tema = t) } },
                                        shape = SegmentedButtonDefaults.itemShape(i, Tema.entries.size)
                                    ) { Text(t.nombre, maxLines = 1) }
                                }
                            }
                        }
                    }
                },
                { f ->
                    val puede = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    FilaSimple(
                        f, Icons.Rounded.Palette, "Colores del fondo de pantalla",
                        if (puede) "Material You: la app toma los colores de tu fondo" else "Necesita Android 12 o más"
                    ) {
                        Switch(ajustes.materialYou && puede, { v -> acciones.cambiar { it.copy(materialYou = v) } }, enabled = puede)
                    }
                }
            )
        )

        CabeceraSeccion("Avisos")
        Grupo(
            listOf(
                { f -> FilaSimple(f, Icons.Rounded.Tag, "Numeración", "El próximo será $siguiente", alPulsar = { prefijo = true }) },
                { f ->
                    FilaSimple(f, Icons.Rounded.SwipeLeft, "Pista de deslizar", "Recordatorio en la Agenda") {
                        Switch(ajustes.pistaGestos, { v -> acciones.cambiar { it.copy(pistaGestos = v) } })
                    }
                }
            )
        )

        CabeceraSeccion("Correo de la empresa")
        Grupo(
            listOf { f ->
                FilaSimple(
                    f, Icons.Rounded.Mail, "Correo → aviso",
                    estadoCorreo ?: "Llega en la próxima versión de la app nativa"
                )
            }
        )

        CabeceraSeccion("Datos")
        Grupo(
            listOf(
                { f -> FilaSimple(f, Icons.Rounded.Science, "Cargar avisos de ejemplo", "Seis avisos y dos técnicos para probar", alPulsar = acciones.ejemplo) },
                { f -> FilaSimple(f, Icons.Rounded.DeleteSweep, "Borrar todos los avisos", "${avisos.size} guardados en este móvil", alPulsar = { borrar = true }) }
            )
        )

        CabeceraSeccion("Acerca de")
        Grupo(listOf { f -> FilaSimple(f, Icons.Rounded.Info, "Avisos ${BuildConfig.VERSION_NAME}", "App nativa de Android") })
        androidx.compose.foundation.layout.Spacer(Modifier.height(EspacioMenu))
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
