package org.antidepressants.mp5.domain.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

/**
 * Represents a user playlist.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val tracks: PersistentList<Track> = persistentListOf(),
    val isCloud: Boolean = false, // true = synced to YouTube/Google, false = local only
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Type of playlist for creation prompt.
 */
enum class PlaylistType {
    CLOUD,  // Edits pushed to user's Google/YouTube account
    LOCAL   // Stored in Room Database (device only)
}
