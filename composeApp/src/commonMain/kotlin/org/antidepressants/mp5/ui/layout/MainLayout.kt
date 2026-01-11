package org.antidepressants.mp5.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.antidepressants.mp5.player.PlayerState
import org.antidepressants.mp5.ui.components.NavigationDestination
import org.antidepressants.mp5.ui.components.PlayerBar
import org.antidepressants.mp5.ui.components.Sidebar

/**
 * Main layout composable implementing the "Psychopath" player toggle.
 * 
 * When isPlayerTop is:
 * - true (Default/Psychopath): Player bar renders at the TOP of the screen
 * - false (Normal): Player bar renders at the BOTTOM of the screen
 * 
 * Layout structure:
 * ```
 * +-------------------+
 * | [PlayerBar TOP]   | <- if isPlayerTop
 * +--------+----------+
 * |        |          |
 * | Side   | Content  |
 * | bar    |          |
 * |        |          |
 * +--------+----------+
 * | [PlayerBar BOTTOM]| <- if !isPlayerTop
 * +-------------------+
 * ```
 * 
 * @param isPlayerTop Whether to render player at top (Psychopath mode) or bottom
 * @param playerState Current player state for the PlayerBar
 * @param currentDestination Current navigation destination
 * @param onNavigate Callback when navigation destination changes
 * @param onSettingsClick Callback when settings is clicked
 * @param onPlayPause Play/pause toggle callback
 * @param onNext Skip to next track callback
 * @param onPrevious Skip to previous track callback
 * @param onSeek Seek position callback (0.0 to 1.0)
 * @param onShuffleToggle Toggle shuffle mode callback
 * @param onRepeatToggle Toggle repeat mode callback
 * @param content Main content composable
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainLayout(
    isPlayerTop: Boolean,
    playerState: PlayerState,
    currentDestination: NavigationDestination = NavigationDestination.HOME,
    onNavigate: (NavigationDestination) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onShuffleToggle: () -> Unit = {},
    onRepeatToggle: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Handle Android status bar insets
    ) {
        // Player at TOP (Psychopath mode)
        AnimatedContent(
            targetState = isPlayerTop,
            transitionSpec = {
                if (targetState) {
                    // Animate in from top
                    (slideInVertically { -it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                } else {
                    // Animate out to top
                    (slideInVertically { -it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                }
            },
            label = "PlayerTopAnimation"
        ) { showTop ->
            if (showTop) {
                PlayerBar(
                    playerState = playerState,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onShuffleToggle = onShuffleToggle,
                    onRepeatToggle = onRepeatToggle,
                    isTop = true
                )
            }
        }
        
        // Main content area with sidebar
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Sidebar (vertical navigation)
            Sidebar(
                currentDestination = currentDestination,
                onNavigate = onNavigate,
                onSettingsClick = onSettingsClick
            )
            
            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
        
        // Player at BOTTOM (Normal mode)
        AnimatedContent(
            targetState = !isPlayerTop,
            transitionSpec = {
                if (targetState) {
                    // Animate in from bottom
                    (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { it } + fadeOut())
                } else {
                    // Animate out to bottom
                    (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { it } + fadeOut())
                }
            },
            label = "PlayerBottomAnimation"
        ) { showBottom ->
            if (showBottom) {
                PlayerBar(
                    playerState = playerState,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onShuffleToggle = onShuffleToggle,
                    onRepeatToggle = onRepeatToggle,
                    isTop = false,
                    modifier = Modifier.navigationBarsPadding() // Handle Android nav bar
                )
            }
        }
    }
}

/**
 * Simple MainLayout overload for quick prototyping.
 * Uses default player state and navigation callbacks.
 */
@Composable
fun MainLayout(
    isPlayerTop: Boolean = true,
    content: @Composable () -> Unit
) {
    MainLayout(
        isPlayerTop = isPlayerTop,
        playerState = PlayerState(),
        content = content
    )
}
