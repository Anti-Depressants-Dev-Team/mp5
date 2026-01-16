package org.antidepressants.mp5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.antidepressants.mp5.data.lyrics.Lyrics
import org.antidepressants.mp5.data.lyrics.SyncedLine

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    currentPosition: Long,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Debug: Log what we receive
    println("[LyricsView] lyrics=${lyrics != null}, isLoading=$isLoading, plainText=${lyrics?.plainText?.take(50)}, syncedCount=${lyrics?.syncedLyrics?.size}")
    
    if (lyrics == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Searching for lyrics...",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        "No lyrics found",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "Try another song or check your connection",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
        return
    }

    if (!lyrics.syncedLyrics.isNullOrEmpty()) {
        SyncedLyricsView(lyrics.syncedLyrics, currentPosition, modifier)
    } else {
        PlainLyricsView(lyrics.plainText, modifier)
    }
}

@Composable
private fun SyncedLyricsView(
    syncedLines: List<SyncedLine>,
    currentPosition: Long,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    
    // Find active line index
    val activeIndex = syncedLines.indexOfLast { it.timeMs <= currentPosition }

    // Auto-scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            // Scroll to center the active line (approx)
            // 3 lines offset is a rough guess for "center" in a typical list
            val scrollIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(scrollIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(syncedLines) { index, line ->
            val isActive = index == activeIndex
            
            // Animated values for smooth transitions
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.15f else 1f,
                animationSpec = tween(300)
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.5f,
                animationSpec = tween(300)
            )
            val offsetX by animateFloatAsState(
                targetValue = if (isActive) 0f else 10f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                )
            )
            
            Text(
                text = line.text,
                style = if (isActive) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = Color.White.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX
                    )
            )
        }
        
        // Bottom padding to allow scrolling last item to center
        item { 
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun PlainLyricsView(
    text: String,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = text,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }
    }
}
