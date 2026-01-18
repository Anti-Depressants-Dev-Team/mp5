package org.antidepressants.mp5.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.antidepressants.mp5.settings.GlobalSettings
import org.antidepressants.mp5.ui.components.NavigationDestination
import org.antidepressants.mp5.ui.components.PsychopathPlayer
import org.antidepressants.mp5.ui.components.Sidebar
import org.antidepressants.mp5.ui.screens.SettingsScreen
import org.antidepressants.mp5.ui.screens.SearchScreen
import org.antidepressants.mp5.ui.screens.PlaylistScreen

@Composable
fun AppWindow() {
    // Navigation state
    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Observe settings state reactively - player position changes immediately!
    val settingsState by GlobalSettings.settings.state.collectAsState()
    
    // Responsive layout: Use BoxWithConstraints to detect mobile vs desktop
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactScreen = maxWidth < 600.dp // Mobile threshold
        
        if (isCompactScreen) {
            // Mobile Layout: Bottom navigation
            MobileLayout(
                currentDestination = currentDestination,
                showSettings = showSettings,
                settingsState = settingsState,
                onNavigate = { destination ->
                    currentDestination = destination
                    showSettings = false
                },
                onSettingsClick = { showSettings = true }
            )
        } else {
            // Desktop Layout: Sidebar navigation
            DesktopLayout(
                currentDestination = currentDestination,
                showSettings = showSettings,
                settingsState = settingsState,
                onNavigate = { destination ->
                    currentDestination = destination
                    showSettings = false
                },
                onSettingsClick = { showSettings = true }
            )
        }
    }
}

@Composable
private fun MobileLayout(
    currentDestination: NavigationDestination,
    showSettings: Boolean,
    settingsState: org.antidepressants.mp5.settings.SettingsState,
    onNavigate: (NavigationDestination) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Player position based on Psychopath mode setting
        if (settingsState.isPlayerTop) {
            PsychopathPlayer()
        }
        
        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
                .padding(if (showSettings) 0.dp else 16.dp)
        ) {
            if (showSettings) {
                SettingsScreen(modifier = Modifier.fillMaxSize())
            } else {
                when (currentDestination) {
                    NavigationDestination.HOME -> HomeContent()
                    NavigationDestination.SEARCH -> SearchScreen()
                    NavigationDestination.PLAYLISTS -> PlaylistScreen()
                }
            }
        }
        
        // Bottom Player (Normal mode)
        if (!settingsState.isPlayerTop) {
            PsychopathPlayer()
        }
        
        // Bottom Navigation Bar
        BottomNavigation(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface
        ) {
            NavigationDestination.entries.forEach { destination ->
                BottomNavigationItem(
                    icon = { Icon(destination.icon, contentDescription = destination.title) },
                    label = { Text(destination.title, style = MaterialTheme.typography.caption) },
                    selected = currentDestination == destination && !showSettings,
                    onClick = { onNavigate(destination) },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            // Settings in bottom nav
            BottomNavigationItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings", style = MaterialTheme.typography.caption) },
                selected = showSettings,
                onClick = { onSettingsClick() },
                selectedContentColor = MaterialTheme.colors.primary,
                unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DesktopLayout(
    currentDestination: NavigationDestination,
    showSettings: Boolean,
    settingsState: org.antidepressants.mp5.settings.SettingsState,
    onNavigate: (NavigationDestination) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Sidebar with navigation wired up
        Sidebar(
            currentDestination = currentDestination,
            onNavigate = onNavigate,
            onSettingsClick = onSettingsClick
        )

        // Main Content Area
        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            // Player position based on Psychopath mode setting (reactive!)
            if (settingsState.isPlayerTop) {
                // Top Docked Player (Psychopath position - default)
                PsychopathPlayer()
            }

            // Content based on navigation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background)
                    .padding(if (showSettings) 0.dp else 24.dp)
            ) {
                if (showSettings) {
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    when (currentDestination) {
                        NavigationDestination.HOME -> HomeContent()
                        NavigationDestination.SEARCH -> SearchScreen()
                        NavigationDestination.PLAYLISTS -> PlaylistScreen()
                    }
                }
            }
            
            // Bottom Player (Normal mode - when Psychopath mode is OFF)
            if (!settingsState.isPlayerTop) {
                PsychopathPlayer()
            }
        }
    }
}

@Composable
private fun HomeContent() {
    val historyState by org.antidepressants.mp5.data.history.PlayHistory.history.collectAsState()
    val playerState by org.antidepressants.mp5.player.DemoPlayer.controller.playerState.collectAsState()
    
    // Get random suggestions excluding current track
    val playAgainSuggestions = remember(historyState, playerState.currentTrack?.id) {
        org.antidepressants.mp5.data.history.PlayHistory.getPlayAgainSuggestions(
            count = 10,
            excludeTrackId = playerState.currentTrack?.id
        )
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Text(
            "🏠 Home",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (playAgainSuggestions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "🎵",
                        style = MaterialTheme.typography.h2
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No play history yet!",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onBackground
                    )
                    Text(
                        "Search and play some songs to see them here.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Play Again Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔄 Play Again",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${historyState.size} songs in history",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horizontal scrollable list of suggestions
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playAgainSuggestions.size) { index ->
                    val entry = playAgainSuggestions[index]
                    PlayAgainCard(entry) {
                        // Play this track
                        val track = with(org.antidepressants.mp5.data.history.PlayHistory) { entry.toTrack() }
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            org.antidepressants.mp5.player.DemoPlayer.controller.playTrack(track)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayAgainCard(
    entry: org.antidepressants.mp5.data.history.PlayHistory.HistoryEntry,
    onClick: () -> Unit
) {
    androidx.compose.material.Card(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp),
        elevation = 4.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable { onClick() }
        ) {
            // Thumbnail or placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (entry.thumbnailUrl != null) {
                    coil3.compose.AsyncImage(
                        model = entry.thumbnailUrl,
                        contentDescription = entry.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text("🎵", style = MaterialTheme.typography.h4)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                entry.title,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            // Artist
            Text(
                entry.artist,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🔍 Search",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Search functionality coming soon...\n\nThis will use NewPipeExtractor to search YouTube.",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PlaylistsContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "📋 Playlists",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your playlists will appear here.\n\nSupports both Cloud and Local playlists.",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
    }
}
