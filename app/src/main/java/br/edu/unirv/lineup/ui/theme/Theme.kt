package br.edu.unirv.lineup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Paleta {
    val Marinho = Color(0xFF0B2545)
    val MarinhoMedio = Color(0xFF13315C)
    val Aco = Color(0xFF3E6990)
    val Neblina = Color(0xFFEEF2F6)
    val Linha = Color(0xFFD5DCE4)
    val Texto = Color(0xFF1B2733)
    val TextoSuave = Color(0xFF5B6B7B)

    val Online = Color(0xFF1E8E4E)
    val Offline = Color(0xFFD9822B)
    val Erro = Color(0xFFB3261E)

    val MareBaixa = Color(0xFFB9C6D6)
}

private val EsquemaClaro = lightColorScheme(
    primary = Paleta.Marinho,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E2F0),
    onPrimaryContainer = Paleta.Marinho,
    secondary = Paleta.Aco,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E9F2),
    onSecondaryContainer = Paleta.MarinhoMedio,
    tertiary = Color(0xFF8C5A1E),
    background = Color(0xFFF6F8FA),
    onBackground = Paleta.Texto,
    surface = Color.White,
    onSurface = Paleta.Texto,
    surfaceVariant = Paleta.Neblina,
    onSurfaceVariant = Paleta.TextoSuave,
    outline = Paleta.Linha,
    error = Paleta.Erro
)

private val EsquemaEscuro = darkColorScheme(
    primary = Color(0xFFA9C4E2),
    onPrimary = Paleta.Marinho,
    primaryContainer = Paleta.MarinhoMedio,
    onPrimaryContainer = Color(0xFFD6E2F0),
    secondary = Color(0xFF8DB0D3),
    background = Color(0xFF0E1620),
    surface = Color(0xFF151F2B),
    surfaceVariant = Color(0xFF1F2B3A),
    onSurfaceVariant = Color(0xFFA7B4C2),
    outline = Color(0xFF334355)
)

private val Tipografia = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun LineupTheme(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
        typography = Tipografia,
        content = conteudo
    )
}
