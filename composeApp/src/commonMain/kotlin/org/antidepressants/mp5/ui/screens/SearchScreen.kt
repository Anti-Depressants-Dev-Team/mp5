package org.antidepressants.mp5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.antidepressants.mp5.data.repository.MusicRepository
import org.antidepressants.mp5.data.repository.GlobalPlaylistRepository
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.player.DemoPlayer

import coil3.compose.AsyncImage

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val musicRepository = remember { MusicRepository() }
    val playlistRepository = remember { GlobalPlaylistRepository.instance }
    val playlists by playlistRepository.getAllPlaylists().collectAsState(emptyList())

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }

    // Fetch suggestions as user types
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            showSuggestions = true
            val result = musicRepository.getSuggestions(searchQuery)
            result.fold(
                onSuccess = { suggestions = it },
                onFailure = { suggestions = emptyList() }
            )
        } else {
            suggestions = emptyList()
            showSuggestions = false
        }
    }

    fun performSearch(query: String = searchQuery) {
        if (query.isBlank()) return

        showSuggestions = false
        searchQuery = query

        scope.launch {
            isLoading = true
            errorMessage = null

            val result = musicRepository.search(query)

            result.fold(
                onSuccess = { tracks ->
                    searchResults = tracks
                    if (tracks.isEmpty()) {
                        errorMessage = "No results found"
                    }
                },
                onFailure = { error ->
                    errorMessage = "Search failed: ${error.message}"
                    searchResults = emptyList()
                }
            )

            isLoading = false
        }
    }
    
    fun playTrack(track: Track) {
        scope.launch {
            // Get stream URL and play (with fallback)
            val streamResult = musicRepository.getStream(track)
            streamResult.fold(
                onSuccess = { streamInfo ->
                    DemoPlayer.controller.loadTrack(track, streamInfo.streamUrl)
                    DemoPlayer.controller.play()
                },
                onFailure = { error ->
                    errorMessage = "Failed to play: ${error.message}"
                }
            )
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Header
        Text(
            "🔍 Search",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    "Search for songs, artists...",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colors.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = MaterialTheme.colors.surface,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                textColor = MaterialTheme.colors.onSurface,
                cursorColor = MaterialTheme.colors.primary
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search Button
        Button(
            onClick = { performSearch() },
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching...")
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search YouTube")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Suggestions dropdown
        if (showSuggestions && suggestions.isNotEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column {
                    suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    performSearch(suggestion)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                        if (suggestion != suggestions.last()) {
                            Divider(
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        error,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Results List
        if (searchResults.isNotEmpty()) {
            Text(
                "${searchResults.size} results",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { track ->
                SearchResultItem(
                    track = track,
                    onClick = { playTrack(track) },
                    onAddToPlaylist = {
                        selectedTrackForPlaylist = track
                        showPlaylistDialog = true
                    }
                )
            }
        }
    }

    // Add to Playlist Dialog
    if (showPlaylistDialog && selectedTrackForPlaylist != null) {
        AddToPlaylistDialog(
            track = selectedTrackForPlaylist!!,
            playlists = playlists,
            onDismiss = {
                showPlaylistDialog = false
                selectedTrackForPlaylist = null
            },
            onAddToPlaylist = { playlist ->
                scope.launch {
                    playlistRepository.addTrackToPlaylist(playlist.id, selectedTrackForPlaylist!!)
                    showPlaylistDialog = false
                    selectedTrackForPlaylist = null
                }
            }
        )
    }
}

@Composable
private fun SearchResultItem(
    track: Track,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDuration(track.duration),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            }
            
            // Add to Playlist button
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Play button
            IconButton(onClick = onClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add to Playlist")
        },
        text = {
            Column {
                Text(
                    "Select a playlist for \"${track.title}\"",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Text(
                        "No playlists found. Create one first!",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToPlaylist(playlist)
                                    },
                                backgroundColor = MaterialTheme.colors.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (playlist.isCloud) Icons.Default.Cloud else Icons.Default.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            playlist.name,
                                            style = MaterialTheme.typography.body1
                                        )
                                        Text(
                                            "${playlist.tracks.size} tracks",
                                            style = MaterialTheme.typography.caption,
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
