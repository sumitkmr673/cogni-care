package com.example.cognicare.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import java.util.UUID

data class StoredCredentials(val email: String, val password: String)

data class StoredDeviceCredentials(
    val deviceIdentifier: String,
    val deviceKey: String,
    val patientPublicId: String? = null
)

/**
 * Keystore-backed storage for authentication material:
 * 1. Caregiver email/password for re-authentication.
 * 2. Patient device credentials (device_identifier, device_key) for elder-friendly, passwordless login.
 * 3. Client device identity (installation-specific UUID).
 */
@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cognicare_secure_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(email: String, password: String) {
        prefs.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply()
    }

    fun read(): StoredCredentials? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        return StoredCredentials(email, password)
    }

    fun saveDeviceCredentials(deviceIdentifier: String, deviceKey: String, patientPublicId: String? = null) {
        prefs.edit()
            .putString(KEY_DEVICE_IDENTIFIER, deviceIdentifier)
            .putString(KEY_DEVICE_KEY, deviceKey)
            .apply {
                if (patientPublicId != null) {
                    putString(KEY_PATIENT_PUBLIC_ID, patientPublicId)
                }
            }
            .apply()
    }

    fun readDeviceCredentials(): StoredDeviceCredentials? {
        val deviceIdentifier = prefs.getString(KEY_DEVICE_IDENTIFIER, null) ?: return null
        val deviceKey = prefs.getString(KEY_DEVICE_KEY, null) ?: return null
        val patientPublicId = prefs.getString(KEY_PATIENT_PUBLIC_ID, null)
        return StoredDeviceCredentials(deviceIdentifier, deviceKey, patientPublicId)
    }

    /**
     * Stable, installation-specific unique device ID (not a hardware serial/IMEI/MAC).
     * Preserved across normal restarts; generated once upon first access.
     */
    fun getOrCreateClientDeviceId(): String {
        val existing = prefs.getString(KEY_CLIENT_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CLIENT_DEVICE_ID, generated).apply()
        return generated
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_PASSWORD)
            .remove(KEY_DEVICE_IDENTIFIER)
            .remove(KEY_DEVICE_KEY)
            .remove(KEY_PATIENT_PUBLIC_ID)
            .apply()
    }

    private companion object {
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
        const val KEY_DEVICE_IDENTIFIER = "device_identifier"
        const val KEY_DEVICE_KEY = "device_key"
        const val KEY_PATIENT_PUBLIC_ID = "patient_public_id"
        const val KEY_CLIENT_DEVICE_ID = "client_device_id"
    }
}
