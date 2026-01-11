package org.antidepressants.mp5.data.lyrics

/**
 * Interface for lyrics providers.
 * Implementations: GeniusProvider, MusixmatchProvider, LrcLibProvider
 */
interface LyricsProvider {
    /**
     * Provider name for logging/debugging.
     */
    val name: String

    /**
     * Search for lyrics by track title and artist.
     * @param title Track title
     * @param artist Artist name
     * @return Lyrics text on success, or failure if not found
     */
    suspend fun searchLyrics(title: String, artist: String): Result<Lyrics>

    /**
     * Check if this provider is available.
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Lyrics data class supporting both plain text and synced (LRC) lyrics.
 */
data class Lyrics(
    val trackTitle: String,
    val artist: String,
    val plainText: String,
    val syncedLyrics: List<SyncedLine>? = null, // LRC format
    val source: String // Provider name
)

/**
 * A single line of synced lyrics with timestamp.
 */
data class SyncedLine(
    val timeMs: Long, // Start time in milliseconds
    val text: String
)

/**
 * Lyrics aggregator that searches multiple providers until a match is found.
 * Order: Genius -> Musixmatch -> LRCLIB
 */
class LyricsAggregator(
    private val providers: List<LyricsProvider>
) {
    suspend fun findLyrics(title: String, artist: String): Result<Lyrics> {
        for (provider in providers) {
            try {
                if (!provider.isAvailable()) continue
                
                val result = provider.searchLyrics(title, artist)
                if (result.isSuccess) {
                    return result
                }
            } catch (e: Exception) {
                println("[LyricsAggregator] ${provider.name} failed: ${e.message}")
            }
        }
        return Result.failure(Exception("Lyrics not found on any provider"))
    }
}
