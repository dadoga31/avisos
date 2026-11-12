package io.github.dadoga31.avisos.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.dadoga31.avisos.R
import io.github.dadoga31.avisos.datos.Estado
import io.github.dadoga31.avisos.datos.Tema

/*
 * Paleta del usuario: Off-White #F9F9F9, Matte Black #28282B y Red Orange
 * #FF4200. La interfaz es neutra (grises) y el naranja va en las acciones y
 * en lo que está «en marcha». Contraste:
 *  - #FF4200 sobre el Off-White da 3,3:1 y con letra blanca encima 3,5:1:
 *    no vale para texto pequeño. Los botones naranjas llevan letra casi negra
 *    (5,3:1) y el texto naranja pequeño usa [Superficies.acentoTexto], un
 *    punto más oscuro en claro (4,5:1) y más claro en oscuro.
 */

val RedOrange = Color(0xFFFF4200)
val OffWhite = Color(0xFFF9F9F9)
val MatteBlack = Color(0xFF28282B)
private val Tinta = Color(0xFF141416)

private val Claro = lightColorScheme(
    primary = RedOrange, onPrimary = Tinta,
    primaryContainer = Color(0xFFFFE3D6), onPrimaryContainer = Color(0xFF6B1D00),
    /* Lo seleccionado (fichas, menú, botones tonales) va en Matte Black. */
    secondary = MatteBlack, onSecondary = OffWhite,
    secondaryContainer = MatteBlack, onSecondaryContainer = OffWhite,
    tertiary = Color(0xFF9A6200), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0D2), onTertiaryContainer = Color(0xFF5C3A00),
    error = Color(0xFFD63800), onError = Color.White,
    errorContainer = Color(0xFFFFE3D6), onErrorContainer = Color(0xFF6B1D00),
    background = OffWhite, onBackground = MatteBlack,
    surface = OffWhite, onSurface = MatteBlack,
    surfaceVariant = Color(0xFFEDEDEF), onSurfaceVariant = Color(0xFF6B6B72),
    outline = Color(0xFFA3A3AA), outlineVariant = Color(0xFFE4E4E7),
    inverseSurface = MatteBlack, inverseOnSurface = OffWhite, inversePrimary = RedOrange,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F4F5),
    surfaceContainer = Color(0xFFEFEFF0), surfaceContainerHigh = Color(0xFFE9E9EB),
    surfaceContainerHighest = Color(0xFFE3E3E6), surfaceDim = Color(0xFFDCDCDF), surfaceBright = OffWhite,
    scrim = Color.Black
)

private val Oscuro = darkColorScheme(
    primary = RedOrange, onPrimary = Tinta,
    primaryContainer = Color(0xFF5A1E08), onPrimaryContainer = Color(0xFFFFD9C9),
    secondary = OffWhite, onSecondary = MatteBlack,
    secondaryContainer = OffWhite, onSecondaryContainer = MatteBlack,
    tertiary = Color(0xFFFFB020), onTertiary = Tinta,
    tertiaryContainer = Color(0xFF4A3410), onTertiaryContainer = Color(0xFFFFD58A),
    error = Color(0xFFFF7A45), onError = Tinta,
    errorContainer = Color(0xFF5A1E08), onErrorContainer = Color(0xFFFFD9C9),
    background = MatteBlack, onBackground = OffWhite,
    surface = MatteBlack, onSurface = OffWhite,
    surfaceVariant = Color(0xFF3A3A3F), onSurfaceVariant = Color(0xFFA6A6AD),
    outline = Color(0xFF6E6E75), outlineVariant = Color(0xFF3D3D42),
    inverseSurface = OffWhite, inverseOnSurface = MatteBlack, inversePrimary = RedOrange,
    surfaceContainerLowest = Color(0xFF1E1E21), surfaceContainerLow = Color(0xFF2D2D31),
    surfaceContainer = Color(0xFF313135), surfaceContainerHigh = Color(0xFF38383C),
    surfaceContainerHighest = Color(0xFF424247), surfaceDim = Color(0xFF1E1E21), surfaceBright = Color(0xFF46464B),
    scrim = Color.Black
)

// ---------------------------------------------------------------- tipografía

/** Geométrica y técnica: títulos, cifras, etiquetas y botones. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

/** Monoespaciada: referencias, horas y cantidades (las cifras no bailan). */
val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold)
)

private fun g(tam: Int, alto: Int, peso: FontWeight, espacio: Double = 0.0) =
    TextStyle(fontFamily = SpaceGrotesk, fontSize = tam.sp, lineHeight = alto.sp, fontWeight = peso, letterSpacing = espacio.sp)

private val Base = Typography()

val Tipografia = Typography(
    displayLarge = g(57, 64, FontWeight.Medium, -1.5),
    displayMedium = g(45, 52, FontWeight.Medium, -1.0),
    displaySmall = g(36, 44, FontWeight.Medium, -0.8),
    headlineLarge = g(34, 40, FontWeight.SemiBold, -0.8),
    headlineMedium = g(28, 36, FontWeight.Medium, -0.5),
    headlineSmall = g(24, 32, FontWeight.Medium, -0.3),
    titleLarge = g(22, 28, FontWeight.Medium, -0.2),
    titleMedium = g(16, 22, FontWeight.SemiBold, -0.1),
    titleSmall = g(14, 20, FontWeight.SemiBold, 0.0),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = g(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = g(12, 16, FontWeight.Medium, 0.2),
    labelSmall = g(11, 16, FontWeight.Medium, 0.3)
)

/** Etiqueta técnica en mayúsculas («SIGUIENTE · 11:00», cabeceras de sección). */
val EstiloEtiqueta = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)

/** Etiqueta pequeña de las rejillas y datos (10,5 sp). */
val EstiloDato = TextStyle(fontFamily = Mono, fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)

/** Cifras y referencias en las filas. */
val EstiloMono = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ---------------------------------------------------------------- colores propios

/**
 * Colores de un estado. [color]: la marca (iconos, rellenos, barras).
 * [texto]: para texto pequeño con contraste suficiente. [fondo]/[sobreFondo]:
 * el recuadro del icono del estado.
 */
@Immutable
data class ColorEstado(val color: Color, val texto: Color, val fondo: Color, val sobreFondo: Color)

@Immutable
data class Superficies(
    /** Fondo de las pantallas. */
    val pagina: Color,
    /** Fondo de filas y tarjetas. */
    val item: Color,
    /** Cápsulas flotantes (menú, barra de acciones), con desenfoque detrás. */
    val flotante: Color,
    /** Línea fina de filas y tarjetas: aquí no hay sombras. */
    val borde: Color,
    /** La tarjeta del siguiente aviso: oscura en los dos modos. */
    val heroe: Color,
    val sobreHeroe: Color,
    val sobreHeroeSuave: Color,
    /** Naranja para texto pequeño (el puro no llega al contraste mínimo). */
    val acentoTexto: Color,
    val oscuro: Boolean,
    /** Puntos de la rejilla del fondo. */
    val rejilla: Color = Color.Transparent,
    /** Velo naranja de lo que está en curso. */
    val tinte: Color = Color.Transparent,
    /** Lo que va bien (correo escuchando, hecho). */
    val ok: Color = Color(0xFF1F9D55),
    /** Tercer nivel de texto: horas de la rejilla, iconos apagados. */
    val tenue: Color = Color(0xFFA3A3AA)
)

@Immutable
data class ColoresAvisos(val estados: Map<Estado, ColorEstado>, val superficies: Superficies) {
    fun de(e: Estado) = estados.getValue(e)
}

private fun estado(color: Color, texto: Color, oscuro: Boolean) =
    ColorEstado(color, texto, color.copy(alpha = if (oscuro) 0.16f else 0.10f), color)

private val EstadosClaro = mapOf(
    Estado.PENDIENTE to estado(Color(0xFF8A8A91), Color(0xFF6B6B72), false),
    Estado.PROGRAMADO to estado(MatteBlack, MatteBlack, false),
    Estado.EN_CURSO to ColorEstado(RedOrange, Color(0xFFD63800), RedOrange, Tinta),
    Estado.EN_ESPERA to estado(Color(0xFFE09A00), Color(0xFF9A6200), false),
    Estado.RESUELTO to estado(Color(0xFF1F9D55), Color(0xFF157A41), false),
    Estado.CANCELADO to estado(Color(0xFFA0A0A6), Color(0xFF7A7A80), false)
)

private val EstadosOscuro = mapOf(
    Estado.PENDIENTE to estado(Color(0xFF9C9CA3), Color(0xFFA6A6AD), true),
    Estado.PROGRAMADO to estado(OffWhite, OffWhite, true),
    Estado.EN_CURSO to ColorEstado(RedOrange, Color(0xFFFF7A45), RedOrange, Tinta),
    Estado.EN_ESPERA to estado(Color(0xFFFFB020), Color(0xFFFFB020), true),
    Estado.RESUELTO to estado(Color(0xFF3DDC84), Color(0xFF3DDC84), true),
    Estado.CANCELADO to estado(Color(0xFF6E6E75), Color(0xFF8A8A91), true)
)

private val SuperficiesClaro = Superficies(
    pagina = OffWhite, item = Color.White, flotante = Color.White.copy(alpha = 0.78f), borde = Color(0xFFE6E6E9),
    heroe = MatteBlack, sobreHeroe = OffWhite, sobreHeroeSuave = Color(0xFFA6A6AD),
    acentoTexto = Color(0xFFD63800), oscuro = false,
    rejilla = MatteBlack.copy(alpha = 0.10f), tinte = RedOrange.copy(alpha = 0.06f), ok = Color(0xFF1F9D55), tenue = Color(0xFFA3A3AA)
)

private val SuperficiesOscuro = Superficies(
    pagina = MatteBlack, item = Color(0xFF313135), flotante = Color(0xFF1E1E21).copy(alpha = 0.72f), borde = Color(0xFF3D3D42),
    heroe = Color(0xFF1C1C1F), sobreHeroe = OffWhite, sobreHeroeSuave = Color(0xFFA6A6AD),
    acentoTexto = Color(0xFFFF7A45), oscuro = true,
    rejilla = OffWhite.copy(alpha = 0.07f), tinte = RedOrange.copy(alpha = 0.10f), ok = Color(0xFF3DDC84), tenue = Color(0xFF6E6E75)
)

val LocalColores = staticCompositionLocalOf { ColoresAvisos(EstadosClaro, SuperficiesClaro) }

/** Avatares de técnicos: sobrios, con la inicial en mono. */
fun colorTecnico(indice: Int, oscuro: Boolean): Pair<Color, Color> {
    val claros = listOf(
        MatteBlack to OffWhite, RedOrange to Tinta, Color(0xFF6B6B72) to Color.White, Color(0xFFE3E3E6) to MatteBlack,
        Color(0xFF1F9D55) to Color.White, Color(0xFFE09A00) to Tinta, Color(0xFF3A3A3F) to OffWhite, Color(0xFFFFE3D6) to Color(0xFF6B1D00)
    )
    val oscuros = listOf(
        OffWhite to MatteBlack, RedOrange to Tinta, Color(0xFFA6A6AD) to Tinta, Color(0xFF46464B) to OffWhite,
        Color(0xFF3DDC84) to Tinta, Color(0xFFFFB020) to Tinta, Color(0xFF5A5A61) to OffWhite, Color(0xFF5A1E08) to Color(0xFFFFD9C9)
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
    CompositionLocalProvider(
        LocalColores provides ColoresAvisos(if (oscuro) EstadosOscuro else EstadosClaro, if (oscuro) SuperficiesOscuro else SuperficiesClaro)
    ) {
        MaterialTheme(colorScheme = esquema, typography = Tipografia, shapes = Formas) {
            /* Sin esto, el texto que no está dentro de un Surface sale negro también en oscuro. */
            CompositionLocalProvider(LocalContentColor provides esquema.onSurface, content = contenido)
        }
    }
}
