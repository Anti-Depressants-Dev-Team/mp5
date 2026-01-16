package org.antidepressants.mp5.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.antidepressants.mp5.domain.model.AuthState
import org.antidepressants.mp5.domain.model.User

/**
 * Manages authentication state and Google OAuth login.
 * 
 * For desktop: Opens browser for OAuth flow
 * For Android: Uses Google Sign-In SDK
 */
class AuthManager {
    
    private val provider = getGoogleAuthProvider()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        // Try to restore session on startup
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            val user = provider.restoreSession()
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
            }
        }
    }
    
    /**
     * Initiate Google OAuth login flow.
     * Platform-specific implementations handle the actual OAuth.
     */
    suspend fun signInWithGoogle(): Result<User> {
        _authState.value = AuthState.Loading
        
        return try {
            val result = provider.signIn()
            result.onSuccess { user ->
                _authState.value = AuthState.Authenticated(user)
            }.onFailure { error ->
                val errorMessage = "Login failed: ${error.message}"
                _authState.value = AuthState.Error(errorMessage)
            }
            result
        } catch (e: Exception) {
            val errorMessage = "Login failed: ${e.message}"
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(e)
        }
    }
    
    /**
     * Sign out the current user.
     */
    fun signOut() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            provider.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Get the currently authenticated user, if any.
     */
    fun getCurrentUser(): User? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.user
            else -> null
        }
    }
    
    /**
     * Sync playlists from the authenticated Google account.
     * Returns success with count of synced playlists, or failure.
     */
    suspend fun syncPlaylists(): Result<Int> {
        val user = getCurrentUser() ?: return Result.failure(Exception("Not authenticated"))
        val repo = org.antidepressants.mp5.data.repository.GlobalPlaylistRepository.instance
        val syncManager = org.antidepressants.mp5.data.sync.YouTubeSyncManager(repo)
        return syncManager.syncPlaylists(user.accessToken)
    }
}

// Global singleton
object GlobalAuthManager {
    val instance = AuthManager()
}
