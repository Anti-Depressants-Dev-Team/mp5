package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Navigation destination for the sidebar.
 */
enum class NavigationDestination(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    PLAYLISTS("Playlists", Icons.Default.PlaylistPlay)
}

/**
 * Sidebar navigation component with glassmorphism effect.
 * 
 * Structure:
 * - Top: App logo/title
 * - Middle: Navigation items (Home, Search, Playlists)
 * - Bottom: Settings (pinned)
 */
@Composable
fun Sidebar(
    currentDestination: NavigationDestination = NavigationDestination.HOME,
    onNavigate: (NavigationDestination) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.15f),
                            Color.Transparent,
                            MaterialTheme.colors.primary.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // App title/logo
                Text(
                    text = "Psychopath",
                    style = MaterialTheme.typography.h2.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Navigation items
                NavigationDestination.entries.forEach { destination ->
                    SidebarItem(
                        title = destination.title,
                        icon = destination.icon,
                        isSelected = currentDestination == destination,
                        onClick = { onNavigate(destination) }
                    )
                }
                
                // Spacer to push settings to bottom
                Spacer(modifier = Modifier.weight(1f))
                
                // Settings (pinned to bottom)
                Divider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                SidebarItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    isSelected = false,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.body1.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor
        )
    }
}
