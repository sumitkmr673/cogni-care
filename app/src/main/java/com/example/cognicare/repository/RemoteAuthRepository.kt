package com.example.cognicare.repository

import com.example.cognicare.core.text.patientNameMatches
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.local.SecureCredentialStore
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.UserRole
import com.example.cognicare.data.remote.CogniCareApi
import com.example.cognicare.data.remote.dto.LoginRequestDto
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to cogni-care/backend/app/api/auth.py. The backend has only one login shape — email and
 * password, for every role — so the patient's "just say your name" flow is built on top of it:
 * [setupPatientDevice] does one real login and caches the password (encrypted); [signInPatient]
 * checks the spoken or typed name locally and reuses the cached password to re-establish the
 * backend session, so the patient never types an email or password on this device.
 *
 * The session is saved last in every flow: saving it is what switches the app to the signed-in
 * screens, which clears the sign-in ViewModel, so all other writes must already be done by then.
 */
@Singleton
class RemoteAuthRepository @Inject constructor(
    private val api: CogniCareApi,
    private val preferences: AppPreferences,
    private val credentialStore: SecureCredentialStore
) : AuthRepository {

    override val session: Flow<AuthSession?> = preferences.session

    override val hasPatientDeviceSetup: Flow<Boolean> = preferences.patientRegisteredName.map { it != null }

    override val patientUnlockStatus: Flow<PatientUnlockStatus> = combine(
        preferences.patientFailedNameAttempts,
        preferences.patientLockedOutAt
    ) { failedAttempts, lockedOutAt ->
        PatientUnlockStatus(failedAttempts, isLockedOut = lockedOutAt != null, lockedOutAt = lockedOutAt)
    }

    override suspend fun setupPatientDevice(email: String, password: String): AuthResult {
        val result = authenticate(email, password, expectedRole = UserRole.PATIENT)
        if (result is AuthResult.Success) {
            withContext(NonCancellable) {
                credentialStore.save(email.trim(), password)
                preferences.setPatientRegisteredName(result.session.displayName)
                preferences.resetPatientNameAttempts()
                preferences.saveSession(result.session)
            }
        }
        return result
    }

    override suspend fun signInPatient(spokenOrTypedName: String): AuthResult {
        if (preferences.patientLockedOutAt.first() != null) return AuthResult.Failure(AuthFailure.LOCKED_OUT)
        val registeredName = preferences.patientRegisteredName.first()
            ?: return AuthResult.Failure(AuthFailure.NOT_SET_UP)
        val credentials = credentialStore.read() ?: return AuthResult.Failure(AuthFailure.NOT_SET_UP)

        if (!patientNameMatches(spokenOrTypedName, registeredName)) {
            val attempts = withContext(NonCancellable) {
                preferences.recordFailedPatientNameAttempt(lockAt = MAX_PATIENT_NAME_ATTEMPTS)
            }
            val reason = if (attempts >= MAX_PATIENT_NAME_ATTEMPTS) AuthFailure.LOCKED_OUT else AuthFailure.NAME_NOT_RECOGNIZED
            return AuthResult.Failure(reason)
        }

        val result = authenticate(credentials.email, credentials.password, expectedRole = UserRole.PATIENT)
        if (result is AuthResult.Success) {
            withContext(NonCancellable) {
                preferences.resetPatientNameAttempts()
                preferences.saveSession(result.session)
            }
        }
        return result
    }

    override suspend fun signInCaregiver(email: String, password: String): AuthResult {
        val result = authenticate(email, password, expectedRole = UserRole.DOCTOR)
        if (result is AuthResult.Success) {
            withContext(NonCancellable) {
                credentialStore.save(email.trim(), password)
                preferences.saveSession(result.session)
            }
        }
        return result
    }

    override suspend fun signOut() {
        credentialStore.clear()
        preferences.clearSession()
        preferences.clearPatientDevice()
    }

    /**
     * Logs in and confirms the account's real role, name and id via /auth/me, passing the new token
     * explicitly. Nothing is saved here — see the class comment for why callers save afterwards.
     */
    private suspend fun authenticate(email: String, password: String, expectedRole: UserRole): AuthResult =
        try {
            val token = api.login(LoginRequestDto(email.trim(), password)).access_token
            val me = api.currentUser("Bearer $token")
            val role = backendRoleToAppRole(me.role)
            if (role != expectedRole) {
                AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
            } else {
                AuthResult.Success(
                    AuthSession(userId = me.id, displayName = me.display_name, role = role, accessToken = token)
                )
            }
        } catch (error: HttpException) {
            if (error.code() == 401 || error.code() == 422) {
                AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
            } else {
                AuthResult.Failure(AuthFailure.NETWORK_ERROR)
            }
        } catch (error: IOException) {
            AuthResult.Failure(AuthFailure.NETWORK_ERROR)
        }

    /** The backend has CAREGIVER and DOCTOR as separate roles; the app treats both as the one "caregiver" side. */
    private fun backendRoleToAppRole(backendRole: String): UserRole =
        if (backendRole == "PATIENT") UserRole.PATIENT else UserRole.DOCTOR
}
