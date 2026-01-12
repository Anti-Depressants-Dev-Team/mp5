package org.antidepressants.mp5.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.antidepressants.mp5.domain.model.AuthState
import org.antidepressants.mp5.domain.model.User

/**
 * Manages authentication state and Google OAuth login.
 * 
 * For desktop: Opens browser for OAuth flow
 * For Android: Uses Google Sign-In SDK
 */
class AuthManager {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Initiate Google OAuth login flow.
     * Platform-specific implementations handle the actual OAuth.
     */
    suspend fun signInWithGoogle(): Result<User> {
        _authState.value = AuthState.Loading
        
        return try {
            // TODO: Platform-specific OAuth implementation
            // Desktop: Open browser with OAuth URL, listen for callback
            // Android: Use Google Sign-In SDK
            
            // For now, return a mock user for testing
            val mockUser = User(
                id = "mock-user-id",
                email = "user@example.com",
                displayName = "Test User",
                photoUrl = null,
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                tokenExpiresAt = System.currentTimeMillis() + (3600 * 1000)
            )
            
            _authState.value = AuthState.Authenticated(mockUser)
            Result.success(mockUser)
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
        _authState.value = AuthState.Unauthenticated
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
}

// Global singleton
object GlobalAuthManager {
    val instance = AuthManager()
}
