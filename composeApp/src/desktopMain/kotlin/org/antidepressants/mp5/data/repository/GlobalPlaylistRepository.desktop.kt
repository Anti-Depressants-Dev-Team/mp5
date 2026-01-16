package org.antidepressants.mp5.data.repository

import org.antidepressants.mp5.domain.repository.PlaylistRepository

actual object GlobalPlaylistRepository {
    actual val instance: PlaylistRepository = PersistentPlaylistRepository()
}
