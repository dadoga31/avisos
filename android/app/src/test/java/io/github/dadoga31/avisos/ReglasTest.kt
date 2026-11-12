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
}
