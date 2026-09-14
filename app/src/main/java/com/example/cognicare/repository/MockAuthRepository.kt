package com.example.cognicare.repository

import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Stands in for the NestJS JWT endpoints until the backend is wired in. */
@Singleton
class MockAuthRepository @Inject constructor(
    private val preferences: AppPreferences
) : AuthRepository {

    override val session: Flow<AuthSession?> = preferences.session

    override suspend fun signInPatient(pin: String): AuthResult {
        delay(SIMULATED_LATENCY_MS)
        if (pin != DemoData.PATIENT_PIN) return AuthResult.Failure(AuthFailure.INVALID_PIN)

        return persist(
            AuthSession(
                userId = DemoData.PATIENT_ID,
                displayName = "Meera Sharma",
                role = UserRole.PATIENT,
                accessToken = mockToken(),
                linkedPatientIds = listOf(DemoData.PATIENT_ID)
            )
        )
    }

    override suspend fun signInCaregiver(email: String, password: String): AuthResult {
        delay(SIMULATED_LATENCY_MS)
        if (password != DemoData.CAREGIVER_PASSWORD) return AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)

        return persist(
            AuthSession(
                userId = DemoData.CAREGIVER_ID,
                displayName = "Ananya Mehta",
                role = UserRole.DOCTOR,
                accessToken = mockToken(),
                linkedPatientIds = DemoData.patients.map { it.id }
            )
        )
    }

    override suspend fun signOut() {
        preferences.clearSession()
    }

    private suspend fun persist(session: AuthSession): AuthResult {
        preferences.saveSession(session)
        return AuthResult.Success(session)
    }

    private fun mockToken(): String = "mock-${UUID.randomUUID()}"

    private companion object {
        const val SIMULATED_LATENCY_MS = 400L
    }
}
