package org.antidepressants.mp5.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism effect modifiers for the Psychopath UI.
 * 
 * Creates frosted glass appearance with:
 * - Semi-transparent background
 * - Blur effect
 * - Gradient overlays
 */

/**
 * Apply glassmorphism effect to a composable.
 * 
 * @param blurRadius Blur intensity
 * @param backgroundColor Background color (should be semi-transparent)
 */
@Composable
fun Modifier.glassmorphism(
    blurRadius: Dp = 16.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.1f)
): Modifier {
    return this
        .blur(blurRadius)
        .background(backgroundColor)
}

/**
 * Apply glass card effect (for cards, dialogs, etc.).
 */
@Composable
fun Modifier.glassCard(
    borderRadius: Dp = 16.dp,
    isDark: Boolean = true
): Modifier {
    val backgroundColor = if (isDark) {
        Color.Black.copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.3f)
    }
    
    return this
        .blur(8.dp)
        .background(
            color = backgroundColor,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(borderRadius)
        )
}

/**
 * Apply gradient overlay for depth effect.
 */
fun Modifier.gradientOverlay(
    colors: List<Color> = listOf(
        Color.Transparent,
        Color.Black.copy(alpha = 0.5f)
    ),
    isVertical: Boolean = true
): Modifier {
    val brush = if (isVertical) {
        Brush.verticalGradient(colors)
    } else {
        Brush.horizontalGradient(colors)
    }
    
    return this.background(brush)
}

/**
 * Sidebar glass effect (vertical blur).
 */
@Composable
fun Modifier.sidebarGlass(): Modifier {
    return this
        .blur(20.dp)
        .background(
            color = Color.Black.copy(alpha = 0.4f)
        )
}

/**
 * Player bar glass effect.
 */
@Composable
fun Modifier.playerBarGlass(): Modifier {
    return this
        .blur(24.dp)
        .background(
            Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.6f),
                    Color.Black.copy(alpha = 0.4f),
                    Color.Black.copy(alpha = 0.6f)
                )
            )
        )
}
