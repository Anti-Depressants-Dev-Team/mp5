package org.antidepressants.mp5.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reactive settings state that can be observed by Composables.
 */
data class SettingsState(
    val isDarkMode: Boolean = true,
    val isPlayerTop: Boolean = true,
    val accentColor: Long = 0xFF9C27B0L,
    val volumeBoost: Int = 100,
    val isLastFmEnabled: Boolean = false,
    val isListenBrainzEnabled: Boolean = false,
    val isDiscordRpcEnabled: Boolean = false
)

/**
 * Application settings using Multiplatform Settings with reactive StateFlow.
 * Changes are immediately observable by UI components.
 */
class AppSettings(
    private val settings: Settings = Settings()
) {
    
    companion object {
        private const val KEY_PLAYER_TOP = "isPlayerTop"
        private const val KEY_ACCENT_COLOR = "accentColor"
        private const val KEY_VOLUME_BOOST = "volumeBoost"
        private const val KEY_DARK_MODE = "darkMode"
        private const val KEY_LASTFM_ENABLED = "lastFmEnabled"
        private const val KEY_LISTENBRAINZ_ENABLED = "listenBrainzEnabled"
        private const val KEY_DISCORD_RPC_ENABLED = "discordRpcEnabled"
        
        // Default purple accent: 0xFF9C27B0
        private const val DEFAULT_ACCENT_COLOR = 0xFF9C27B0L
    }
    
    // Reactive state that UI can observe
    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    private fun loadInitialState(): SettingsState {
        return SettingsState(
            isDarkMode = settings.getBoolean(KEY_DARK_MODE, defaultValue = true),
            isPlayerTop = settings.getBoolean(KEY_PLAYER_TOP, defaultValue = true),
            accentColor = settings.getLong(KEY_ACCENT_COLOR, defaultValue = DEFAULT_ACCENT_COLOR),
            volumeBoost = settings.getInt(KEY_VOLUME_BOOST, defaultValue = 100),
            isLastFmEnabled = settings.getBoolean(KEY_LASTFM_ENABLED, defaultValue = false),
            isListenBrainzEnabled = settings.getBoolean(KEY_LISTENBRAINZ_ENABLED, defaultValue = false),
            isDiscordRpcEnabled = settings.getBoolean(KEY_DISCORD_RPC_ENABLED, defaultValue = false)
        )
    }
    
    /**
     * "Psychopath" mode: Player bar at the TOP of the screen.
     * Default: true (Psychopath mode)
     */
    var isPlayerTop: Boolean
        get() = _state.value.isPlayerTop
        set(value) {
            settings.putBoolean(KEY_PLAYER_TOP, value)
            _state.update { it.copy(isPlayerTop = value) }
        }
    
    /**
     * Dark mode enabled.
     * Default: true (Dark mode is default)
     */
    var isDarkMode: Boolean
        get() = _state.value.isDarkMode
        set(value) {
            settings.putBoolean(KEY_DARK_MODE, value)
            _state.update { it.copy(isDarkMode = value) }
        }
    
    /**
     * Custom accent color (ARGB).
     * Default: Purple (0xFF9C27B0)
     */
    var accentColor: Long
        get() = _state.value.accentColor
        set(value) {
            settings.putLong(KEY_ACCENT_COLOR, value)
            _state.update { it.copy(accentColor = value) }
        }
    
    /**
     * Volume boost percentage (100 = normal, >100 = boosted).
     * Range: 100-200
     */
    var volumeBoost: Int
        get() = _state.value.volumeBoost
        set(value) {
            val clamped = value.coerceIn(100, 200)
            settings.putInt(KEY_VOLUME_BOOST, clamped)
            _state.update { it.copy(volumeBoost = clamped) }
        }
    
    /**
     * Last.fm scrobbling enabled.
     */
    var isLastFmEnabled: Boolean
        get() = _state.value.isLastFmEnabled
        set(value) {
            settings.putBoolean(KEY_LASTFM_ENABLED, value)
            _state.update { it.copy(isLastFmEnabled = value) }
        }
    
    /**
     * ListenBrainz scrobbling enabled.
     */
    var isListenBrainzEnabled: Boolean
        get() = _state.value.isListenBrainzEnabled
        set(value) {
            settings.putBoolean(KEY_LISTENBRAINZ_ENABLED, value)
            _state.update { it.copy(isListenBrainzEnabled = value) }
        }
    
    /**
     * Discord Rich Presence enabled.
     */
    var isDiscordRpcEnabled: Boolean
        get() = _state.value.isDiscordRpcEnabled
        set(value) {
            settings.putBoolean(KEY_DISCORD_RPC_ENABLED, value)
            _state.update { it.copy(isDiscordRpcEnabled = value) }
        }
    
    /**
     * Clear all settings.
     */
    fun clear() {
        settings.clear()
        _state.value = SettingsState()
    }
}

/**
 * Global settings instance.
 */
object GlobalSettings {
    val settings = AppSettings()
}
