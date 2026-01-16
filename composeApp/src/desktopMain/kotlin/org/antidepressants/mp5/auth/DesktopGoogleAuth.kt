package org.antidepressants.mp5.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI

class DesktopGoogleAuth {
    
    companion object {
        // PLACEHOLDERS - User needs to populate these
        private const val CLIENT_ID = "968911856530-d2u7lp3npgcufjmp60mo8hknaluvdacn.apps.googleusercontent.com"
        private const val CLIENT_SECRET = "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"
        private const val REDIRECT_URI = "http://localhost:54321/callback"
        private const val PORT = 54321
        private const val SCOPE = "email profile https://www.googleapis.com/auth/youtube.readonly"
        private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    }
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    suspend fun startAuthFlow(): Result<AuthTokens> {
        return withContext(Dispatchers.IO) {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket(PORT)
                // URL-encode spaces in scope
                val encodedScope = SCOPE.replace(" ", "%20")
                val authUri = "$AUTH_URL?client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&response_type=code&scope=$encodedScope&access_type=offline&prompt=consent"
                
                // Open browser
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(authUri))
                } else {
                    return@withContext Result.failure(Exception("Desktop browsing not supported"))
                }
                
                println("Waiting for redirect on port $PORT...")
                
                // Wait for callback
                val socket = serverSocket.accept()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine()
                
                // Parse code from GET request
                // GET /callback?code=... HTTP/1.1
                val code = if (line != null && line.contains("code=")) {
                    line.substringAfter("code=").substringBefore(" ")
                } else {
                    null
                }
                
                // Send response to browser
                val writer = PrintWriter(socket.getOutputStream())
                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: text/html")
                writer.println("\r\n")
                writer.println("<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px;'><h1>Login Successful!</h1><p>You can close this window and return to the app.</p><script>window.close()</script></body></html>")
                writer.flush()
                
                socket.close()
                
                if (code != null) {
                    exchangeCodeForToken(code)
                } else {
                    Result.failure(Exception("No authorization code received from callback"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                serverSocket?.close()
            }
        }
    }
    
    private suspend fun exchangeCodeForToken(code: String): Result<AuthTokens> {
        return try {
            val response = httpClient.submitForm(
                url = TOKEN_URL,
                formParameters = Parameters.build {
                    append("client_id", CLIENT_ID)
                    append("client_secret", CLIENT_SECRET)
                    append("code", code)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", REDIRECT_URI)
                }
            )
            
            if (response.status.value in 200..299) {
                val tokenResponse: TokenResponse = response.body()
                Result.success(AuthTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAt = tokenResponse.expiresIn
                ))
            } else {
                val errorBody = response.body<String>()
                Result.failure(Exception("Token exchange failed: ${response.status} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Token exchange network error: ${e.message}"))
        }
    }
    
    // For refreshing tokens later
    suspend fun refreshToken(refreshToken: String): AuthTokens? {
        return try {
            val response: TokenResponse = httpClient.submitForm(
                url = TOKEN_URL,
                formParameters = Parameters.build {
                    append("client_id", CLIENT_ID)
                    append("client_secret", CLIENT_SECRET)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                }
            ).body()
            
            AuthTokens(
                accessToken = response.accessToken,
                refreshToken = null, // specific to Google, might not return new refresh token
                expiresAt = response.expiresIn
            )
        } catch (e: Exception) {
            println("Token refresh failed: ${e.message}")
            null
        }
    }
    suspend fun fetchUserInfo(accessToken: String): Result<GoogleUserInfo> {
        return try {
            val response = httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                header("Authorization", "Bearer $accessToken")
            }
            
            if (response.status.value in 200..299) {
                Result.success(response.body<GoogleUserInfo>())
            } else {
                Result.failure(Exception("Failed to fetch user info: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("token_type") val tokenType: String? = null
)

@Serializable
data class GoogleUserInfo(
    val id: String,
    val email: String,
    @SerialName("name") val name: String,
    @SerialName("picture") val picture: String? = null
)
