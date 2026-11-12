package io.github.dadoga31.avisos.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Rutas: agenda, hechos, equipo, ajustes (con menú) y aviso/{id}, nuevo, editar/{id}. */
@Composable
fun AppAvisos(vm: AvisosVM, rutaPedida: String?, alConsumirRuta: () -> Unit) {
    val avisos by vm.avisos.collectAsStateWithLifecycle()
    val tecnicos by vm.tecnicos.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val ahora by vm.ahora.collectAsStateWithLifecycle()
    val cargado by vm.cargado.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contexto = LocalContext.current
    val entrada by nav.currentBackStackEntryAsState()
    val seccion = Seccion.de(entrada?.destination?.route)
    var nuevoTecnico by remember { mutableStateOf(false) }
    val hoy = ahora.toLocalDate()

    fun irASeccion(s: Seccion) = nav.navigate(s.ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    LaunchedEffect(rutaPedida) {
        when (rutaPedida) {
            null -> return@LaunchedEffect
            "nuevo" -> nav.navigate("nuevo")
            else -> if (rutaPedida.startsWith("aviso/")) nav.navigate(rutaPedida)
            else Seccion.de(rutaPedida)?.let { irASeccion(it) }
        }
        alConsumirRuta()
    }

    LaunchedEffect(Unit) { vm.mensajes.collect { snackbar.showSnackbar(it) } }

    /* Cambiar de estado siempre se puede deshacer. */
    fun cambiar(a: Aviso, e: Estado) {
        vm.cambiarEstado(a.id, e) { antes ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val r = snackbar.showSnackbar("${antes.ref} · ${e.nombre}", "Deshacer", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) vm.restaurar(antes)
            }
        }
    }

    MarcoSeccion(
        actual = seccion ?: Seccion.AGENDA,
        alIr = ::irASeccion,
        avisoAgenda = Reglas.resumen(avisos, hoy).vencidos,
        boton = when (seccion) {
            Seccion.AGENDA -> BotonFlotante(Icons.Rounded.Add, "Nuevo aviso") { nav.navigate("nuevo") }
            Seccion.EQUIPO -> BotonFlotante(Icons.Rounded.PersonAdd, "Añadir técnico") { nuevoTecnico = true }
            else -> null
        },
        snackbar = snackbar,
        mostrarMenu = seccion != null
    ) {
        NavHost(
            nav, startDestination = "agenda",
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }
        ) {
            composable("agenda") {
                AgendaPantalla(
                    avisos, tecnicos, ajustes, ahora,
                    AccionesAgenda(
                        abrir = { nav.navigate("aviso/$it") },
                        nuevo = { nav.navigate("nuevo") },
                        cambiarEstado = ::cambiar,
                        llamar = { llamar(contexto, it) },
                        irA = { mapa(contexto, it) },
                        ejemplo = { vm.datosDeEjemplo() },
                        ocultarPista = { vm.cambiarAjustes { it.copy(pistaGestos = false) } }
                    )
                )
            }
            composable("hechos") { HechosPantalla(avisos, tecnicos, ahora) { nav.navigate("aviso/$it") } }
            composable("equipo") {
                EquipoPantalla(
                    avisos, tecnicos, ahora,
                    alAbrir = { nav.navigate("aviso/$it") },
                    alLlamar = { llamar(contexto, it) },
                    alGuardar = { n, t, id -> vm.guardarTecnico(n, t, id) },
                    alBorrar = { vm.borrarTecnico(it) },
                    nuevoAbierto = nuevoTecnico, alCerrarNuevo = { nuevoTecnico = false }
                )
            }
            composable("ajustes") {
                val correo by produceState<String?>(null) { value = withContext(Dispatchers.IO) { estadoCorreo(contexto) } }
                AjustesPantalla(
                    ajustes, avisos, correo,
                    AccionesAjustes(
                        cambiar = { vm.cambiarAjustes(it) },
                        abrirCorreo = { nav.navigate("correo") },
                        ejemplo = { vm.datosDeEjemplo(); irASeccion(Seccion.AGENDA) },
                        borrarTodo = { vm.borrarTodos() }
                    )
                )
            }
            composable(
                "aviso/{id}",
                enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() }
            ) { e ->
                val id = e.arguments?.getString("id")
                val a = avisos.firstOrNull { it.id == id }
                if (a == null) {
                    if (cargado) LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    FichaPantalla(
                        a, tecnicos, ahora,
                        AccionesFicha(
                            volver = { nav.popBackStack() },
                            editar = { nav.navigate("editar/${a.id}") },
                            estado = { cambiar(a, it) },
                            horas = { vm.sumarHoras(a.id, it) },
                            nota = { vm.anadirNota(a.id, it) },
                            borrarNota = { vm.borrarNota(a.id, it) },
                            fecha = { f, h -> vm.guardar(a.copy(fecha = f, hora = h)) },
                            asignar = { vm.guardar(a.copy(asignadoA = it)) },
                            duplicar = { vm.duplicar(a.id) { c -> nav.navigate("editar/${c.id}") } },
                            borrar = { nav.popBackStack(); vm.borrar(a.id) },
                            llamar = { llamar(contexto, it) },
                            whatsapp = { whatsapp(contexto, it) },
                            irA = { mapa(contexto, it) },
                            abrirAdjunto = { abrirAdjunto(contexto, it) }
                        )
                    )
                }
            }
            composable("correo") { CorreoRuta(vm, ajustes.correoIgnorados) { nav.popBackStack() } }
            composable("nuevo") {
                val inicial = remember { vm.nuevo() }
                FormularioPantalla(
                    inicial, esNuevo = true, tecnicos = tecnicos, clientes = Reglas.clientesConocidos(avisos), hoy = hoy,
                    alGuardar = { a ->
                        vm.guardar(a) { g ->
                            nav.popBackStack()
                            nav.navigate("aviso/${g.id}")
                        }
                    },
                    alCerrar = { nav.popBackStack() }
                )
            }
            composable("editar/{id}") { e ->
                val id = e.arguments?.getString("id")
                val inicial = remember(id, cargado) { avisos.firstOrNull { it.id == id } }
                if (inicial != null) {
                    FormularioPantalla(
                        inicial, esNuevo = false, tecnicos = tecnicos, clientes = Reglas.clientesConocidos(avisos), hoy = hoy,
                        alGuardar = { a -> vm.guardar(a) { nav.popBackStack() } },
                        alCerrar = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Una línea para Ajustes: la cuenta y cómo va. */
private fun estadoCorreo(contexto: Context): String? = runCatching {
    val e = leerEstado(contexto)
    if (!e.configurada) return null
    e.usuario + " · " + e.error.ifEmpty { if (e.activo) "escuchando" else "conectando" }
}.getOrNull()

/** Abre un adjunto con la app que el usuario tenga para ese tipo (visor de PDF, galería…). */
fun abrirAdjunto(contexto: Context, adj: io.github.dadoga31.avisos.datos.Adjunto) {
    val f = java.io.File(contexto.filesDir, adj.ruta)
    if (!f.exists()) {
        Toast.makeText(contexto, "El archivo ya no está en el móvil", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(contexto, contexto.packageName + ".archivos", f)
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, adj.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    abrir(contexto, Intent.createChooser(i, adj.nombre).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "No hay ninguna app para abrir este archivo")
}

private fun abrir(contexto: Context, intent: Intent, error: String) {
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, error, Toast.LENGTH_SHORT).show()
    }
}

fun llamar(contexto: Context, tel: String) {
    if (tel.isBlank()) return
    abrir(contexto, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel.filter { it.isDigit() || it == '+' })), "No hay app de teléfono")
}

fun whatsapp(contexto: Context, tel: String) {
    if (tel.isBlank()) return
    abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + Reglas.telefonoWhatsApp(tel))), "No se pudo abrir WhatsApp")
}

fun mapa(contexto: Context, direccion: String) {
    if (direccion.isBlank()) return
    abrir(contexto, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccion))), "No hay app de mapas")
}
