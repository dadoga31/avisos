package io.github.dadoga31.avisos.datos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Todo lo que cambia datos pasa por aquí: asigna referencias, fechas de
 * cierre y de inicio, y avisa al widget. Las reglas son las de la web.
 */
class Repositorio(
    private val dao: AvisosDao,
    private val ajustes: AlmacenAjustes,
    private val reloj: Clock = Clock.systemDefaultZone(),
    /** filesDir de la app: ahí van los adjuntos, en adjuntos/<id del aviso>/. */
    private val carpeta: File? = null,
    /** Se llama tras cada cambio (para el widget). */
    private val alCambiar: suspend (List<Aviso>) -> Unit = {}
) {
    val avisos: Flow<List<Aviso>> = dao.todos()
    val tecnicos: Flow<List<Tecnico>> = dao.tecnicos()
    private val cerrojo = Mutex()

    private fun ahora() = Instant.now(reloj).toString()
    fun hoy(): LocalDate = LocalDate.now(reloj)

    suspend fun porId(id: String) = dao.porId(id)

    fun nuevo(): Aviso = Aviso(
        id = nuevoId("av"), fecha = hoy().toString(), asignadoA = Tecnico.YO
    )

    /** Guarda con las reglas: referencia, creado, cerrado y empezado. */
    suspend fun guardar(aviso: Aviso): Aviso = cerrojo.withLock {
        val antes = dao.porId(aviso.id)
        var a = aviso.copy(actualizado = ahora())
        if (a.creado.isEmpty()) a = a.copy(creado = antes?.creado?.ifEmpty { null } ?: ahora())
        if (a.ref.isEmpty()) {
            val (ref, n) = Reglas.siguienteRef(ajustes.actual.prefijoRef, ajustes.actual.contadorRef, dao.todosAhora())
            a = a.copy(ref = ref)
            ajustes.cambiar { it.copy(contadorRef = n) }
        }
        a = when {
            !a.abierto && a.cerrado.isEmpty() -> a.copy(cerrado = ahora())
            a.abierto -> a.copy(cerrado = "")
            else -> a
        }
        if (a.estadoE == Estado.EN_CURSO && antes?.estadoE != Estado.EN_CURSO) a = a.copy(empezado = ahora())
        dao.guardar(a)
        avisarCambio()
        a
    }

    /** Cambia el estado y devuelve cómo estaba, para poder deshacer. */
    suspend fun cambiarEstado(id: String, estado: Estado): Aviso? {
        val antes = dao.porId(id) ?: return null
        if (antes.estadoE == estado) return antes
        guardar(antes.copy(estado = estado.id))
        return antes
    }

    /** Deshacer: vuelve a dejar el aviso exactamente como estaba. */
    suspend fun restaurar(aviso: Aviso) {
        dao.guardar(aviso)
        avisarCambio()
    }

    suspend fun borrar(id: String) {
        dao.porId(id)?.let { dao.borrar(it) }
        carpeta?.let { File(it, "adjuntos/$id").deleteRecursively() }
        avisarCambio()
    }

    suspend fun borrarTodos() {
        dao.borrarTodos()
        carpeta?.let { File(it, "adjuntos").deleteRecursively() }
        avisarCambio()
    }

    // ---------- copia de seguridad ----------

    data class Recuperado(val avisos: Int, val tecnicos: Int, val ficheros: Int)

    /** La copia entera en el formato «avisos-backup», con los adjuntos dentro. */
    suspend fun exportar(): String = CopiaSeguridad.exportar(dao.todosAhora(), dao.tecnicosAhora(), ajustes.actual, leer = { adj ->
        carpeta?.let { File(it, adj.ruta) }?.takeIf { it.exists() }?.readBytes()
    })

    /**
     * Añade lo que falte de una copia (nativa o de la web): los avisos y
     * técnicos que ya están no se tocan. La numeración sigue por la más alta.
     */
    suspend fun importar(c: ContenidoCopia): Recuperado {
        val r = cerrojo.withLock {
            val idsA = dao.todosAhora().map { it.id }.toSet()
            val idsT = dao.tecnicosAhora().map { it.id }.toSet()
            val nuevosT = c.tecnicos.filter { it.id !in idsT }
            nuevosT.forEach { dao.guardarTecnico(it) }
            val conocidos = idsT + nuevosT.map { it.id } + Tecnico.YO
            val porAviso = c.ficheros.groupBy { it.avisoId }
            var ficheros = 0
            val nuevosA = c.avisos.filter { it.id !in idsA }.map { a ->
                var b = if (a.asignadoA.isNotEmpty() && a.asignadoA !in conocidos) a.copy(asignadoA = "") else a
                val fs = porAviso[a.id].orEmpty()
                if (carpeta != null && fs.isNotEmpty()) {
                    val dir = File(carpeta, "adjuntos/${a.id}").apply { mkdirs() }
                    b = b.copy(adjuntos = fs.mapIndexed { i, f ->
                        val nombre = f.nombre.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        val destino = File(dir, "$i-$nombre").apply { writeBytes(f.datos) }
                        ficheros++
                        Adjunto(f.id.ifEmpty { nuevoId("f") }, nombre, f.mime, destino.length(), "adjuntos/${a.id}/${destino.name}")
                    })
                }
                b
            }
            dao.guardarVarios(nuevosA)
            val (_, siguiente) = Reglas.siguienteRef(ajustes.actual.prefijoRef, maxOf(ajustes.actual.contadorRef, c.contadorRef), dao.todosAhora())
            ajustes.cambiar { it.copy(contadorRef = siguiente - 1, correoIgnorados = (it.correoIgnorados + c.correoIgnorados).distinct()) }
            Recuperado(nuevosA.size, nuevosT.size, ficheros)
        }
        avisarCambio()
        return r
    }

    /** ¿Ya hay un aviso hecho con este correo? */
    suspend fun yaConvertido(claves: List<String>) = claves.isNotEmpty() && dao.cuantosConCorreo(claves) > 0

    suspend fun todos(): List<Aviso> = dao.todosAhora()

    /**
     * Una respuesta del mismo hilo: se apunta en el registro del aviso, con
     * sus adjuntos, y sus claves quedan en el hilo para reconocer las
     * siguientes. El estado no se toca (un «gracias» no reabre un aviso hecho).
     */
    suspend fun anadirRespuesta(id: String, nota: String, momento: String, claves: List<String>, adjuntos: List<Adjunto>): Aviso? {
        val a = dao.porId(id) ?: return null
        val n = Nota(nuevoId("n"), momento.ifBlank { ahora() }, nota)
        return guardar(a.copy(notas = listOf(n) + a.notas, adjuntos = a.adjuntos + adjuntos, hilo = (a.hilo + claves).distinct().takeLast(60)))
    }

    /** Lo que se manda desde el aviso entra en su hilo: si contestan a eso, también se reconoce. */
    suspend fun anadirAlHilo(id: String, claves: List<String>) {
        val a = dao.porId(id) ?: return
        guardar(a.copy(hilo = (a.hilo + claves).distinct().takeLast(60)))
    }

    suspend fun duplicar(id: String): Aviso? {
        val src = dao.porId(id) ?: return null
        return guardar(
            src.copy(
                id = nuevoId("av"), ref = "", estado = Estado.PENDIENTE.id, notas = emptyList(), horas = 0.0,
                cerrado = "", empezado = "", creado = "", fecha = hoy().toString(), titulo = src.titulo + " (copia)",
                correoId = "", origen = "manual", adjuntos = emptyList()
            )
        )
    }

    suspend fun anadirNota(id: String, texto: String) {
        val a = dao.porId(id) ?: return
        if (texto.isBlank()) return
        guardar(a.copy(notas = listOf(Nota(nuevoId("n"), ahora(), texto.trim())) + a.notas))
    }

    suspend fun borrarNota(id: String, notaId: String) {
        val a = dao.porId(id) ?: return
        guardar(a.copy(notas = a.notas.filterNot { it.id == notaId }))
    }

    suspend fun anadirAdjunto(id: String, adjunto: Adjunto) {
        val a = dao.porId(id) ?: return
        guardar(a.copy(adjuntos = a.adjuntos + adjunto))
    }

    suspend fun borrarAdjunto(id: String, adjuntoId: String) {
        val a = dao.porId(id) ?: return
        val adj = a.adjuntos.firstOrNull { it.id == adjuntoId } ?: return
        carpeta?.let { File(it, adj.ruta).delete() }
        guardar(a.copy(adjuntos = a.adjuntos - adj))
    }

    suspend fun sumarHoras(id: String, delta: Double) {
        val a = dao.porId(id) ?: return
        guardar(a.copy(horas = maxOf(0.0, a.horas + delta)))
    }

    // ---------- técnicos ----------

    suspend fun asegurarYo() {
        if (dao.tecnicosAhora().none { it.id == Tecnico.YO }) dao.guardarTecnico(Tecnico(Tecnico.YO, "Yo", color = 0))
    }

    suspend fun guardarTecnico(nombre: String, telefono: String, id: String? = null) {
        val todos = dao.tecnicosAhora()
        val previo = todos.firstOrNull { it.id == id }
        dao.guardarTecnico(
            Tecnico(
                id = previo?.id ?: nuevoId("tc"), nombre = nombre.trim(), telefono = telefono.trim(),
                color = previo?.color ?: (todos.size % 8)
            )
        )
    }

    suspend fun borrarTecnico(id: String) {
        if (id == Tecnico.YO) return
        dao.tecnicosAhora().firstOrNull { it.id == id }?.let {
            dao.desasignar(id)
            dao.borrarTecnico(it)
            avisarCambio()
        }
    }

    // ---------- datos de ejemplo (los mismos que la web) ----------

    suspend fun datosDeEjemplo() {
        asegurarYo()
        val marcos = Tecnico(nuevoId("tc"), "Marcos Rey", "600112233", 1)
        val laura = Tecnico(nuevoId("tc"), "Laura Pinto", "600445566", 2)
        dao.guardarTecnico(marcos)
        dao.guardarTecnico(laura)
        val h = hoy()
        val ahora = ahora()
        fun hace(segundos: Long) = Instant.now(reloj).minusSeconds(segundos).toString()
        fun av(
            titulo: String, tipo: Tipo, sistema: Sistema, prioridad: Prioridad, cliente: Cliente,
            fecha: LocalDate, hora: String, estado: Estado, asignado: String, descripcion: String
        ) = Aviso(
            id = nuevoId("av"), titulo = titulo, tipo = tipo.id, sistema = sistema.id, prioridad = prioridad.id,
            cliente = cliente, fecha = fecha.toString(), hora = hora, estado = estado.id, asignadoA = asignado,
            descripcion = descripcion, creado = hace(26 * 3600), actualizado = ahora
        )
        val lista = listOf(
            av("Central de alarma en fallo de comunicación", Tipo.AVERIA, Sistema.ALARMA, Prioridad.URGENTE,
                Cliente("Farmacia Centro", "C/ Mayor 14, Bajo", "911234567", "Ana"), h.minusDays(1), "09:30",
                Estado.PENDIENTE, Tecnico.YO, "La central pierde conexión con la CRA por la noche. Revisar módulo GPRS y cableado."),
            av("Sustitución de 2 cámaras exteriores", Tipo.INSTALACION, Sistema.CCTV, Prioridad.ALTA,
                Cliente("Naves Logísticas Sur", "Pol. Ind. El Prado, nave 7", "915556677", "Jefe de planta"), h, "11:30",
                Estado.PROGRAMADO, marcos.id, "Cambio de dos domos por bullet 4MP. Llevar escalera y latiguillos.").copy(duracion = "1,5"),
            av("Mantenimiento anual del sistema de incendios", Tipo.MANTENIMIENTO, Sistema.INCENDIOS, Prioridad.NORMAL,
                Cliente("Colegio San Pablo", "Av. de la Paz 3", "913334455", "Conserjería"), h, "09:00",
                Estado.EN_CURSO, Tecnico.YO, "Revisión de detectores y pulsadores. Firmar parte al terminar.").copy(
                empezado = hace(41 * 60 + 27), duracion = "2", horas = 0.5,
                notas = listOf(Nota(nuevoId("n"), hace(6 * 60), "Revisada la primera planta, faltan sótano y azotea."))
            ),
            av("Revisión de CRA", Tipo.REVISION, Sistema.ALARMA, Prioridad.NORMAL,
                Cliente("Joyería Duarte", "Plaza del Sol 1", "912223344", "Sr. Duarte"), h, "08:00",
                Estado.RESUELTO, Tecnico.YO, "Prueba de comunicación con la central receptora.").copy(
                empezado = hace(101 * 60), cerrado = hace(59 * 60), horas = 0.5
            ),
            av("Lector de acceso no lee tarjetas", Tipo.AVERIA, Sistema.ACCESOS, Prioridad.ALTA,
                Cliente("Gimnasio Atlas", "C/ Deportes 22", "917778899", "Recepción"), h.plusDays(1), "08:00",
                Estado.PROGRAMADO, laura.id, "Lector de la puerta principal intermitente. Posible fuente o cableado Wiegand."),
            av("Presupuesto de videoportero para 12 viviendas", Tipo.PRESUPUESTO, Sistema.INTERFONO, Prioridad.BAJA,
                Cliente("Comunidad C/ Olmo 8", "C/ Olmo 8", "600998877", "Presidente"), h.plusDays(4), "",
                Estado.PENDIENTE, "", "Tomar medidas del portal y pasar oferta con dos opciones."),
            av("Ampliación de 4 detectores volumétricos", Tipo.INSTALACION, Sistema.ALARMA, Prioridad.NORMAL,
                Cliente("Joyería Duarte", "Plaza del Sol 1", "912223344", "Sr. Duarte"), h.minusDays(6), "10:00",
                Estado.RESUELTO, marcos.id, "Ampliación realizada y probada con la CRA.").copy(
                cerrado = hace(3 * 24 * 3600), horas = 3.5,
                notas = listOf(Nota(nuevoId("n"), ahora, "Trabajo terminado y firmado por el cliente."))
            )
        )
        cerrojo.withLock {
            var contador = ajustes.actual.contadorRef
            val existentes = dao.todosAhora()
            val conRef = lista.map { a ->
                val (ref, n) = Reglas.siguienteRef(ajustes.actual.prefijoRef, contador, existentes)
                contador = n
                a.copy(ref = ref)
            }
            dao.guardarVarios(conRef)
            ajustes.cambiar { it.copy(contadorRef = contador) }
        }
        avisarCambio()
    }

    suspend fun avisarCambio() = alCambiar(dao.todosAhora())

    companion object {
        fun nuevoId(prefijo: String) = prefijo + "_" + UUID.randomUUID().toString().replace("-", "").take(16)
    }
}
