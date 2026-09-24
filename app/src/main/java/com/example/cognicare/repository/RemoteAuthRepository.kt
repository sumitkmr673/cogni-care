package com.example.cognicare.repository

import com.example.cognicare.core.text.patientNameMatches
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.local.SecureCredentialStore
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.UserRole
import com.example.cognicare.data.remote.CogniCareApi
import com.example.cognicare.data.remote.dto.DeviceLoginRequestDto
import com.example.cognicare.data.remote.dto.LoginRequestDto
import com.example.cognicare.data.remote.dto.PatientLoginByIdRequestDto
import com.example.cognicare.data.remote.dto.PatientRegisterRequestDto
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

    override suspend fun registerPatientDevice(
        name: String,
        dateOfBirth: String?,
        gender: String?,
        language: String?
    ): AuthResult {
        return try {
            val clientDeviceId = credentialStore.getOrCreateClientDeviceId()
            val response = api.registerPatient(
                PatientRegisterRequestDto(
                    display_name = name.trim(),
                    date_of_birth = dateOfBirth,
                    gender = gender,
                    preferred_language = language,
                    client_device_id = clientDeviceId
                )
            )
            val session = AuthSession(
                userId = response.patient_id,
                displayName = response.display_name,
                role = UserRole.PATIENT,
                accessToken = response.token.access_token,
                publicId = response.patient_public_id,
                patientId = response.patient_id
            )
            withContext(NonCancellable) {
                credentialStore.saveDeviceCredentials(
                    deviceIdentifier = response.device_identifier,
                    deviceKey = response.device_key,
                    patientPublicId = response.patient_public_id
                )
                preferences.setPatientRegisteredName(response.display_name)
                preferences.resetPatientNameAttempts()
            }
            AuthResult.Success(session)
        } catch (error: HttpException) {
            if (error.code() == 409) {
                AuthResult.Failure(AuthFailure.DEVICE_ALREADY_BOUND)
            } else if (error.code() == 401 || error.code() == 422) {
                AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
            } else {
                AuthResult.Failure(AuthFailure.NETWORK_ERROR)
            }
        } catch (error: IOException) {
            AuthResult.Failure(AuthFailure.NETWORK_ERROR)
        }
    }

    override suspend fun completePatientSession(session: AuthSession) {
        withContext(NonCancellable) {
            preferences.saveSession(session)
        }
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

        if (!patientNameMatches(spokenOrTypedName, registeredName)) {
            val attempts = withContext(NonCancellable) {
                preferences.recordFailedPatientNameAttempt(lockAt = MAX_PATIENT_NAME_ATTEMPTS)
            }
            val reason = if (attempts >= MAX_PATIENT_NAME_ATTEMPTS) AuthFailure.LOCKED_OUT else AuthFailure.NAME_NOT_RECOGNIZED
            return AuthResult.Failure(reason)
        }

        // 1. Try passwordless device authentication if device credentials exist
        val deviceCreds = credentialStore.readDeviceCredentials()
        if (deviceCreds != null) {
            val result = authenticateDevice(deviceCreds.deviceIdentifier, deviceCreds.deviceKey, deviceCreds.patientPublicId)
            if (result is AuthResult.Success) {
                withContext(NonCancellable) {
                    preferences.resetPatientNameAttempts()
                    preferences.saveSession(result.session)
                }
            }
            return result
        }

        // 2. Fallback to cached email/password credentials for legacy setups
        val credentials = credentialStore.read() ?: return AuthResult.Failure(AuthFailure.NOT_SET_UP)
        val result = authenticate(credentials.email, credentials.password, expectedRole = UserRole.PATIENT)
        if (result is AuthResult.Success) {
            withContext(NonCancellable) {
                preferences.resetPatientNameAttempts()
                preferences.saveSession(result.session)
            }
        }
        return result
    }

    override suspend fun loginPatientWithId(publicId: String): AuthResult {
        return try {
            val clientDeviceId = credentialStore.getOrCreateClientDeviceId()
            val response = api.loginPatientWithId(
                PatientLoginByIdRequestDto(
                    public_id = publicId.trim().uppercase(),
                    client_device_id = clientDeviceId
                )
            )
            val session = AuthSession(
                userId = response.patient_id,
                displayName = response.display_name,
                role = UserRole.PATIENT,
                accessToken = response.token.access_token,
                publicId = response.patient_public_id,
                patientId = response.patient_id
            )
            withContext(NonCancellable) {
                credentialStore.saveDeviceCredentials(
                    deviceIdentifier = response.device_identifier,
                    deviceKey = response.device_key,
                    patientPublicId = response.patient_public_id
                )
                preferences.setPatientRegisteredName(response.display_name)
                preferences.resetPatientNameAttempts()
                preferences.saveSession(session)
            }
            AuthResult.Success(session)
        } catch (error: HttpException) {
            when (error.code()) {
                404 -> AuthResult.Failure(AuthFailure.PATIENT_ID_NOT_FOUND)
                409 -> AuthResult.Failure(AuthFailure.DEVICE_ALREADY_BOUND)
                401, 422 -> AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
                else -> AuthResult.Failure(AuthFailure.NETWORK_ERROR)
            }
        } catch (error: IOException) {
            AuthResult.Failure(AuthFailure.NETWORK_ERROR)
        }
    }

    override suspend fun signInCaregiver(email: String, password: String): AuthResult {
        val result = authenticate(email, password, expectedRole = UserRole.CAREGIVER)
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

    private suspend fun authenticateDevice(
        deviceIdentifier: String,
        deviceKey: String,
        patientPublicId: String?
    ): AuthResult =
        try {
            val token = api.deviceLogin(DeviceLoginRequestDto(deviceIdentifier, deviceKey)).access_token
            val me = api.currentUser("Bearer $token")
            val role = backendRoleToAppRole(me.role)
            if (role != UserRole.PATIENT) {
                AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
            } else {
                AuthResult.Success(
                    AuthSession(
                        userId = me.id,
                        displayName = me.display_name,
                        role = role,
                        accessToken = token,
                        publicId = me.public_id ?: patientPublicId,
                        patientId = me.patient_id
                    )
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

    /**
     * Logs in and confirms the account's real role, name and id via /auth/me, passing the new token
     * explicitly.
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
                    AuthSession(
                        userId = me.id,
                        displayName = me.display_name,
                        role = role,
                        accessToken = token,
                        publicId = me.public_id,
                        patientId = me.patient_id
                    )
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

    private fun backendRoleToAppRole(backendRole: String): UserRole =
        if (backendRole == "PATIENT") UserRole.PATIENT else UserRole.CAREGIVER
}
