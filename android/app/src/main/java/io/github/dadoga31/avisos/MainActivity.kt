package io.github.dadoga31.avisos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dadoga31.avisos.correo.Cuenta
import io.github.dadoga31.avisos.correo.ServicioCorreo
import io.github.dadoga31.avisos.ui.AppAvisos
import io.github.dadoga31.avisos.ui.AvisosTema
import io.github.dadoga31.avisos.ui.AvisosVM
import io.github.dadoga31.avisos.ui.esOscuro
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val vm: AvisosVM by viewModels()

    /** Ruta que piden el widget, los atajos o una notificación. */
    private val rutaPedida = MutableStateFlow<String?>(null)

    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        enableEdgeToEdge()
        rutaPedida.value = rutaDe(intent)

        setContent {
            val ajustes by vm.ajustes.collectAsStateWithLifecycle()
            val oscuro = esOscuro(ajustes.tema, isSystemInDarkTheme())
            LaunchedEffect(oscuro) {
                val estilo = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            AvisosTema(ajustes.tema, ajustes.materialYou) {
                val ruta by rutaPedida.collectAsStateWithLifecycle()
                /* Con «Quitar animaciones» del móvil, la app también se queda quieta. */
                androidx.compose.runtime.CompositionLocalProvider(
                    io.github.dadoga31.avisos.ui.LocalMovimiento provides io.github.dadoga31.avisos.ui.movimientoDelSistema()
                ) {
                    AppAvisos(vm, ruta) { rutaPedida.value = null }
                }
            }
        }

        pedirNotificaciones()
        arrancarCorreoSiHayCuenta()
    }

    override fun onResume() {
        super.onResume()
        vm.procesarCorreo()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rutaPedida.value = rutaDe(intent)
    }

    /** «#/nuevo» del widget y las notificaciones, o avisos://nuevo de los atajos. */
    private fun rutaDe(intent: Intent?): String? {
        if (intent == null) return null
        intent.getStringExtra("ruta")?.let { r -> if (r.startsWith("#/")) return r.removePrefix("#/").substringBefore('?') }
        val datos = intent.data
        if (datos != null && datos.scheme == "avisos") return datos.host
        return null
    }

    private fun arrancarCorreoSiHayCuenta() {
        val cuenta = runCatching { Cuenta.cargar(this) }.getOrNull()
        if (cuenta != null && cuenta.valida()) ServicioCorreo.arrancar(this)
    }

    /* Sin este permiso el servicio de correo sigue, pero no se ven sus avisos. */
    private fun pedirNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
