package org.antidepressants.mp5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.antidepressants.mp5.data.repository.GlobalPlaylistRepository
import org.antidepressants.mp5.domain.model.Playlist
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.domain.repository.PlaylistRepository
import org.antidepressants.mp5.util.saveTextToFile
import org.antidepressants.mp5.util.loadTextFromFile

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier
) {
    val repository = remember { GlobalPlaylistRepository.instance }
    val playlists by repository.getAllPlaylists().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with create button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📚 Playlists",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colors.onBackground
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Export All button
                var showExportMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, "Export", tint = MaterialTheme.colors.onBackground)
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            showExportMenu = false
                            scope.launch {
                                val json = org.antidepressants.mp5.data.export.PlaylistExporter.exportAllToJson(playlists)
                                saveTextToFile("playlists_export.json", json)
                            }
                        }) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export All (JSON)")
                        }
                    }
                }
                
                // Import button
                IconButton(onClick = {
                    scope.launch {
                        val json = loadTextFromFile()
                        if (json != null) {
                            // Try library import first, then single playlist
                            val libraryResult = org.antidepressants.mp5.data.export.PlaylistExporter.importLibraryFromJson(json)
                            if (libraryResult.isSuccess) {
                                libraryResult.getOrNull()?.forEach { playlist ->
                                    repository.importPlaylist(playlist)
                                }
                            } else {
                                val playlistResult = org.antidepressants.mp5.data.export.PlaylistExporter.importFromJson(json)
                                playlistResult.getOrNull()?.let { playlist ->
                                    repository.importPlaylist(playlist)
                                }
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.FileOpen, "Import", tint = MaterialTheme.colors.onBackground)
                }
                
                // New Playlist button
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Playlist")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Playlists list
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No playlists yet",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        "Create one to organize your music",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { selectedPlaylist = playlist },
                        onDelete = {
                            scope.launch {
                                repository.deletePlaylist(playlist.id)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Create playlist dialog
    if (showCreateDialog) {
        PlaylistDialog(
            title = "Create Playlist",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, isCloud ->
                scope.launch {
                    repository.createPlaylist(name, description, isCloud)
                    showCreateDialog = false
                }
            }
        )
    }
    
    // Playlist detail view
    selectedPlaylist?.let { playlist ->
        PlaylistDetailView(
            playlist = playlist,
            repository = repository,
            onDismiss = { selectedPlaylist = null }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playlist.isCloud) Icons.Default.Cloud else Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Playlist info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${playlist.tracks.size} tracks",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
                playlist.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

@Composable
private fun PlaylistDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialIsCloud: Boolean = false,
    isCloudEditable: Boolean = true,
    confirmButtonText: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, isCloud: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var isCloud by remember { mutableStateOf(initialIsCloud) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Playlist Type Selection
                if (isCloudEditable) {
                    Text(
                        "Playlist Type",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Local Playlist Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCloud = false },
                            backgroundColor = if (!isCloud) 
                                MaterialTheme.colors.primary.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            elevation = if (!isCloud) 4.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = if (!isCloud) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Local",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = if (!isCloud) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isCloud) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                )
                                Text(
                                    "Device only",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        // Cloud Playlist Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isCloud = true },
                            backgroundColor = if (isCloud) 
                                MaterialTheme.colors.primary.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(12.dp),
                            elevation = if (isCloud) 4.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = if (isCloud) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "YouTube",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = if (isCloud) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCloud) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                                )
                                Text(
                                    "Sync to account",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description.takeIf { it.isNotBlank() }, isCloud)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    repository: PlaylistRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showEditDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    
    // Collect the latest version of this playlist
    val currentPlaylist by repository.getAllPlaylists()
        .collectAsState(emptyList())
        .let { state ->
            derivedStateOf { state.value.find { it.id == playlist.id } ?: playlist }
        }

    if (showEditDialog) {
        PlaylistDialog(
            title = "Edit Playlist",
            initialName = currentPlaylist.name,
            initialDescription = currentPlaylist.description ?: "",
            initialIsCloud = currentPlaylist.isCloud,
            isCloudEditable = false,
            confirmButtonText = "Save",
            onDismiss = { showEditDialog = false },
            onConfirm = { name, description, _ ->
                scope.launch {
                    repository.updatePlaylist(currentPlaylist.copy(name = name, description = description))
                    showEditDialog = false
                }
            }
        )
    }
    
    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text(
                    currentPlaylist.name,
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    // Edit Mode Toggle
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            if (isEditMode) Icons.Default.CheckCircle else Icons.Default.Reorder,
                            "Reorder",
                            tint = if (isEditMode) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                        )
                    }
                    // Edit Metadata
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tracks
            if (playlist.tracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No tracks yet",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "Add songs from search",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Track loading state
                var loadingTrackId by remember { mutableStateOf<String?>(null) }
                val youtubeProvider = remember { org.antidepressants.mp5.data.provider.YouTubeProvider() }
                val playerController = remember { org.antidepressants.mp5.player.DemoPlayer.controller }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(currentPlaylist.tracks) { index, track ->
                        TrackItem(
                            track = track,
                            index = index,
                            totalCount = currentPlaylist.tracks.size,
                            isLoading = loadingTrackId == track.id,
                            isEditMode = isEditMode,
                            onPlay = {
                                if (loadingTrackId != null) return@TrackItem
                                scope.launch {
                                    loadingTrackId = track.id
                                    try {
                                        val streamResult = youtubeProvider.getStream(track.id)
                                        streamResult.onSuccess { streamInfo ->
                                            playerController.loadTrack(track, streamInfo.streamUrl)
                                        }.onFailure { error ->
                                            println("[PlaylistScreen] Failed to load: ${error.message}")
                                        }
                                    } finally {
                                        loadingTrackId = null
                                    }
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    repository.removeTrackFromPlaylist(currentPlaylist.id, track.id)
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    scope.launch {
                                        repository.reorderTracks(currentPlaylist.id, index, index - 1)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < currentPlaylist.tracks.size - 1) {
                                    scope.launch {
                                        repository.reorderTracks(currentPlaylist.id, index, index + 1)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    index: Int = 0,
    totalCount: Int = 0,
    isLoading: Boolean = false,
    isEditMode: Boolean = false,
    onPlay: () -> Unit = {},
    onRemove: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditMode) { onPlay() },
        backgroundColor = MaterialTheme.colors.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                // Reorder controls
                Column {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (index > 0) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (index < totalCount - 1) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                // Play button
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colors.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}
