package org.antidepressants.mp5.auth

import org.antidepressants.mp5.domain.model.User

class AndroidGoogleAuthProvider : GoogleAuthProvider {
    override suspend fun signIn(): Result<User> {
        // TODO: Implement Android Google Sign-In
        return Result.failure(Exception("Not implemented on Android yet"))
    }
    
    override suspend fun signOut() {
        // TODO
    }
    
    override suspend fun restoreSession(): User? {
        return null
    }
}

actual fun getGoogleAuthProvider(): GoogleAuthProvider = AndroidGoogleAuthProvider()
