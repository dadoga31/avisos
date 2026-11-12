package io.github.dadoga31.avisos.datos

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/* Modelo de datos. Los nombres y los valores son los de la app web
   (assets/js/store.js), para poder importar sus copias de seguridad. */

enum class Estado(val id: String, val nombre: String, val explicacion: String, val abierto: Boolean) {
    PENDIENTE("pendiente", "Pendiente", "Sin fecha o sin técnico", true),
    PROGRAMADO("programado", "Programado", "Tiene fecha y técnico", true),
    EN_CURSO("en_curso", "En curso", "Estás en ello ahora", true),
    EN_ESPERA("en_espera", "En espera", "Falta material o respuesta", true),
    RESUELTO("resuelto", "Resuelto", "Hecho: pasa a Hechos", false),
    CANCELADO("cancelado", "Cancelado", "No se hace", false);

    companion object {
        fun de(id: String?): Estado = entries.firstOrNull { it.id == id } ?: PENDIENTE
    }
}

enum class Prioridad(val id: String, val nombre: String, val peso: Int) {
    BAJA("baja", "Baja", 0),
    NORMAL("normal", "Normal", 1),
    ALTA("alta", "Alta", 2),
    URGENTE("urgente", "Urgente", 3);

    companion object {
        fun de(id: String?): Prioridad = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class Tipo(val id: String, val nombre: String) {
    AVERIA("averia", "Avería"),
    INSTALACION("instalacion", "Instalación"),
    MANTENIMIENTO("mantenimiento", "Mantenimiento"),
    REVISION("revision", "Revisión"),
    PRESUPUESTO("presupuesto", "Presupuesto");

    companion object {
        fun de(id: String?): Tipo = entries.firstOrNull { it.id == id } ?: AVERIA
    }
}

enum class Sistema(val id: String, val nombre: String) {
    ALARMA("alarma", "Alarma"),
    CCTV("cctv", "CCTV"),
    ACCESOS("accesos", "Control de accesos"),
    INCENDIOS("incendios", "Incendios"),
    INTERFONO("interfono", "Portero / interfono"),
    OTROS("otros", "Otros");

    companion object {
        fun de(id: String?): Sistema = entries.firstOrNull { it.id == id } ?: ALARMA
    }
}

data class Cliente(
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val contacto: String = ""
)

data class Nota(val id: String, val ts: String, val texto: String)

@Entity(tableName = "avisos")
data class Aviso(
    @PrimaryKey val id: String,
    val ref: String = "",
    val titulo: String = "",
    val tipo: String = Tipo.AVERIA.id,
    val sistema: String = Sistema.ALARMA.id,
    val prioridad: String = Prioridad.NORMAL.id,
    val estado: String = Estado.PENDIENTE.id,
    /** 'manual' o 'correo'. */
    val origen: String = "manual",
    /** Message-ID del correo que lo creó, para no crearlo dos veces. */
    val correoId: String = "",
    @Embedded(prefix = "cliente_") val cliente: Cliente = Cliente(),
    /** yyyy-MM-dd o vacío. */
    val fecha: String = "",
    /** HH:mm o vacío. */
    val hora: String = "",
    /** Horas previstas, como texto («2», «1,5»). */
    val duracion: String = "",
    /** Id del técnico o vacío. */
    val asignadoA: String = "",
    val descripcion: String = "",
    val notas: List<Nota> = emptyList(),
    /** Horas trabajadas en total. */
    val horas: Double = 0.0,
    val creado: String = "",
    val actualizado: String = "",
    /** Cuándo se puso en curso por última vez (ISO), o vacío. */
    val empezado: String = "",
    /** Cuándo se cerró (ISO), o vacío si está abierto. */
    val cerrado: String = ""
) {
    val estadoE get() = Estado.de(estado)
    val prioridadE get() = Prioridad.de(prioridad)
    val tipoE get() = Tipo.de(tipo)
    val sistemaE get() = Sistema.de(sistema)
    val abierto get() = estadoE.abierto
}

@Entity(tableName = "tecnicos")
data class Tecnico(
    @PrimaryKey val id: String,
    val nombre: String,
    val telefono: String = "",
    /** Índice de color (0..7) para su avatar. */
    val color: Int = 0
) {
    val iniciales: String
        get() {
            val p = nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (p.isEmpty()) return "?"
            return (p.first().take(1) + if (p.size > 1) p.last().take(1) else "").uppercase()
        }

    companion object {
        /** El técnico que usa el móvil. Se crea al abrir la app por primera vez. */
        const val YO = "yo"
    }
}
