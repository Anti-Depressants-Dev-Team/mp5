package org.antidepressants.mp5.data.provider

/**
 * Global singleton for accessing the music provider.
 * Uses YouTubeProvider by default (via NewPipeExtractor fallback chain).
 */
object GlobalProvider {
    val provider: MusicProvider by lazy { YouTubeProvider() }
}
