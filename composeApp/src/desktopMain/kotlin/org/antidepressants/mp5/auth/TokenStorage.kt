package org.antidepressants.mp5.auth

import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages persistent storage of authentication tokens.
 * Stores tokens in a local properties file.
 * NOTE: For a production app, use OS-level secure storage (Keychain/Credential Locker).
 */
class TokenStorage {
    private val storageDir = File(System.getProperty("user.home"), ".mp5").also { it.mkdirs() }
    private val tokenFile = File(storageDir, "auth.properties")
    
    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Long) {
        withContext(Dispatchers.IO) {
            val props = Properties()
            props.setProperty("access_token", accessToken)
            refreshToken?.let { props.setProperty("refresh_token", it) }
            props.setProperty("expires_at", (System.currentTimeMillis() + expiresIn * 1000).toString())
            
            tokenFile.outputStream().use { 
                props.store(it, "MP5 Auth Tokens") 
            }
        }
    }
    
    suspend fun loadTokens(): AuthTokens? {
        return withContext(Dispatchers.IO) {
            if (!tokenFile.exists()) return@withContext null
            
            try {
                val props = Properties()
                tokenFile.inputStream().use { props.load(it) }
                
                val accessToken = props.getProperty("access_token") ?: return@withContext null
                val refreshToken = props.getProperty("refresh_token")
                val expiresAt = props.getProperty("expires_at")?.toLongOrNull() ?: 0L
                
                AuthTokens(accessToken, refreshToken, expiresAt)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            if (tokenFile.exists()) {
                tokenFile.delete()
            }
        }
    }
}

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long
)
