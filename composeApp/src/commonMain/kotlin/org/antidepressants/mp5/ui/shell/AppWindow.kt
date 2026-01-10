package org.antidepressants.mp5.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.antidepressants.mp5.ui.components.PsychopathPlayer
import org.antidepressants.mp5.ui.components.Sidebar

@Composable
fun AppWindow() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Sidebar
        Sidebar()

        // Main Content Area
        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            // Top Docked Player
            PsychopathPlayer()

            // Content
            Box(modifier = Modifier.weight(1f)) {
                // Feature content (Home, Search, etc.) will go here
            }
        }
    }
}
