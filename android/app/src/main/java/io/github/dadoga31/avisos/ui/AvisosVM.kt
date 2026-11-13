package io.github.dadoga31.avisos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dadoga31.avisos.Actualizador
import io.github.dadoga31.avisos.AvisosApp
import io.github.dadoga31.avisos.EstadoActualizacion
import io.github.dadoga31.avisos.datos.Ajustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Tecnico
import io.github.dadoga31.avisos.correo.AlmacenCorreo
import io.github.dadoga31.avisos.correo.ProcesadorCorreo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AvisosVM(app: Application) : AndroidViewModel(app) {
    private val avisosApp = app as AvisosApp
    private val repo = avisosApp.repositorio
    private val almacen = avisosApp.ajustes

    /** false hasta que la base de datos contesta por primera vez. */
    val cargado = MutableStateFlow(false)

    val avisos: StateFlow<List<Aviso>> = repo.avisos
        .onEach { cargado.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tecnicos: StateFlow<List<Tecnico>> = repo.tecnicos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ajustes: StateFlow<Ajustes> = almacen.flujo

    /** La hora, al minuto, para «en 1 h 19 min» y para que cambie el día. */
    val ahora: StateFlow<LocalDateTime> = flow {
        while (true) {
            val t = LocalDateTime.now()
            emit(t)
            delay((60 - t.second) * 1000L)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDateTime.now())

    /** Avisos de abajo que no vienen de un gesto («2 avisos nuevos desde el correo»). */
    val mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)

    init {
        viewModelScope.launch {
            repo.asegurarYo()
            repo.avisarCambio()
        }
        procesarCorreo()
        /* La versión nueva se mira en onResume (alVolver): aquí aún no existe su estado. */
    }

    // ---------------------------------------------------------------- actualizaciones

    /** Si hay versión nueva y cómo va su descarga: la franja de la Agenda y Ajustes. */
    val actualizacion = MutableStateFlow<EstadoActualizacion>(EstadoActualizacion.Nada)
    private var ultimaConsulta = 0L
    private var esperandoPermiso = false

    /**
     * Mira si hay versión nueva (al abrir y al volver a la app, como mucho
     * cada 30 min salvo que se pida). [descargar]: si la hay, la baja ya
     * (al tocar la notificación). [avisarSiNo]: dice «ya tienes la última».
     */
    fun buscarActualizacion(forzar: Boolean = false, descargar: Boolean = false, avisarSiNo: Boolean = false) {
        val estado = actualizacion.value
        if (estado is EstadoActualizacion.Descargando) return
        if (estado is EstadoActualizacion.Lista) { if (descargar) actualizar(); return }
        val ahora = System.currentTimeMillis()
        if (!forzar && ahora - ultimaConsulta < 30 * 60_000L) return
        ultimaConsulta = ahora
        viewModelScope.launch(Dispatchers.IO) {
            val v = Actualizador.consultar()
            when {
                v != null && Actualizador.hayNueva(v) -> {
                    actualizacion.value = EstadoActualizacion.Disponible(v)
                    if (descargar) actualizar()
                }
                avisarSiNo -> mensajes.tryEmit(if (v == null) "No se pudo mirar si hay versión nueva" else "Tienes la última versión")
            }
        }
    }

    /** El botón de la franja: descarga, y al terminar abre el instalador. */
    fun actualizar() {
        when (val e = actualizacion.value) {
            is EstadoActualizacion.Lista -> instalar(e)
            is EstadoActualizacion.Disponible -> descargar(e.version)
            is EstadoActualizacion.Fallo -> descargar(e.version)
            else -> Unit
        }
    }

    private fun descargar(v: Actualizador.Version) {
        actualizacion.value = EstadoActualizacion.Descargando(v, 0f)
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching { Actualizador.descargar(avisosApp) { p -> actualizacion.value = EstadoActualizacion.Descargando(v, p) } }
            actualizacion.value = r.fold(
                { EstadoActualizacion.Lista(v, it) },
                { EstadoActualizacion.Fallo(v, if (it is java.io.IOException) "Sin conexión" else it.message ?: "No se pudo descargar") }
            )
            (actualizacion.value as? EstadoActualizacion.Lista)?.let { instalar(it) }
        }
    }

    private fun instalar(e: EstadoActualizacion.Lista) {
        if (!Actualizador.puedeInstalar(avisosApp)) {
            /* La primera vez: Android pide permitir que Avisos instale apps. Al volver se instala. */
            esperandoPermiso = true
            mensajes.tryEmit("Permite instalar apps de Avisos y vuelve atrás")
            runCatching { avisosApp.startActivity(Actualizador.pedirPermiso(avisosApp)) }
            return
        }
        runCatching { Actualizador.instalar(avisosApp, e.apk) }
            .onFailure { actualizacion.value = EstadoActualizacion.Fallo(e.version, "No se pudo abrir el instalador") }
    }

    /** Al volver a la app: si venía de dar el permiso, sigue con la instalación. */
    fun alVolver() {
        Actualizador.quitarNotificacion(avisosApp)
        val e = actualizacion.value
        if (esperandoPermiso && e is EstadoActualizacion.Lista && Actualizador.puedeInstalar(avisosApp)) {
            esperandoPermiso = false
            instalar(e)
        } else buscarActualizacion()
    }

    /** Convierte los correos que esperan (al abrir la app y al volver a ella). */
    fun procesarCorreo() = viewModelScope.launch(Dispatchers.IO) {
        val r = runCatching {
            ProcesadorCorreo.procesar(avisosApp, repo, AlmacenCorreo(avisosApp), almacen.actual.correoIgnorados)
        }.getOrNull() ?: return@launch
        val n = r.creados.size
        if (n > 0) mensajes.tryEmit(if (n == 1) "1 aviso nuevo desde el correo" else "$n avisos nuevos desde el correo")
    }

    fun nuevo(): Aviso = repo.nuevo()

    suspend fun porId(id: String) = repo.porId(id)

    fun guardar(aviso: Aviso, luego: (Aviso) -> Unit = {}) = viewModelScope.launch { luego(repo.guardar(aviso)) }

    /** Cambia el estado; [luego] recibe el aviso como estaba, para deshacer. */
    fun cambiarEstado(id: String, estado: Estado, luego: (Aviso) -> Unit = {}) = viewModelScope.launch {
        repo.cambiarEstado(id, estado)?.let(luego)
    }

    fun restaurar(aviso: Aviso) = viewModelScope.launch { repo.restaurar(aviso) }
    fun borrar(id: String) = viewModelScope.launch { repo.borrar(id) }
    fun duplicar(id: String, luego: (Aviso) -> Unit) = viewModelScope.launch { repo.duplicar(id)?.let(luego) }
    fun anadirNota(id: String, texto: String) = viewModelScope.launch { repo.anadirNota(id, texto) }
    fun borrarNota(id: String, notaId: String) = viewModelScope.launch { repo.borrarNota(id, notaId) }
    /** Foto de la cámara o la galería: se reduce, se endereza y se guarda con el aviso. */
    fun anadirFoto(id: String, uri: android.net.Uri, deCamara: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val adj = runCatching { io.github.dadoga31.avisos.Fotos.guardar(avisosApp, id, uri) }.getOrNull()
        if (adj != null) repo.anadirAdjunto(id, adj) else mensajes.tryEmit("No se pudo leer la foto")
        if (deCamara) java.io.File(avisosApp.cacheDir, "camara").listFiles()?.forEach { it.delete() }
    }

    fun borrarAdjunto(id: String, adjuntoId: String) = viewModelScope.launch { repo.borrarAdjunto(id, adjuntoId) }

    /**
     * Manda un correo del aviso (responder o reenviar). Si va bien, lo apunta
     * en el registro del aviso. [luego] recibe el error, o null si se envió.
     */
    fun enviarCorreo(avisoId: String, envio: io.github.dadoga31.avisos.correo.Envio, nota: String, luego: (String?) -> Unit) =
        viewModelScope.launch {
            val error = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val c = runCatching { io.github.dadoga31.avisos.correo.Cuenta.cargar(avisosApp) }.getOrNull()
                if (c == null || !c.envioValido()) return@withContext "Falta configurar el envío en Ajustes → Correo"
                try {
                    io.github.dadoga31.avisos.correo.EnviadorCorreo.enviar(c, envio)?.let { mensajes.tryEmit(it) }
                    repo.anadirNota(avisoId, nota)
                    null
                } catch (e: Exception) {
                    io.github.dadoga31.avisos.correo.EnviadorCorreo.explicar(e)
                }
            }
            luego(error)
        }

    fun sumarHoras(id: String, delta: Double) = viewModelScope.launch { repo.sumarHoras(id, delta) }

    fun guardarTecnico(nombre: String, telefono: String, id: String?) =
        viewModelScope.launch { repo.guardarTecnico(nombre, telefono, id) }

    fun borrarTecnico(id: String) = viewModelScope.launch { repo.borrarTecnico(id) }
    fun borrarTodos() = viewModelScope.launch { repo.borrarTodos() }

    /** Guarda la copia en el archivo que eligió el usuario (Drive, Descargas…). */
    fun exportar(uri: android.net.Uri) = viewModelScope.launch(Dispatchers.IO) {
        val texto = runCatching {
            val json = repo.exportar()
            avisosApp.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            val n = avisos.value.size
            if (n == 1) "Copia guardada: 1 aviso" else "Copia guardada: $n avisos"
        }.getOrElse { "No se pudo guardar la copia: ${it.message ?: "error"}" }
        mensajes.tryEmit(texto)
    }

    /** Recupera una copia (de esta app o de la web): añade lo que falte. */
    fun importar(uri: android.net.Uri) = viewModelScope.launch(Dispatchers.IO) {
        val texto = runCatching {
            val json = avisosApp.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            val r = repo.importar(io.github.dadoga31.avisos.datos.CopiaSeguridad.leer(json))
            if (r.avisos == 0 && r.tecnicos == 0) "La copia no trae nada nuevo: todo estaba ya"
            else buildList {
                add(if (r.avisos == 1) "1 aviso" else "${r.avisos} avisos")
                if (r.tecnicos > 0) add(if (r.tecnicos == 1) "1 técnico" else "${r.tecnicos} técnicos")
                if (r.ficheros > 0) add(if (r.ficheros == 1) "1 archivo" else "${r.ficheros} archivos")
            }.joinToString(", ", prefix = "Recuperados: ")
        }.getOrElse { if (it is IllegalArgumentException) it.message ?: "Copia no válida" else "No se pudo leer la copia: ${it.message ?: "error"}" }
        mensajes.tryEmit(texto)
    }
    fun datosDeEjemplo() = viewModelScope.launch { repo.datosDeEjemplo() }
    fun cambiarAjustes(f: (Ajustes) -> Ajustes) {
        val antes = almacen.actual.recordatorio
        almacen.cambiar(f)
        /* Otra antelación: las alarmas se vuelven a calcular. */
        if (almacen.actual.recordatorio != antes) viewModelScope.launch { repo.avisarCambio() }
    }
}
