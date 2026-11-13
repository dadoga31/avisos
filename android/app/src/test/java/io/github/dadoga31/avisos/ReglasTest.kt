package io.github.dadoga31.avisos

import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.Cliente
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Prioridad
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Tecnico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class ReglasTest {
    private val hoy = LocalDate.of(2026, 11, 12)

    private fun av(id: String, fecha: String = "", hora: String = "", estado: Estado = Estado.PENDIENTE,
                   prioridad: Prioridad = Prioridad.NORMAL, asignado: String = "", ref: String = "", cerrado: String = "") =
        Aviso(id = id, titulo = "Aviso $id", fecha = fecha, hora = hora, estado = estado.id, prioridad = prioridad.id,
            asignadoA = asignado, ref = ref, cerrado = cerrado)

    @Test
    fun `todo aviso abierto cae en algun grupo de la agenda`() {
        val lista = listOf(
            av("venc", "2026-11-10"), av("hoy", "2026-11-12"), av("man", "2026-11-13"),
            av("sem", "2026-11-18"), av("luego", "2026-12-30"), av("sin"),
            av("cerrado", "2026-11-12", estado = Estado.RESUELTO)
        )
        val grupos = Reglas.grupos(lista, hoy)
        val claves = grupos.associate { g -> g.clave to g.avisos.map { it.id } }
        assertEquals(listOf("venc"), claves["vencidos"])
        assertEquals(listOf("hoy"), claves["hoy"])
        assertEquals(listOf("man"), claves["manana"])
        assertEquals(listOf("sem"), claves["semana"])
        assertEquals(listOf("luego"), claves["despues"])
        assertEquals(listOf("sin"), claves["sinfecha"])
        assertEquals(6, grupos.sumOf { it.avisos.size })
    }

    @Test
    fun `hoy sale aunque este vacio y los demas grupos vacios no`() {
        val grupos = Reglas.grupos(listOf(av("a", "2026-11-18")), hoy)
        assertEquals(listOf("hoy", "semana"), grupos.map { it.clave })
    }

    @Test
    fun `dentro del grupo ordena por hora y a igual hora lo urgente primero`() {
        val lista = listOf(
            av("tarde", "2026-11-12", "16:00"), av("sinhora", "2026-11-12"),
            av("normal", "2026-11-12", "09:00"), av("urgente", "2026-11-12", "09:00", prioridad = Prioridad.URGENTE)
        )
        val hoyG = Reglas.grupos(lista, hoy).first { it.clave == "hoy" }
        assertEquals(listOf("urgente", "normal", "tarde", "sinhora"), hoyG.avisos.map { it.id })
    }

    @Test
    fun `vencido solo si esta abierto y la fecha ya paso`() {
        assertTrue(Reglas.vencido(av("a", "2026-11-11"), hoy))
        assertFalse(Reglas.vencido(av("b", "2026-11-12"), hoy))
        assertFalse(Reglas.vencido(av("c", "2026-11-01", estado = Estado.RESUELTO), hoy))
        assertFalse(Reglas.vencido(av("d"), hoy))
    }

    @Test
    fun `el siguiente es el que esta en curso y si no el proximo de hoy`() {
        val ahora = LocalTime.of(9, 41)
        val lista = listOf(av("pasado", "2026-11-12", "08:00"), av("once", "2026-11-12", "11:00"), av("cuatro", "2026-11-12", "16:00"))
        val s = Reglas.siguiente(lista, hoy, ahora)!!
        assertEquals("once", s.aviso.id)
        assertEquals(Reglas.Momento.PROXIMO, s.momento)
        assertEquals(79, s.minutos)

        val conCurso = lista + av("curso", "2026-11-12", "16:00", estado = Estado.EN_CURSO)
        assertEquals("curso", Reglas.siguiente(conCurso, hoy, ahora)!!.aviso.id)

        val tarde = Reglas.siguiente(lista, hoy, LocalTime.of(18, 0))!!
        assertEquals(Reglas.Momento.PASADO, tarde.momento)
        assertEquals("cuatro", tarde.aviso.id)

        assertNull(Reglas.siguiente(listOf(av("otro", "2026-11-13", "10:00")), hoy, ahora))
    }

    @Test
    fun `la referencia sigue al contador o al numero mas alto que haya`() {
        assertEquals("AV-0001" to 1, Reglas.siguienteRef("AV", 0, emptyList()))
        assertEquals("AV-0008" to 8, Reglas.siguienteRef("AV", 7, listOf(av("a", ref = "AV-0003"))))
        assertEquals("OT-0013" to 13, Reglas.siguienteRef("OT", 2, listOf(av("a", ref = "AV-0012"))))
    }

    @Test
    fun `buscar ignora tildes y mayusculas y con texto entran los cerrados`() {
        val lista = listOf(
            av("a").copy(cliente = Cliente(nombre = "Farmacia Centro")),
            av("b", estado = Estado.RESUELTO).copy(cliente = Cliente(nombre = "Joyería Duarte")),
            av("c", asignado = "t1")
        )
        val tec = listOf(Tecnico("t1", "Laura Pinto"))
        assertEquals(listOf("b"), Reglas.filtrar(lista, tec, Reglas.Filtro(texto = "joyeria"), hoy).map { it.id })
        assertEquals(listOf("a"), Reglas.filtrar(lista, tec, Reglas.Filtro(texto = "FARMACIA cen"), hoy).map { it.id })
        assertEquals(listOf("c"), Reglas.filtrar(lista, tec, Reglas.Filtro(texto = "laura"), hoy).map { it.id })
        assertEquals(listOf("a"), Reglas.filtrar(lista, tec, Reglas.Filtro(sinAsignar = true), hoy).map { it.id })
    }

    @Test
    fun `progreso de hoy cuenta lo cerrado hoy frente a lo que habia`() {
        val cerradoHoy = hoy.atTime(11, 23).toInstant(ZoneOffset.UTC).toString()
        val lista = listOf(
            av("a", "2026-11-12"), av("b", "2026-11-12"),
            av("c", "2026-11-06", estado = Estado.RESUELTO, cerrado = cerradoHoy)
        )
        assertEquals(Reglas.Progreso(1, 3), Reglas.progresoHoy(lista, hoy, ZoneOffset.UTC))
    }

    @Test
    fun `textos de horas, dias y telefonos`() {
        assertEquals("1,5 h", Reglas.horas(1.5))
        assertEquals("3 h", Reglas.horas(3.0))
        assertEquals("1 h 19 min", Reglas.minutos(79))
        assertEquals("Hoy", Reglas.dia("2026-11-12", hoy))
        assertEquals("Mañana", Reglas.dia("2026-11-13", hoy))
        assertEquals("Lun 16", Reglas.dia("2026-11-16", hoy))
        assertEquals("30 dic", Reglas.dia("2026-12-30", hoy))
        assertEquals("Ayer 09:30", Reglas.cuando(av("a", "2026-11-11", "09:30"), hoy))
        assertEquals("34600112233", Reglas.telefonoWhatsApp("600 11 22 33"))
        assertEquals("34600112233", Reglas.telefonoWhatsApp("+34 600112233"))
    }

    @Test
    fun `recordatorio solo para los mios o sin asignar, abiertos, sin empezar y con hora`() {
        val z = ZoneOffset.UTC
        val base = av("a", "2026-11-12", "11:00", estado = Estado.PROGRAMADO, asignado = Tecnico.YO)
        assertEquals(java.time.Instant.parse("2026-11-12T10:40:00Z"), Reglas.momentoRecordatorio(base, 20, z))
        assertEquals(java.time.Instant.parse("2026-11-12T10:40:00Z"), Reglas.momentoRecordatorio(base.copy(asignadoA = ""), 20, z))
        assertNull(Reglas.momentoRecordatorio(base.copy(asignadoA = "laura"), 20, z))
        assertNull(Reglas.momentoRecordatorio(base.copy(estado = Estado.EN_CURSO.id), 20, z))
        assertNull(Reglas.momentoRecordatorio(base.copy(estado = Estado.RESUELTO.id), 20, z))
        assertNull(Reglas.momentoRecordatorio(base.copy(hora = ""), 20, z))
        assertNull(Reglas.momentoRecordatorio(base, 0, z))
    }

    @Test
    fun `pendientes son los que aun no han saltado, por orden`() {
        val z = ZoneOffset.UTC
        val ahora = java.time.Instant.parse("2026-11-12T09:41:00Z")
        val lista = listOf(
            av("tarde", "2026-11-12", "16:00", asignado = Tecnico.YO),
            av("pasado", "2026-11-12", "09:50", asignado = Tecnico.YO),   // saltaba a las 09:30
            av("once", "2026-11-12", "11:00"),
            av("manana", "2026-11-13", "08:00", asignado = Tecnico.YO)
        )
        assertEquals(listOf("once", "tarde", "manana"), Reglas.recordatoriosPendientes(lista, 20, ahora, z).map { it.first.id })
    }

    @Test
    fun `texto del recordatorio`() {
        val z = ZoneOffset.UTC
        val a = av("a", "2026-11-12", "11:00").copy(titulo = "Sustitución de 2 cámaras", cliente = Cliente(nombre = "Naves Logísticas Sur"))
        assertEquals(
            "En 20 min · Naves Logísticas Sur" to "11:00 · Sustitución de 2 cámaras",
            Reglas.textoRecordatorio(a, java.time.Instant.parse("2026-11-12T10:40:00Z"), z)
        )
        assertEquals("Ahora · Naves Logísticas Sur", Reglas.textoRecordatorio(a, java.time.Instant.parse("2026-11-12T11:05:00Z"), z).first)
    }

    // ---------- línea de tiempo y panel de Hechos (3.5.0) ----------

    private fun utc(h: Int, m: Int, dia: LocalDate = hoy) = dia.atTime(h, m).toInstant(ZoneOffset.UTC).toString()

    @Test
    fun `la linea del dia pone cada aviso en su hora y deja fuera lo cancelado`() {
        val lista = listOf(
            av("once", "2026-11-12", "11:30").copy(duracion = "1,5"),
            av("curso", "2026-11-12", "09:00", estado = Estado.EN_CURSO).copy(duracion = "2"),
            av("hecho", "2026-11-12", "08:00", estado = Estado.RESUELTO, cerrado = utc(8, 42)).copy(horas = 0.5),
            av("cancelado", "2026-11-12", "10:00", estado = Estado.CANCELADO),
            av("sinhora", "2026-11-12"),
            av("manana", "2026-11-13", "09:00")
        )
        val l = Reglas.linea(lista, hoy, LocalTime.of(9, 41), ZoneOffset.UTC)
        assertEquals(listOf("hecho", "curso", "once"), l.map { it.aviso.id })
        assertEquals(11 * 60 + 30, l[2].inicio)
        assertEquals(13 * 60, l[2].fin)
        assertTrue(l[0].hecho)
        assertEquals(listOf("sinhora"), Reglas.sinHora(lista, hoy, ZoneOffset.UTC).map { it.id })
        assertEquals(listOf("manana"), Reglas.proximos(lista, hoy).map { it.id })
    }

    @Test
    fun `lo que esta en curso llega hasta ahora y sin hora va desde que se empezo`() {
        val largo = av("largo", "2026-11-12", "08:00", estado = Estado.EN_CURSO)
        assertEquals(12 * 60 + 10, Reglas.linea(listOf(largo), hoy, LocalTime.of(12, 10), ZoneOffset.UTC).single().fin)
        val sinHora = av("sh", "2026-11-12", estado = Estado.EN_CURSO).copy(empezado = utc(10, 5))
        val b = Reglas.linea(listOf(sinHora), hoy, null, ZoneOffset.UTC).single()
        assertEquals(10 * 60 + 5, b.inicio)
        assertTrue(Reglas.sinHora(listOf(sinHora), hoy, ZoneOffset.UTC).isEmpty())
    }

    @Test
    fun `lo cerrado hoy de otro dia sale en la linea de hoy acabando a su hora`() {
        val viejo = av("v", "2026-11-06", "10:00", estado = Estado.RESUELTO, cerrado = utc(12, 0)).copy(horas = 2.0)
        val b = Reglas.linea(listOf(viejo), hoy, null, ZoneOffset.UTC).single()
        assertEquals(10 * 60, b.inicio)
        assertEquals(12 * 60, b.fin)
    }

    @Test
    fun `en curso va aparte, lo empezado antes primero`() {
        val lista = listOf(
            av("b", estado = Estado.EN_CURSO).copy(empezado = utc(9, 30)),
            av("a", estado = Estado.EN_CURSO).copy(empezado = utc(8, 0)),
            av("p", "2026-11-12", "10:00")
        )
        assertEquals(listOf("a", "b"), Reglas.enCurso(lista).map { it.id })
    }

    @Test
    fun `sin fecha va en su bandeja, lo urgente primero`() {
        val lista = listOf(av("n"), av("u", prioridad = Prioridad.URGENTE), av("f", "2026-11-12"), av("c", estado = Estado.RESUELTO))
        assertEquals(listOf("u", "n"), Reglas.sinFecha(lista).map { it.id })
    }

    @Test
    fun `panel de hechos suma horas por dia y compara con la semana anterior`() {
        val lista = listOf(
            av("hoy", estado = Estado.RESUELTO, cerrado = utc(10, 0)).copy(horas = 4.0),
            av("lunes", estado = Estado.RESUELTO, cerrado = utc(10, 0, hoy.minusDays(3))).copy(horas = 3.5),
            av("antes", estado = Estado.RESUELTO, cerrado = utc(10, 0, hoy.minusDays(9))).copy(horas = 5.0),
            av("cancel", estado = Estado.CANCELADO, cerrado = utc(9, 0)),
            av("abierto", "2026-11-12")
        )
        val p = Reglas.panel(lista, Reglas.Periodo.SEMANA, hoy, ZoneOffset.UTC)
        assertEquals(7.5, p.horas, 0.0)
        assertEquals(5.0, p.horasAntes, 0.0)
        assertEquals(2, p.resueltos)
        assertEquals(1, p.cancelados)
        assertEquals(listOf("V", "S", "D", "L", "M", "X", "J"), p.barras.map { it.etiqueta })
        assertEquals(4.0, p.barras.last().horas, 0.0)
        assertTrue(p.barras.last().actual)
        assertEquals(3.5, p.barras[3].horas, 0.0)
        assertEquals(12, Reglas.panel(lista, Reglas.Periodo.ANO, hoy, ZoneOffset.UTC).barras.size)
    }

    @Test
    fun `duracion prevista con coma y por defecto una hora`() {
        assertEquals(90, Reglas.duracionMin(av("a").copy(duracion = "1,5")))
        assertEquals(60, Reglas.duracionMin(av("a")))
    }
}
