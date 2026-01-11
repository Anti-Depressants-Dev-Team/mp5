package org.antidepressants.mp5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.antidepressants.mp5.settings.GlobalSettings

/**
 * Preset accent colors for quick selection.
 */
val presetColors = listOf(
    0xFF9C27B0L to "Purple",      // Default
    0xFF2196F3L to "Blue",
    0xFF4CAF50L to "Green",
    0xFFE91E63L to "Pink",
    0xFFFF5722L to "Orange",
    0xFFFF9800L to "Amber",
    0xFF00BCD4L to "Cyan",
    0xFF673AB7L to "Deep Purple",
    0xFFF44336L to "Red",
    0xFF009688L to "Teal"
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Local state that syncs with GlobalSettings
    var isDarkMode by remember { mutableStateOf(GlobalSettings.settings.isDarkMode) }
    var isPlayerTop by remember { mutableStateOf(GlobalSettings.settings.isPlayerTop) }
    var accentColor by remember { mutableStateOf(GlobalSettings.settings.accentColor) }
    var volumeBoost by remember { mutableStateOf(GlobalSettings.settings.volumeBoost) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "⚙️ Settings",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.onBackground
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === APPEARANCE SECTION ===
        SettingsSection(title = "Appearance") {
            // Dark Mode Toggle
            SettingsSwitch(
                title = "Dark Mode",
                subtitle = "Use dark theme (recommended)",
                checked = isDarkMode,
                onCheckedChange = { 
                    isDarkMode = it
                    GlobalSettings.settings.isDarkMode = it
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accent Color Picker
            Text(
                "Accent Color",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                "Choose your primary color",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presetColors) { (color, name) ->
                    ColorCircle(
                        color = Color(color.toInt()),
                        isSelected = accentColor == color,
                        onClick = {
                            accentColor = color
                            GlobalSettings.settings.accentColor = color
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === PLAYER SECTION ===
        SettingsSection(title = "Player") {
            // Psychopath Mode (Player Position)
            SettingsSwitch(
                title = "Psychopath Mode",
                subtitle = if (isPlayerTop) "Player bar at TOP (enabled)" else "Player bar at BOTTOM (normal)",
                checked = isPlayerTop,
                onCheckedChange = { 
                    isPlayerTop = it
                    GlobalSettings.settings.isPlayerTop = it
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Volume Boost
            Text(
                "Volume Boost: ${volumeBoost}%",
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onBackground
            )
            Text(
                "Boost audio above 100% (may cause distortion)",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = volumeBoost.toFloat(),
                onValueChange = { 
                    volumeBoost = it.toInt()
                    GlobalSettings.settings.volumeBoost = it.toInt()
                },
                valueRange = 100f..200f,
                steps = 9, // 10% increments
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === INTEGRATIONS SECTION ===
        SettingsSection(title = "Integrations (Coming Soon)") {
            SettingsSwitch(
                title = "Discord Rich Presence",
                subtitle = "Show what you're playing on Discord",
                checked = false,
                enabled = false,
                onCheckedChange = { }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsSwitch(
                title = "Last.fm Scrobbling",
                subtitle = "Track your listening history",
                checked = false,
                enabled = false,
                onCheckedChange = { }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsSwitch(
                title = "ListenBrainz",
                subtitle = "Open-source music tracking",
                checked = false,
                enabled = false,
                onCheckedChange = { }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Version info
        Text(
            "Psychopath Player v1.0.0",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colors.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.subtitle1,
                color = if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.caption,
                color = if (enabled) MaterialTheme.colors.onSurface.copy(alpha = 0.6f) else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, Color.White, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
