package org.antidepressants.mp5.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.antidepressants.mp5.settings.GlobalSettings
import org.antidepressants.mp5.ui.components.NavigationDestination
import org.antidepressants.mp5.ui.components.PsychopathPlayer
import org.antidepressants.mp5.ui.components.Sidebar
import org.antidepressants.mp5.ui.screens.SettingsScreen

@Composable
fun AppWindow() {
    // Navigation state
    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Observe settings state reactively - player position changes immediately!
    val settingsState by GlobalSettings.settings.state.collectAsState()
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Sidebar with navigation wired up
        Sidebar(
            currentDestination = currentDestination,
            onNavigate = { destination ->
                currentDestination = destination
                showSettings = false
            },
            onSettingsClick = {
                showSettings = true
            }
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
                        NavigationDestination.SEARCH -> SearchContent()
                        NavigationDestination.PLAYLISTS -> PlaylistsContent()
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🏠 Home",
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Welcome to Psychopath Player!\n\nClick the play button above to test playback.\nGo to Settings to customize your experience.\n\n✨ Settings changes now apply immediately!",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)
        )
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
