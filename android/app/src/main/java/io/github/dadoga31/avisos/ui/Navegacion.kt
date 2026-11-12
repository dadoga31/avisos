package io.github.dadoga31.avisos.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Reglas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Rutas: agenda, hechos, equipo, ajustes (con menú) y aviso/{id}, nuevo, editar/{id}. */
@OptIn(ExperimentalSharedTransitionApi::class)
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

    /* Cómo va el buzón, para el punto de la Agenda: se mira al minuto. */
    val correo by produceState<Boolean?>(null, ahora) {
        value = withContext(Dispatchers.IO) {
            runCatching { leerEstado(contexto) }.getOrNull()?.takeIf { it.configurada }?.let { it.error.isEmpty() }
        }
    }

    MarcoSeccion(
        actual = seccion ?: Seccion.AGENDA,
        alIr = ::irASeccion,
        alNuevo = { nav.navigate("nuevo") },
        snackbar = snackbar,
        mostrarMenu = seccion != null
    ) {
      SharedTransitionLayout {
       CompositionLocalProvider(LocalCompartido provides this) {
        NavHost(
            nav, startDestination = "agenda",
            /* Entre secciones: fundido con un zoom mínimo. */
            enterTransition = { fadeIn(tween(240)) + scaleIn(tween(320), initialScale = 0.985f) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) + scaleIn(tween(320), initialScale = 0.985f) },
            popExitTransition = { fadeOut(tween(160)) }
        ) {
            destino("agenda") {
                AgendaPantalla(
                    avisos, tecnicos, ajustes, ahora,
                    AccionesAgenda(
                        abrir = { nav.navigate("aviso/$it") },
                        nuevo = { nav.navigate("nuevo") },
                        cambiarEstado = ::cambiar,
                        llamar = { llamar(contexto, it) },
                        irA = { mapa(contexto, it) },
                        ejemplo = { vm.datosDeEjemplo() },
                        ocultarPista = { vm.cambiarAjustes { it.copy(pistaGestos = false) } },
                        correo = { nav.navigate("correo") }
                    ),
                    correo = correo
                )
            }
            destino("hechos") { HechosPantalla(avisos, tecnicos, ahora) { nav.navigate("aviso/$it") } }
            destino("equipo") {
                EquipoPantalla(
                    avisos, tecnicos, ahora,
                    alAbrir = { nav.navigate("aviso/$it") },
                    alLlamar = { llamar(contexto, it) },
                    alGuardar = { n, t, id -> vm.guardarTecnico(n, t, id) },
                    alBorrar = { vm.borrarTecnico(it) }
                )
            }
            destino("ajustes") {
                val buzon by produceState<EstadoBuzon?>(null) { value = withContext(Dispatchers.IO) { runCatching { leerEstado(contexto) }.getOrNull() } }
                val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    uri?.let { vm.exportar(it) }
                }
                val recuperar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    uri?.let { vm.importar(it) }
                }
                AjustesPantalla(
                    ajustes, avisos, buzon,
                    AccionesAjustes(
                        cambiar = { vm.cambiarAjustes(it) },
                        abrirCorreo = { nav.navigate("correo") },
                        guardarCopia = { guardar.launch("avisos-$hoy.json") },
                        recuperarCopia = { recuperar.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        ejemplo = { vm.datosDeEjemplo(); irASeccion(Seccion.AGENDA) },
                        borrarTodo = { vm.borrarTodos() }
                    )
                )
            }
            /* La ficha entra con fundido mientras su título viaja desde la fila. */
            destino(
                "aviso/{id}",
                entrar = { fadeIn(tween(300)) + slideInVertically(Muelles.suave()) { it / 14 } },
                salir = { fadeOut(tween(220)) + slideOutVertically(Muelles.suave()) { it / 14 } }
            ) { e ->
                val id = e.arguments?.getString("id")
                val a = avisos.firstOrNull { it.id == id }
                if (a == null) {
                    if (cargado) LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    var temporal by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Uri?>(null) }
                    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                        val u = temporal
                        if (ok && u != null) vm.anadirFoto(a.id, u, deCamara = true)
                    }
                    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
                        u?.let { vm.anadirFoto(a.id, it, deCamara = false) }
                    }
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
                            abrirAdjunto = { abrirAdjunto(contexto, it) },
                            borrarAdjunto = { vm.borrarAdjunto(a.id, it.id) },
                            camara = {
                                val f = java.io.File(contexto.cacheDir, "camara/foto-${System.currentTimeMillis()}.jpg")
                                f.parentFile?.mkdirs()
                                val u = androidx.core.content.FileProvider.getUriForFile(contexto, contexto.packageName + ".archivos", f)
                                temporal = u
                                try { camara.launch(u) } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(contexto, "No hay app de cámara", Toast.LENGTH_SHORT).show()
                                }
                            },
                            galeria = {
                                galeria.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    )
                }
            }
            destino("correo", entrar = entrarLado, salir = salirLado) { CorreoRuta(vm, ajustes.correoIgnorados) { nav.popBackStack() } }
            destino("nuevo", entrar = entrarLado, salir = salirLado) {
                val inicial = remember { vm.nuevo() }
                FormularioPantalla(
                    inicial, esNuevo = true, tecnicos = tecnicos, clientes = Reglas.clientesConocidos(avisos), hoy = hoy,
                    ref = remember { Reglas.siguienteRef(ajustes.prefijoRef, ajustes.contadorRef, avisos).first },
                    alGuardar = { a ->
                        vm.guardar(a) { g ->
                            nav.popBackStack()
                            nav.navigate("aviso/${g.id}")
                        }
                    },
                    alCerrar = { nav.popBackStack() }
                )
            }
            destino("editar/{id}", entrar = entrarLado, salir = salirLado) { e ->
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
    }
}

private val entrarLado: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideInHorizontally(Muelles.suave()) { it / 3 } + fadeIn(tween(220)) }
private val salirLado: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutHorizontally(Muelles.suave()) { it / 3 } + fadeOut(tween(180)) }

/**
 * Un destino de la navegación que pasa a sus pantallas el ámbito de la
 * animación (para la ficha que crece desde la fila) y el momento en que se
 * abrió (para la entrada escalonada). [salir] es la salida al volver atrás.
 */
private fun NavGraphBuilder.destino(
    ruta: String,
    entrar: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    salir: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    contenido: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable(ruta, enterTransition = entrar, popExitTransition = salir) { e ->
    val inicio = remember { System.currentTimeMillis() }
    CompositionLocalProvider(LocalAnimado provides this, LocalInicioPantalla provides inicio) { contenido(e) }
}

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
