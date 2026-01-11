package org.antidepressants.mp5.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import org.antidepressants.mp5.settings.GlobalSettings

private val DarkColorPalette = darkColors(
    primary = PrimaryPurple,
    primaryVariant = PrimaryPurpleVariant,
    secondary = SecondaryTeal,
    background = DeepGlass,
    surface = GlassSurface,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

private val LightColorPalette = lightColors(
    primary = Purple40,
    primaryVariant = PurpleGrey40,
    secondary = Pink40,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

/**
 * Mp5 Theme with support for:
 * - Dark/Light mode toggle (dark is default)
 * - Dynamic accent color from settings
 * - REACTIVE: Changes take effect immediately via StateFlow observation
 */
@Composable
fun Mp5Theme(
    content: @Composable () -> Unit
) {
    // Observe settings state reactively - UI will recompose when these change
    val settingsState by GlobalSettings.settings.state.collectAsState()
    
    val customAccent = Color(settingsState.accentColor.toInt())
    
    val colors = if (settingsState.isDarkMode) {
        DarkColorPalette.copy(
            primary = customAccent,
            primaryVariant = customAccent.copy(alpha = 0.8f),
            secondary = customAccent.copy(alpha = 0.6f)
        )
    } else {
        LightColorPalette.copy(
            primary = customAccent,
            primaryVariant = customAccent.copy(alpha = 0.8f),
            secondary = customAccent.copy(alpha = 0.6f)
        )
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
