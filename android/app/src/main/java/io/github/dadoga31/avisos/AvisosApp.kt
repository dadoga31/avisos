package io.github.dadoga31.avisos

import android.app.Application
import android.content.Context
import io.github.dadoga31.avisos.datos.AlmacenAjustes
import io.github.dadoga31.avisos.datos.Aviso
import io.github.dadoga31.avisos.datos.BaseDatos
import io.github.dadoga31.avisos.datos.Reglas
import io.github.dadoga31.avisos.datos.Repositorio
import java.time.LocalDate

class AvisosApp : Application() {
    lateinit var ajustes: AlmacenAjustes
        private set
    lateinit var repositorio: Repositorio
        private set

    override fun onCreate() {
        super.onCreate()
        ajustes = AlmacenAjustes(this)
        repositorio = Repositorio(BaseDatos.de(this).dao(), ajustes, carpeta = filesDir, alCambiar = {
            publicarResumen(this, it)
            Recordatorios.reprogramar(this, it, ajustes.actual.recordatorio)
        })
        Actualizador.programar(this)
        Thread { runCatching { Actualizador.limpiar(this) } }.start()
    }

    companion object {
        /** Vuelve a calcular el widget y los recordatorios desde la base de datos (hilo de fondo). */
        @JvmStatic
        fun recalcular(contexto: Context) {
            runCatching {
                kotlinx.coroutines.runBlocking { (contexto.applicationContext as AvisosApp).repositorio.avisarCambio() }
            }
        }

        /** Los contadores del widget de la pantalla de inicio. */
        fun publicarResumen(contexto: Context, avisos: List<Aviso>) {
            val hoy = LocalDate.now()
            val r = Reglas.resumen(avisos, hoy)
            /* El siguiente aviso, como en la tarjeta de la Agenda. */
            val s = Reglas.siguiente(avisos, hoy, java.time.LocalTime.now())
            val cabecera = when (s?.momento) {
                null -> ""
                Reglas.Momento.EN_CURSO -> "EN CURSO" + if (s.aviso.hora.isNotEmpty()) " · ${s.aviso.hora}" else ""
                Reglas.Momento.SIN_HORA -> "HOY"
                else -> "SIGUIENTE · ${s.aviso.hora}"
            }
            val texto = s?.aviso?.let { a -> listOf(a.cliente.nombre, a.titulo).filter { it.isNotBlank() }.joinToString(" · ") } ?: ""
            contexto.getSharedPreferences(WidgetAvisos.PREFS, Context.MODE_PRIVATE).edit()
                .putString(WidgetAvisos.CLAVE_SIGUIENTE_ID, s?.aviso?.id ?: "")
                .putString(WidgetAvisos.CLAVE_SIGUIENTE_CABECERA, cabecera)
                .putString(WidgetAvisos.CLAVE_SIGUIENTE_TEXTO, texto)
                .putString(WidgetAvisos.CLAVE_DIA, hoy.toString())
                .putInt(WidgetAvisos.CLAVE_HOY, r.hoy)
                .putInt(WidgetAvisos.CLAVE_VENCIDOS, r.vencidos)
                .putInt(WidgetAvisos.CLAVE_ABIERTOS, r.abiertos)
                .putInt(WidgetAvisos.CLAVE_HECHOS, r.hechosHoy)
                .apply()
            WidgetAvisos.actualizarTodos(contexto)
        }
    }
}
