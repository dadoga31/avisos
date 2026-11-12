package io.github.dadoga31.avisos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dadoga31.avisos.AvisosApp
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
    fun sumarHoras(id: String, delta: Double) = viewModelScope.launch { repo.sumarHoras(id, delta) }

    fun guardarTecnico(nombre: String, telefono: String, id: String?) =
        viewModelScope.launch { repo.guardarTecnico(nombre, telefono, id) }

    fun borrarTecnico(id: String) = viewModelScope.launch { repo.borrarTecnico(id) }
    fun borrarTodos() = viewModelScope.launch { repo.borrarTodos() }
    fun datosDeEjemplo() = viewModelScope.launch { repo.datosDeEjemplo() }
    fun cambiarAjustes(f: (Ajustes) -> Ajustes) = almacen.cambiar(f)
}
