package org.antidepressants.mp5.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Default Purple Accent
val PurpleAccent = Color(0xFF9C27B0)
val PurpleAccentLight = Color(0xFFCE93D8)
val PurpleAccentDark = Color(0xFF7B1FA2)

// Dark theme colors with glassmorphism support
private val DarkColorPalette = darkColors(
    primary = PurpleAccent,
    primaryVariant = PurpleAccentDark,
    secondary = PurpleAccentLight,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorPalette = lightColors(
    primary = PurpleAccent,
    primaryVariant = PurpleAccentDark,
    secondary = PurpleAccentLight,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val AppTypography = Typography(
    h1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    h2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    h3 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    body1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

/**
 * Psychopath theme with glassmorphism support.
 * 
 * @param accentColor Custom accent color (ARGB long)
 * @param darkTheme Whether to use dark theme
 * @param content Composable content
 */
@Composable
fun PsychopathTheme(
    accentColor: Long = 0xFF9C27B0L,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customAccent = Color(accentColor.toInt())
    
    val colors = if (darkTheme) {
        DarkColorPalette.copy(
            primary = customAccent,
            primaryVariant = customAccent.copy(alpha = 0.7f),
            secondary = customAccent.copy(alpha = 0.8f)
        )
    } else {
        LightColorPalette.copy(
            primary = customAccent,
            primaryVariant = customAccent.copy(alpha = 0.7f),
            secondary = customAccent.copy(alpha = 0.8f)
        )
    }
    
    MaterialTheme(
        colors = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
