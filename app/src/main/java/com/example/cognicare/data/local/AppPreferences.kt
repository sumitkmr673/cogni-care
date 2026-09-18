package com.example.cognicare.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cognicare.core.game.MIN_GAME_LEVEL
import com.example.cognicare.core.game.clampLevel
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.GameType
import com.example.cognicare.data.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
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

    /** Name on the patient account set up on this device — what the patient says or types to sign in. */
    val patientRegisteredName: Flow<String?> = data.map { it[Keys.PATIENT_REGISTERED_NAME] }.distinctUntilChanged()

    val patientFailedNameAttempts: Flow<Int> =
        data.map { it[Keys.PATIENT_FAILED_NAME_ATTEMPTS] ?: 0 }.distinctUntilChanged()

    /** Set once too many wrong names were given; null while the device is not locked. */
    val patientLockedOutAt: Flow<Long?> = data.map { it[Keys.PATIENT_LOCKED_OUT_AT] }.distinctUntilChanged()

    val apiBaseUrlOverride: Flow<String?> = data.map { it[Keys.API_BASE_URL_OVERRIDE] }.distinctUntilChanged()

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

    suspend fun setPatientRegisteredName(name: String) {
        context.dataStore.edit { it[Keys.PATIENT_REGISTERED_NAME] = name }
    }

    /** Adds one wrong-name attempt and returns the new total, locking the device once it reaches [lockAt]. */
    suspend fun recordFailedPatientNameAttempt(lockAt: Int): Int {
        var total = 0
        context.dataStore.edit { prefs ->
            total = (prefs[Keys.PATIENT_FAILED_NAME_ATTEMPTS] ?: 0) + 1
            prefs[Keys.PATIENT_FAILED_NAME_ATTEMPTS] = total
            if (total >= lockAt && prefs[Keys.PATIENT_LOCKED_OUT_AT] == null) {
                prefs[Keys.PATIENT_LOCKED_OUT_AT] = System.currentTimeMillis()
            }
        }
        return total
    }

    suspend fun resetPatientNameAttempts() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PATIENT_FAILED_NAME_ATTEMPTS)
            prefs.remove(Keys.PATIENT_LOCKED_OUT_AT)
        }
    }

    suspend fun clearPatientDevice() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PATIENT_REGISTERED_NAME)
            prefs.remove(Keys.PATIENT_FAILED_NAME_ATTEMPTS)
            prefs.remove(Keys.PATIENT_LOCKED_OUT_AT)
        }
    }

    suspend fun setApiBaseUrlOverride(url: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) it.remove(Keys.API_BASE_URL_OVERRIDE) else it[Keys.API_BASE_URL_OVERRIDE] = url
        }
    }

    /**
     * The level a patient has reached in [gameType], defaulting to [MIN_GAME_LEVEL] on a new
     * device. Kept on the device rather than fetched: a patient must be able to start playing at
     * the right difficulty while offline, and the backend has no per-game progress endpoint.
     */
    fun gameLevel(gameType: GameType): Flow<Int> =
        data.map { clampLevel(it[levelKey(gameType)] ?: MIN_GAME_LEVEL) }.distinctUntilChanged()

    suspend fun setGameLevel(gameType: GameType, level: Int) {
        context.dataStore.edit { it[levelKey(gameType)] = clampLevel(level) }
    }

    /** Reads the stored level once, for a ViewModel setting up a board before collecting. */
    suspend fun currentGameLevel(gameType: GameType): Int = gameLevel(gameType).firstOrNull() ?: MIN_GAME_LEVEL

    // One key per game, so adding a game needs no change here.
    private fun levelKey(gameType: GameType) = intPreferencesKey("game_level_${gameType.name}")

    /** Reads the access token once, for use outside a Flow collector (e.g. an OkHttp interceptor). */
    suspend fun currentAccessToken(): String? = session.firstOrNull()?.accessToken

    private object Keys {
        val USER_ID = stringPreferencesKey("session_user_id")
        val DISPLAY_NAME = stringPreferencesKey("session_display_name")
        val ROLE = stringPreferencesKey("session_role")
        val ACCESS_TOKEN = stringPreferencesKey("session_access_token")
        val LINKED_PATIENT_IDS = stringPreferencesKey("session_linked_patient_ids")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val CONSENT_ACCEPTED = booleanPreferencesKey("consent_accepted")
        val SELECTED_PATIENT_ID = stringPreferencesKey("selected_patient_id")
        val PATIENT_REGISTERED_NAME = stringPreferencesKey("patient_registered_name")
        val PATIENT_FAILED_NAME_ATTEMPTS = intPreferencesKey("patient_failed_name_attempts")
        val PATIENT_LOCKED_OUT_AT = longPreferencesKey("patient_locked_out_at")
        val API_BASE_URL_OVERRIDE = stringPreferencesKey("api_base_url_override")
    }
}
