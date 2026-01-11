package org.antidepressants.mp5.domain.model

import kotlinx.serialization.Serializable

/**
 * Stream information returned from a music provider.
 */
@Serializable
data class StreamInfo(
    val trackId: String,
    val streamUrl: String,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val expiresAt: Long? = null, // Unix timestamp
    val source: MusicSource
)
