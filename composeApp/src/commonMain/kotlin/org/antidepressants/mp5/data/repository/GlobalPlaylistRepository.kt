package org.antidepressants.mp5.data.repository

import org.antidepressants.mp5.domain.repository.PlaylistRepository

expect object GlobalPlaylistRepository {
    val instance: PlaylistRepository
}
