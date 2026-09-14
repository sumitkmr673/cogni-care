package com.example.cognicare.repository

import com.example.cognicare.data.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<AuthSession?>

    suspend fun signInPatient(pin: String): AuthResult

    suspend fun signInCaregiver(email: String, password: String): AuthResult

    suspend fun signOut()
}

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure { INVALID_PIN, INVALID_CREDENTIALS }
