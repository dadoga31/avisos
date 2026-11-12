package io.github.dadoga31.avisos.datos

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tema(val id: String, val nombre: String) {
    AUTO("auto", "Automático"), CLARO("claro", "Claro"), OSCURO("oscuro", "Oscuro");

    companion object {
        fun de(id: String?) = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class Ajustes(
    val tema: Tema = Tema.AUTO,
    /** Colores del fondo de pantalla (Material You, Android 12+). */
    val materialYou: Boolean = false,
    val prefijoRef: String = "AV",
    val contadorRef: Int = 0,
    /** Minutos de antelación del recordatorio; 0 = sin recordatorio. */
    val recordatorio: Int = 20,
    /** Enseñar la pista de deslizar en la Agenda hasta que se use. */
    val pistaGestos: Boolean = true
)

/** Ajustes en SharedPreferences, con un flujo para que la interfaz se entere. */
class AlmacenAjustes(contexto: Context) {
    private val prefs = contexto.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    private val estado = MutableStateFlow(leer())
    val flujo: StateFlow<Ajustes> = estado.asStateFlow()
    val actual: Ajustes get() = estado.value

    private fun leer() = Ajustes(
        tema = Tema.de(prefs.getString("tema", null)),
        materialYou = prefs.getBoolean("materialYou", false),
        prefijoRef = prefs.getString("prefijoRef", "AV") ?: "AV",
        contadorRef = prefs.getInt("contadorRef", 0),
        recordatorio = prefs.getInt("recordatorio", 20),
        pistaGestos = prefs.getBoolean("pistaGestos", true)
    )

    @Synchronized
    fun cambiar(f: (Ajustes) -> Ajustes) {
        val n = f(estado.value)
        prefs.edit()
            .putString("tema", n.tema.id)
            .putBoolean("materialYou", n.materialYou)
            .putString("prefijoRef", n.prefijoRef)
            .putInt("contadorRef", n.contadorRef)
            .putInt("recordatorio", n.recordatorio)
            .putBoolean("pistaGestos", n.pistaGestos)
            .apply()
        estado.value = n
    }
}
