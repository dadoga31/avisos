package io.github.dadoga31.avisos.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Tema

/* Paleta de diseno/tokens2.css: fondos neutros y naranja de la marca solo en
   las acciones. Los estados llevan su propio color. */

private val Claro = lightColorScheme(
    primary = Color(0xFFC65500), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCB), onPrimaryContainer = Color(0xFF793100),
    secondary = Color(0xFF765849), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCB), onSecondaryContainer = Color(0xFF5C4033),
    tertiary = Color(0xFF656031), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECE4AA), onTertiaryContainer = Color(0xFF4C481C),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFEEEEEE), onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF9F9F9), onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE2E2E2), onSurfaceVariant = Color(0xFF494645),
    outline = Color(0xFF7A7675), outlineVariant = Color(0xFFCBC5C4),
    inverseSurface = Color(0xFF303030), inverseOnSurface = Color(0xFFF1F1F1), inversePrimary = Color(0xFFFFB691),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEEEEEE), surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2), surfaceDim = Color(0xFFDADADA), surfaceBright = Color(0xFFF9F9F9)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB691), onPrimary = Color(0xFF552000),
    primaryContainer = Color(0xFF793100), onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFE6BEAC), onSecondary = Color(0xFF432A1E),
    secondaryContainer = Color(0xFF5C4033), onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFCFC890), onTertiary = Color(0xFF353107),
    tertiaryContainer = Color(0xFF4C481C), onTertiaryContainer = Color(0xFFECE4AA),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131313), onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF131313), onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF353535), onSurfaceVariant = Color(0xFFCBC5C4),
    outline = Color(0xFF94908E), outlineVariant = Color(0xFF494645),
    inverseSurface = Color(0xFFE2E2E2), inverseOnSurface = Color(0xFF303030), inversePrimary = Color(0xFF9E4200),
    surfaceContainerLowest = Color(0xFF0E0E0E), surfaceContainerLow = Color(0xFF1B1B1B),
    surfaceContainer = Color(0xFF1F1F1F), surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353535), surfaceDim = Color(0xFF131313), surfaceBright = Color(0xFF393939)
)

/** Colores de un estado: el del texto, y el fondo y el contenido de su círculo. */
@Immutable
data class ColorEstado(val color: Color, val fondo: Color, val sobreFondo: Color)

@Immutable
data class Superficies(
    /** Fondo de las pantallas. */
    val pagina: Color,
    /** Fondo de las filas y tarjetas. */
    val item: Color,
    /** Cápsulas flotantes (menú, barra de acciones). */
    val flotante: Color,
    val oscuro: Boolean
)

@Immutable
data class ColoresAvisos(val estados: Map<Estado, ColorEstado>, val superficies: Superficies) {
    fun de(e: Estado) = estados.getValue(e)
}

private val EstadosClaro = mapOf(
    Estado.PENDIENTE to ColorEstado(Color(0xFF5C5E64), Color(0xFFE0E2E8), Color(0xFF191C20)),
    Estado.PROGRAMADO to ColorEstado(Color(0xFF006B5F), Color(0xFF92F4E3), Color(0xFF00201C)),
    Estado.EN_CURSO to ColorEstado(Color(0xFF3F5E98), Color(0xFFD8E2FF), Color(0xFF001A41)),
    Estado.EN_ESPERA to ColorEstado(Color(0xFF6C5293), Color(0xFFECDCFF), Color(0xFF260A4B)),
    Estado.RESUELTO to ColorEstado(Color(0xFF2B6A3C), Color(0xFFAFF3B7), Color(0xFF00210A)),
    Estado.CANCELADO to ColorEstado(Color(0xFF5C5F5F), Color(0xFFE1E3E3), Color(0xFF191C1D))
)

private val EstadosOscuro = mapOf(
    Estado.PENDIENTE to ColorEstado(Color(0xFFC4C6CC), Color(0xFF44474C), Color(0xFFE0E2E8)),
    Estado.PROGRAMADO to ColorEstado(Color(0xFF76D7C7), Color(0xFF005048), Color(0xFF92F4E3)),
    Estado.EN_CURSO to ColorEstado(Color(0xFFADC6FF), Color(0xFF24467F), Color(0xFFD8E2FF)),
    Estado.EN_ESPERA to ColorEstado(Color(0xFFD6BAFF), Color(0xFF533A79), Color(0xFFECDCFF)),
    Estado.RESUELTO to ColorEstado(Color(0xFF93D69D), Color(0xFF0E5226), Color(0xFFAFF3B7)),
    Estado.CANCELADO to ColorEstado(Color(0xFFC4C7C7), Color(0xFF444748), Color(0xFFE1E3E3))
)

val LocalColores = staticCompositionLocalOf {
    ColoresAvisos(EstadosClaro, Superficies(Color(0xFFEEEEEE), Color.White, Color.White, false))
}

/** Colores de técnicos para los avatares (fondo, letra), en claro y oscuro. */
fun colorTecnico(indice: Int, oscuro: Boolean): Pair<Color, Color> {
    val claros = listOf(
        Color(0xFFFFDBCB) to Color(0xFF793100), Color(0xFF92F4E3) to Color(0xFF00201C),
        Color(0xFFECDCFF) to Color(0xFF260A4B), Color(0xFFD8E2FF) to Color(0xFF001A41),
        Color(0xFFAFF3B7) to Color(0xFF00210A), Color(0xFFFFD9E2) to Color(0xFF3E001D),
        Color(0xFFECE4AA) to Color(0xFF4C481C), Color(0xFFCDE5FF) to Color(0xFF001D32)
    )
    val oscuros = listOf(
        Color(0xFF793100) to Color(0xFFFFDBCB), Color(0xFF005048) to Color(0xFF92F4E3),
        Color(0xFF533A79) to Color(0xFFECDCFF), Color(0xFF24467F) to Color(0xFFD8E2FF),
        Color(0xFF0E5226) to Color(0xFFAFF3B7), Color(0xFF7B2949) to Color(0xFFFFD9E2),
        Color(0xFF4C481C) to Color(0xFFECE4AA), Color(0xFF004B73) to Color(0xFFCDE5FF)
    )
    val l = if (oscuro) oscuros else claros
    return l[((indice % l.size) + l.size) % l.size]
}

fun esOscuro(tema: Tema, sistema: Boolean) = when (tema) {
    Tema.AUTO -> sistema
    Tema.CLARO -> false
    Tema.OSCURO -> true
}

@Composable
fun AvisosTema(tema: Tema = Tema.AUTO, materialYou: Boolean = false, contenido: @Composable () -> Unit) {
    val oscuro = esOscuro(tema, isSystemInDarkTheme())
    val contexto = LocalContext.current
    val esquema: ColorScheme = when {
        materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
        oscuro -> Oscuro
        else -> Claro
    }
    val superficies = if (oscuro) Superficies(esquema.surface, esquema.surfaceContainerHigh, esquema.surfaceContainerHighest, true)
    else Superficies(esquema.surfaceContainer, esquema.surfaceContainerLowest, esquema.surfaceContainerLowest, false)
    CompositionLocalProvider(LocalColores provides ColoresAvisos(if (oscuro) EstadosOscuro else EstadosClaro, superficies)) {
        MaterialTheme(colorScheme = esquema) {
            /* Sin esto, el texto que no está dentro de un Surface sale negro también en oscuro. */
            CompositionLocalProvider(LocalContentColor provides esquema.onSurface, content = contenido)
        }
    }
}
