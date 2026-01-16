package org.antidepressants.mp5.auth

import org.antidepressants.mp5.domain.model.User

interface GoogleAuthProvider {
    suspend fun signIn(): Result<User>
    suspend fun signOut()
    suspend fun restoreSession(): User?
}

expect fun getGoogleAuthProvider(): GoogleAuthProvider
