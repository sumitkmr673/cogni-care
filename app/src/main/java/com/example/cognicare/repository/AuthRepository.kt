package com.example.cognicare.repository

import com.example.cognicare.data.model.AuthSession
import kotlinx.coroutines.flow.Flow

/** Wrong names allowed before the patient's device locks and asks for a caregiver or doctor. */
const val MAX_PATIENT_NAME_ATTEMPTS = 10

data class PatientUnlockStatus(
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    /** When the device locked. Kept so the caregiver side can surface it once there is an alerts channel. */
    val lockedOutAt: Long? = null
)

interface AuthRepository {
    val session: Flow<AuthSession?>

    /** Whether this device has completed patient device setup (see [setupPatientDevice]). */
    val hasPatientDeviceSetup: Flow<Boolean>

    val patientUnlockStatus: Flow<PatientUnlockStatus>

    /**
     * First-time setup for a patient's device, usually done by a caregiver: a real login against
     * the backend with the patient's own email and password. The password is never asked for again
     * on this device; from then on the patient signs in by saying or typing their name
     * ([signInPatient]). A successful setup also clears any earlier lockout.
     */
    suspend fun setupPatientDevice(email: String, password: String): AuthResult

    /**
     * Everyday patient sign-in: checks [spokenOrTypedName] against the name on the account set up
     * on this device, then silently re-establishes the backend session. Each wrong name counts
     * toward [MAX_PATIENT_NAME_ATTEMPTS]; after that the device stays locked until it is set up again.
     */
    suspend fun signInPatient(spokenOrTypedName: String): AuthResult

    suspend fun signInCaregiver(email: String, password: String): AuthResult

    suspend fun signOut()
}

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure { NAME_NOT_RECOGNIZED, LOCKED_OUT, INVALID_CREDENTIALS, NETWORK_ERROR, NOT_SET_UP }
