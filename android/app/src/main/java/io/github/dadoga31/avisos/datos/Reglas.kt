package io.github.dadoga31.avisos.datos

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/* Reglas de negocio sin Android: se prueban en la JVM (ReglasTest). Vienen
   de assets/js/store.js y de la Agenda de views.js. */
object Reglas {
    private val ES = Locale("es", "ES")

    fun vencido(a: Aviso, hoy: LocalDate) = a.abierto && a.fecha.isNotEmpty() && a.fecha < hoy.toString()

    /** Por fecha y hora (lo que no tiene va al final) y, a igualdad, lo más prioritario antes. */
    val porFecha: Comparator<Aviso> = compareBy<Aviso>(
        { it.fecha.ifEmpty { "9999-12-31" } + " " + it.hora.ifEmpty { "99:99" } },
        { -it.prioridadE.peso }
    )

    data class Grupo(val clave: String, val titulo: String, val avisos: List<Aviso>, val alerta: Boolean = false)

    /** Los grupos de la Agenda. Todo aviso abierto cae en uno (por eso existe «Más adelante»). */
    fun grupos(avisos: List<Aviso>, hoy: LocalDate): List<Grupo> {
        val abiertos = avisos.filter { it.abierto }.sortedWith(porFecha)
        val h = hoy.toString()
        val m = hoy.plusDays(1).toString()
        val semana = hoy.plusDays(7).toString()
        fun de(f: (Aviso) -> Boolean) = abiertos.filter(f)
        return listOf(
            Grupo("vencidos", "Vencidos", de { it.fecha.isNotEmpty() && it.fecha < h }, alerta = true),
            Grupo("hoy", "Hoy", de { it.fecha == h }),
            Grupo("manana", "Mañana", de { it.fecha == m }),
            Grupo("semana", "Próximos 7 días", de { it.fecha > m && it.fecha <= semana }),
            Grupo("despues", "Más adelante", de { it.fecha > semana }),
            Grupo("sinfecha", "Sin fecha", de { it.fecha.isEmpty() })
        ).filter { it.avisos.isNotEmpty() || it.clave == "hoy" }
    }

    enum class Momento { EN_CURSO, PROXIMO, PASADO, SIN_HORA }

    data class Siguiente(val aviso: Aviso, val momento: Momento, val minutos: Long = 0)

    /** El aviso que toca ahora: el que está en curso o, si no, el próximo de hoy. */
    fun siguiente(avisos: List<Aviso>, hoy: LocalDate, ahora: LocalTime): Siguiente? {
        val deHoy = avisos.filter { it.abierto && it.fecha == hoy.toString() }.sortedWith(porFecha)
        deHoy.firstOrNull { it.estadoE == Estado.EN_CURSO }?.let { return Siguiente(it, Momento.EN_CURSO) }
        val conHora = deHoy.filter { it.hora.isNotEmpty() }
        conHora.firstOrNull { hora(it) >= ahora.withSecond(0).withNano(0) }?.let {
            return Siguiente(it, Momento.PROXIMO, Duration.between(ahora, hora(it)).toMinutes())
        }
        conHora.lastOrNull()?.let { return Siguiente(it, Momento.PASADO, Duration.between(hora(it), ahora).toMinutes()) }
        return deHoy.firstOrNull()?.let { Siguiente(it, Momento.SIN_HORA) }
    }

    private fun hora(a: Aviso): LocalTime = runCatching { LocalTime.parse(a.hora) }.getOrDefault(LocalTime.MIDNIGHT)

    fun diaDe(iso: String, zona: ZoneId = ZoneId.systemDefault()): LocalDate? =
        runCatching { Instant.parse(iso).atZone(zona).toLocalDate() }.getOrNull()

    data class Progreso(val hechos: Int, val total: Int)

    /** Hechos hoy frente a lo que había para hoy. */
    fun progresoHoy(avisos: List<Aviso>, hoy: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Progreso {
        val hechos = avisos.count { it.estadoE == Estado.RESUELTO && diaDe(it.cerrado, zona) == hoy }
        val pendientes = avisos.count { it.abierto && it.fecha == hoy.toString() }
        return Progreso(hechos, hechos + pendientes)
    }

    data class Filtro(
        val texto: String = "",
        val vencidos: Boolean = false,
        val urgentes: Boolean = false,
        val sinAsignar: Boolean = false,
        val semana: Boolean = false,
        val tecnico: String = ""
    ) {
        val activo get() = texto.isNotBlank() || vencidos || urgentes || sinAsignar || semana || tecnico.isNotEmpty()
    }

    /** Resultados de buscar o filtrar: con texto entran también los cerrados. */
    fun filtrar(avisos: List<Aviso>, tecnicos: List<Tecnico>, f: Filtro, hoy: LocalDate): List<Aviso> {
        val q = normalizar(f.texto)
        val nombres = tecnicos.associate { it.id to it.nombre }
        val semana = hoy.plusDays(7).toString()
        return avisos.filter { a ->
            if (q.isEmpty() && !a.abierto) return@filter false
            if (f.vencidos && !vencido(a, hoy)) return@filter false
            if (f.urgentes && a.prioridadE != Prioridad.URGENTE) return@filter false
            if (f.sinAsignar && a.asignadoA.isNotEmpty()) return@filter false
            if (f.semana && (a.fecha.isEmpty() || a.fecha < hoy.toString() || a.fecha > semana)) return@filter false
            if (f.tecnico.isNotEmpty() && a.asignadoA != f.tecnico) return@filter false
            if (q.isNotEmpty()) {
                val heno = listOf(
                    a.ref, a.titulo, a.descripcion, a.cliente.nombre, a.cliente.direccion,
                    a.cliente.telefono, a.cliente.contacto, nombres[a.asignadoA] ?: "",
                    a.tipoE.nombre, a.sistemaE.nombre
                ).joinToString(" ")
                if (q.split(" ").any { it.isNotEmpty() && !normalizar(heno).contains(it) }) return@filter false
            }
            true
        }.sortedWith(compareBy<Aviso> { !it.abierto }.then(porFecha))
    }

    fun normalizar(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(ES), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ").trim()

    data class Resumen(
        val abiertos: Int, val vencidos: Int, val hoy: Int, val semana: Int,
        val sinAsignar: Int, val urgentes: Int, val hechosHoy: Int
    )

    fun resumen(avisos: List<Aviso>, hoy: LocalDate, zona: ZoneId = ZoneId.systemDefault()): Resumen {
        val abiertos = avisos.filter { it.abierto }
        val h = hoy.toString()
        val semana = hoy.plusDays(7).toString()
        return Resumen(
            abiertos = abiertos.size,
            vencidos = abiertos.count { it.fecha.isNotEmpty() && it.fecha < h },
            hoy = abiertos.count { it.fecha == h },
            semana = abiertos.count { it.fecha >= h && it.fecha <= semana },
            sinAsignar = abiertos.count { it.asignadoA.isEmpty() },
            urgentes = abiertos.count { it.prioridadE == Prioridad.URGENTE },
            hechosHoy = avisos.count { it.estadoE == Estado.RESUELTO && diaDe(it.cerrado, zona) == hoy }
        )
    }

    /** Siguiente referencia: el contador o el número más alto que haya, más uno. */
    fun siguienteRef(prefijo: String, contador: Int, avisos: List<Aviso>): Pair<String, Int> {
        val max = avisos.maxOfOrNull { Regex("(\\d+)\\s*$").find(it.ref)?.groupValues?.get(1)?.toIntOrNull() ?: 0 } ?: 0
        val n = maxOf(contador, max) + 1
        return "${prefijo.ifBlank { "AV" }}-${n.toString().padStart(4, '0')}" to n
    }

    /** Clientes que ya han salido en algún aviso, del más reciente al más antiguo, sin repetir. */
    fun clientesConocidos(avisos: List<Aviso>): List<Pair<Cliente, String>> =
        avisos.filter { it.cliente.nombre.isNotBlank() }
            .sortedByDescending { it.creado }
            .distinctBy { normalizar(it.cliente.nombre) }
            .map { it.cliente to it.ref }

    // ---------- recordatorios ----------

    /**
     * Cuándo salta el recordatorio de un aviso, o null si no lleva: tiene que
     * estar abierto y sin empezar, con fecha y hora, y ser mío o sin asignar
     * (los de otros técnicos no avisan en este móvil).
     */
    fun momentoRecordatorio(a: Aviso, minutos: Int, zona: ZoneId = ZoneId.systemDefault()): Instant? {
        if (minutos <= 0 || !a.abierto || a.estadoE == Estado.EN_CURSO) return null
        if (a.asignadoA.isNotEmpty() && a.asignadoA != Tecnico.YO) return null
        val cita = cita(a, zona) ?: return null
        return cita.minusSeconds(minutos * 60L)
    }

    /** Fecha y hora del aviso como instante, o null si le falta alguna. */
    fun cita(a: Aviso, zona: ZoneId = ZoneId.systemDefault()): Instant? {
        if (a.fecha.isEmpty() || a.hora.isEmpty()) return null
        val d = runCatching { LocalDate.parse(a.fecha) }.getOrNull() ?: return null
        val h = runCatching { LocalTime.parse(a.hora) }.getOrNull() ?: return null
        return d.atTime(h).atZone(zona).toInstant()
    }

    /** Los recordatorios que quedan por saltar, del más próximo al más lejano. */
    fun recordatoriosPendientes(avisos: List<Aviso>, minutos: Int, ahora: Instant, zona: ZoneId = ZoneId.systemDefault()) =
        avisos.mapNotNull { a -> momentoRecordatorio(a, minutos, zona)?.takeIf { it.isAfter(ahora) }?.let { a to it } }
            .sortedBy { it.second }

    /** Título y texto de la notificación: «En 20 min · Naves Logísticas Sur» y «11:00 · Sustitución…». */
    fun textoRecordatorio(a: Aviso, ahora: Instant, zona: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
        val cita = cita(a, zona)
        val faltan = cita?.let { Duration.between(ahora, it).toMinutes() }
        val cuando = when {
            faltan == null -> "Hoy"
            faltan <= 0 -> "Ahora"
            else -> "En " + minutos(faltan)
        }
        val quien = a.cliente.nombre.ifBlank { a.titulo }
        return "$cuando · $quien" to listOf(a.hora, a.titulo).filter { it.isNotBlank() }.joinToString(" · ")
    }

    // ---------- textos ----------

    fun horas(h: Double): String {
        val r = Math.round(h * 10) / 10.0
        val t = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().replace('.', ',')
        return "$t h"
    }

    fun minutos(m: Long): String = when {
        m < 1 -> "ahora"
        m < 60 -> "$m min"
        m % 60 == 0L -> "${m / 60} h"
        else -> "${m / 60} h ${m % 60} min"
    }

    /** «Hoy», «Mañana», «Ayer», «Lun 16», «16 dic». */
    fun dia(iso: String, hoy: LocalDate): String {
        val d = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return ""
        return when (d) {
            hoy -> "Hoy"
            hoy.plusDays(1) -> "Mañana"
            hoy.minusDays(1) -> "Ayer"
            else -> if (d.isAfter(hoy) && d.isBefore(hoy.plusDays(7)))
                d.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).replace(".", "").replaceFirstChar { it.uppercase() } + " ${d.dayOfMonth}"
            else "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, ES).replace(".", "")}" +
                if (d.year != hoy.year) " ${d.year}" else ""
        }
    }

    /** «jueves, 12 de noviembre». */
    fun fechaLarga(d: LocalDate): String =
        d.dayOfWeek.getDisplayName(TextStyle.FULL, ES) + ", " + d.dayOfMonth + " de " +
            d.month.getDisplayName(TextStyle.FULL, ES)

    /** Hora y día para una fila: «11:00», «Ayer 09:30», «Lun 16». */
    fun cuando(a: Aviso, hoy: LocalDate): String {
        val d = dia(a.fecha, hoy)
        return when {
            a.fecha.isEmpty() -> a.hora
            d == "Hoy" -> a.hora.ifEmpty { "Hoy" }
            a.hora.isEmpty() -> d
            else -> "$d ${a.hora}"
        }
    }

    /** Teléfono para wa.me: solo cifras y con prefijo de España si no lo lleva. */
    fun telefonoWhatsApp(tel: String): String {
        var t = tel.filter { it.isDigit() || it == '+' }
        if (t.startsWith("+")) return t.drop(1)
        if (t.startsWith("00")) return t.drop(2)
        if (t.length == 9) t = "34$t"
        return t
    }
}
