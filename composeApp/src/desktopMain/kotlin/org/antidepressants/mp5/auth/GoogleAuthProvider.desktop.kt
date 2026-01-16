package org.antidepressants.mp5.auth

import org.antidepressants.mp5.domain.model.User

class DesktopGoogleAuthProvider : GoogleAuthProvider {
    private val auth = DesktopGoogleAuth()
    private val storage = TokenStorage()
    
    override suspend fun signIn(): Result<User> {
        val result = auth.startAuthFlow()
        
        return result.mapCatching { tokens ->
            storage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresAt)
            
            // Fetch real user info
            val userInfoResult = auth.fetchUserInfo(tokens.accessToken)
            val userInfo = userInfoResult.getOrNull()
            
            User(
                id = userInfo?.id ?: "google-user",
                email = userInfo?.email ?: "user@gmail.com",
                displayName = userInfo?.name ?: "Google User",
                photoUrl = userInfo?.picture,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken ?: "",
                tokenExpiresAt = System.currentTimeMillis() + tokens.expiresAt * 1000
            )
        }
    }
    
    override suspend fun signOut() {
        storage.clearTokens()
    }
    
    override suspend fun restoreSession(): User? {
        val tokens = storage.loadTokens() ?: return null
        
        // Check if expired, try refresh if needed (simplified)
        if (System.currentTimeMillis() > (tokens.expiresAt)) {
            if (tokens.refreshToken != null) {
                val newTokens = auth.refreshToken(tokens.refreshToken)
                if (newTokens != null) {
                    storage.saveTokens(newTokens.accessToken, newTokens.refreshToken ?: tokens.refreshToken, newTokens.expiresAt)
                    return User("google-user", "user@gmail.com", "Google User", null, newTokens.accessToken, newTokens.refreshToken ?: tokens.refreshToken, System.currentTimeMillis() + newTokens.expiresAt * 1000)
                }
            }
            return null
        }
        
        return User("google-user", "user@gmail.com", "Google User", null, tokens.accessToken, tokens.refreshToken ?: "", tokens.expiresAt) // Should be converted correctly
    }
}

actual fun getGoogleAuthProvider(): GoogleAuthProvider = DesktopGoogleAuthProvider()
