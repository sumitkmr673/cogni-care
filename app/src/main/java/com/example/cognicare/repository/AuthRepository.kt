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
     * Option B: Simple self-registration and device binding initiated directly from the Android app.
     * The patient enters their name; the app binds this installation via [PatientDevice] and stores
     * the backend credentials securely. No email or password is required from the patient.
     */
    suspend fun registerPatientDevice(
        name: String,
        dateOfBirth: String? = null,
        gender: String? = null,
        language: String? = null
    ): AuthResult

    /**
     * Activates and saves the patient session in local preferences after the patient
     * has reviewed their newly generated public ID on the success screen.
     */
    suspend fun completePatientSession(session: AuthSession)

    /**
     * First-time setup for a patient's device with existing account credentials (e.g. demo or caregiver-managed):
     * authenticates against the backend, binds or saves credentials, and enables passwordless name-based sign-in.
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

enum class AuthFailure {
    NAME_NOT_RECOGNIZED,
    LOCKED_OUT,
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    NOT_SET_UP,
    DEVICE_ALREADY_BOUND
}
