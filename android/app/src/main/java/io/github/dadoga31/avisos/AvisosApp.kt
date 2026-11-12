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
        repositorio = Repositorio(BaseDatos.de(this).dao(), ajustes, carpeta = filesDir, alCambiar = { publicarResumen(this, it) })
    }

    companion object {
        /** Los contadores del widget de la pantalla de inicio. */
        fun publicarResumen(contexto: Context, avisos: List<Aviso>) {
            val hoy = LocalDate.now()
            val r = Reglas.resumen(avisos, hoy)
            contexto.getSharedPreferences(WidgetAvisos.PREFS, Context.MODE_PRIVATE).edit()
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
