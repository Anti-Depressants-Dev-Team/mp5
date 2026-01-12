package org.antidepressants.mp5.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents an authenticated user.
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenExpiresAt: Long
)

/**
 * Auth state for tracking login status.
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
