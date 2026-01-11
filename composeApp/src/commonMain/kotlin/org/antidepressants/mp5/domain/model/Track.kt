package org.antidepressants.mp5.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a playable music track.
 */
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long, // in milliseconds
    val thumbnailUrl: String? = null,
    val streamUrl: String? = null,
    val source: MusicSource = MusicSource.YOUTUBE
)

/**
 * Music source provider enum.
 */
@Serializable
enum class MusicSource {
    YOUTUBE,
    SOUNDCLOUD,
    PIPED,
    LOCAL
}
