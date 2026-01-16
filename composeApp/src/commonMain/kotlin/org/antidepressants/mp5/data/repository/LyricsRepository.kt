package org.antidepressants.mp5.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.antidepressants.mp5.data.lyrics.LrcLibLyricsProvider
import org.antidepressants.mp5.data.lyrics.Lyrics
import org.antidepressants.mp5.data.lyrics.LyricsAggregator
import org.antidepressants.mp5.data.lyrics.LyricsProvider

/**
 * Repository for fetching lyrics.
 * Uses LyricsAggregator to try multiple sources.
 */
class LyricsRepository {
    
    private val lrcLibProvider = LrcLibLyricsProvider()
    private val geniusProvider = org.antidepressants.mp5.data.lyrics.GeniusLyricsProvider()
    
    // In the future we can add MusixmatchProvider here
    private val providers: List<LyricsProvider> = listOf(
        lrcLibProvider,
        geniusProvider
    )
    
    private val aggregator = LyricsAggregator(providers)
    
    suspend fun getLyrics(title: String, artist: String): Result<Lyrics> = withContext(Dispatchers.IO) {
        return@withContext aggregator.findLyrics(title, artist)
    }
}
