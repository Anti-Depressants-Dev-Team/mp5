package org.antidepressants.mp5.data.history

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.antidepressants.mp5.domain.model.Track
import org.antidepressants.mp5.settings.GlobalSettings

/**
 * Manages play history - tracks that have been played before.
 * Persists to settings and provides random "Play Again" suggestions.
 */
object PlayHistory {
    
    private const val MAX_HISTORY_SIZE = 100
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    @Serializable
    data class HistoryEntry(
        val trackId: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val duration: Long,
        val album: String? = null,
        val playedAt: Long = System.currentTimeMillis()
    )
    
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()
    
    init {
        loadFromStorage()
    }
    
    /**
     * Record a track as played.
     * Deduplicates by track ID (only keeps most recent play).
     */
    fun recordPlay(track: Track) {
        val entry = HistoryEntry(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            thumbnailUrl = track.thumbnailUrl,
            duration = track.duration,
            album = track.album
        )
        
        // Remove existing entry for this track (if any)
        val updated = _history.value.filter { it.trackId != track.id }.toMutableList()
        
        // Add new entry at the beginning
        updated.add(0, entry)
        
        // Trim to max size
        if (updated.size > MAX_HISTORY_SIZE) {
            updated.removeAt(updated.lastIndex)
        }
        
        _history.value = updated
        saveToStorage()
        
        System.err.println("[PlayHistory] Recorded: ${track.title} (Total: ${updated.size})")
    }
    
    /**
     * Get random tracks from history for "Play Again" section.
     * Excludes the currently playing track if provided.
     */
    fun getPlayAgainSuggestions(count: Int = 10, excludeTrackId: String? = null): List<HistoryEntry> {
        val candidates = if (excludeTrackId != null) {
            _history.value.filter { it.trackId != excludeTrackId }
        } else {
            _history.value
        }
        
        return candidates.shuffled().take(count)
    }
    
    /**
     * Convert history entry back to Track for playback.
     */
    fun HistoryEntry.toTrack(): Track {
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            thumbnailUrl = thumbnailUrl,
            duration = duration
        )
    }
    
    /**
     * Clear all history.
     */
    fun clearHistory() {
        _history.value = emptyList()
        saveToStorage()
    }
    
    private fun loadFromStorage() {
        try {
            val stored = GlobalSettings.settings.playHistory
            if (!stored.isNullOrBlank()) {
                _history.value = json.decodeFromString<List<HistoryEntry>>(stored)
                System.err.println("[PlayHistory] Loaded ${_history.value.size} entries from storage")
            }
        } catch (e: Exception) {
            System.err.println("[PlayHistory] Failed to load: ${e.message}")
            _history.value = emptyList()
        }
    }
    
    private fun saveToStorage() {
        try {
            val encoded = json.encodeToString(_history.value)
            GlobalSettings.settings.playHistory = encoded
        } catch (e: Exception) {
            System.err.println("[PlayHistory] Failed to save: ${e.message}")
        }
    }
}
