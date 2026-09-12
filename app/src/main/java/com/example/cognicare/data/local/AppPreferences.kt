package com.example.cognicare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cognicare_preferences")

/**
 * Session, onboarding and patient-selection preferences.
 * The access token moves to encrypted storage in the security hardening step.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val data: Flow<Preferences> = context.dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val session: Flow<AuthSession?> = data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null
        val role = prefs[Keys.ROLE]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
            ?: return@map null
        AuthSession(
            userId = userId,
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            role = role,
            accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty(),
            linkedPatientIds = prefs[Keys.LINKED_PATIENT_IDS].orEmpty()
                .split(',')
                .filter { it.isNotBlank() }
        )
    }.distinctUntilChanged()

    val languageTag: Flow<String?> = data.map { it[Keys.LANGUAGE_TAG] }.distinctUntilChanged()

    val consentAccepted: Flow<Boolean> = data.map { it[Keys.CONSENT_ACCEPTED] ?: false }.distinctUntilChanged()

    val selectedPatientId: Flow<String?> = data.map { it[Keys.SELECTED_PATIENT_ID] }.distinctUntilChanged()

    suspend fun saveSession(session: AuthSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.DISPLAY_NAME] = session.displayName
            prefs[Keys.ROLE] = session.role.name
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.LINKED_PATIENT_IDS] = session.linkedPatientIds.joinToString(",")
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.ROLE)
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.LINKED_PATIENT_IDS)
            prefs.remove(Keys.SELECTED_PATIENT_ID)
        }
    }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }

    suspend fun setConsentAccepted(accepted: Boolean) {
        context.dataStore.edit { it[Keys.CONSENT_ACCEPTED] = accepted }
    }

    suspend fun setSelectedPatientId(patientId: String) {
        context.dataStore.edit { it[Keys.SELECTED_PATIENT_ID] = patientId }
    }

    private object Keys {
        val USER_ID = stringPreferencesKey("session_user_id")
        val DISPLAY_NAME = stringPreferencesKey("session_display_name")
        val ROLE = stringPreferencesKey("session_role")
        val ACCESS_TOKEN = stringPreferencesKey("session_access_token")
        val LINKED_PATIENT_IDS = stringPreferencesKey("session_linked_patient_ids")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val CONSENT_ACCEPTED = booleanPreferencesKey("consent_accepted")
        val SELECTED_PATIENT_ID = stringPreferencesKey("selected_patient_id")
    }
}
