package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PsychopathPlayer(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colors.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Track Info
        Column(
            modifier = Modifier.weight(0.3f)
        ) {
            Text(
                "Psychopath Player",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                "Artist Name",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        // Controls
        Column(
            modifier = Modifier.weight(0.4f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = MaterialTheme.colors.onSurface)
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.PlayArrow, 
                        "Play", 
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipNext, "Next", tint = MaterialTheme.colors.onSurface)
                }
            }
        }

        // Seek Bar (simplified for now) & Volume
        Row(
            modifier = Modifier.weight(0.3f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = 0.3f,
                onValueChange = {},
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )
        }
    }
}
