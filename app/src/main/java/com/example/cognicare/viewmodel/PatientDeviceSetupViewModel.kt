package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step-by-step Simple Mode patient onboarding & device setup ViewModel.
 * Follows the Android Simple Mode philosophy for elderly users:
 * Welcome → Name → Date of Birth → Gender → Preferred Language → Success (Patient ID).
 */
@HiltViewModel
class PatientDeviceSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDeviceSetupUiState())
    val uiState: StateFlow<PatientDeviceSetupUiState> = _uiState.asStateFlow()

    fun onStartSetup() = _uiState.update { it.copy(step = PatientSetupStep.NAME, error = null) }

    fun onNameChange(value: String) = _uiState.update { it.copy(patientName = value, error = null) }

    fun onNameContinue() {
        if (_uiState.value.patientName.trim().isNotBlank()) {
            _uiState.update { it.copy(step = PatientSetupStep.BIRTH_DATE, error = null) }
        }
    }

    fun onBirthDayChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(birthDay = filtered, dobError = false, error = null) }
    }

    fun onBirthMonthChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(birthMonth = filtered, dobError = false, error = null) }
    }

    fun onBirthYearChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(birthYear = filtered, dobError = false, error = null) }
    }

    fun onDateChanged(day: Int, month: Int, year: Int) {
        val maxDay = maxDaysInMonth(month, year)
        val clampedDay = day.coerceIn(1, maxDay)
        _uiState.update {
            it.copy(
                birthDay = "%02d".format(clampedDay),
                birthMonth = "%02d".format(month),
                birthYear = "%04d".format(year),
                dobError = false,
                error = null
            )
        }
    }

    fun onDobContinue() {
        val state = _uiState.value
        if (state.isDobValid) {
            _uiState.update { it.copy(step = PatientSetupStep.GENDER, dobError = false, error = null) }
        } else {
            _uiState.update { it.copy(dobError = true) }
        }
    }

    fun onGenderSelect(value: String) = _uiState.update {
        it.copy(gender = value, error = null)
    }

    fun onGenderContinue() {
        if (_uiState.value.gender != null) {
            _uiState.update { it.copy(step = PatientSetupStep.LANGUAGE, error = null) }
        }
    }

    fun onLanguageSelect(language: AppLanguage) {
        if (language.isTranslated) {
            _uiState.update { it.copy(selectedLanguage = language, error = null) }
            viewModelScope.launch {
                preferences.setLanguageTag(language.tag)
            }
        }
    }

    fun onLanguageContinue() {
        if (_uiState.value.selectedLanguage.isTranslated) {
            _uiState.update { it.copy(step = PatientSetupStep.CONSENT, error = null) }
        }
    }

    fun onConsentToggle(agreed: Boolean) = _uiState.update {
        it.copy(consentAccepted = agreed, error = null)
    }

    fun onConsentContinue() {
        if (_uiState.value.consentAccepted) {
            onSubmitRegistration()
        }
    }

    fun onSubmitRegistration() {
        val state = _uiState.value
        if (state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            // Apply chosen language and consent to app preferences
            preferences.setLanguageTag(state.selectedLanguage.tag)
            preferences.setConsentAccepted(true)

            val result = authRepository.registerPatientDevice(
                name = state.patientName.trim(),
                dateOfBirth = state.formattedDobIso,
                gender = state.gender,
                language = state.selectedLanguage.englishName
            )
            when (result) {
                is AuthResult.Success -> {
                    preferences.setPatientDob(state.formattedDobIso)
                    preferences.setPatientGender(state.gender)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            step = PatientSetupStep.SUCCESS,
                            patientPublicId = result.session.publicId,
                            pendingSession = result.session,
                            error = null
                        )
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(isSubmitting = false, error = result.reason)
                    }
                }
            }
        }
    }

    // --- Login with an existing Patient ID (returning patient, new device) ---
    fun onDismissLoginCoachmark() = _uiState.update { it.copy(showLoginCoachmark = false) }

    fun onShowLoginWithIdDialog() = _uiState.update {
        it.copy(isLoginWithIdDialogVisible = true, showLoginCoachmark = false, loginWithIdError = null)
    }

    fun onDismissLoginWithIdDialog() = _uiState.update {
        it.copy(isLoginWithIdDialogVisible = false, loginPublicId = "", loginWithIdError = null)
    }

    fun onLoginPublicIdChange(value: String) = _uiState.update {
        it.copy(loginPublicId = value.uppercase(), loginWithIdError = null)
    }

    fun onSubmitLoginWithId() {
        val state = _uiState.value
        if (!state.canSubmitLoginWithId) return

        _uiState.update { it.copy(isLoggingInWithId = true, loginWithIdError = null) }
        viewModelScope.launch {
            val result = authRepository.loginPatientWithId(state.loginPublicId.trim())
            when (result) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoggingInWithId = false, isLoginWithIdDialogVisible = false, loginPublicId = "")
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoggingInWithId = false, loginWithIdError = result.reason)
                }
            }
        }
    }

    fun onCompleteSetup() {
        val session = _uiState.value.pendingSession ?: return
        viewModelScope.launch {
            authRepository.completePatientSession(session)
        }
    }

    fun onPrevStep(): Boolean {
        val current = _uiState.value.step
        return when (current) {
            PatientSetupStep.WELCOME -> false
            PatientSetupStep.NAME -> {
                _uiState.update { it.copy(step = PatientSetupStep.WELCOME, error = null) }
                true
            }
            PatientSetupStep.BIRTH_DATE -> {
                _uiState.update { it.copy(step = PatientSetupStep.NAME, error = null, dobError = false) }
                true
            }
            PatientSetupStep.GENDER -> {
                _uiState.update { it.copy(step = PatientSetupStep.BIRTH_DATE, error = null) }
                true
            }
            PatientSetupStep.LANGUAGE -> {
                _uiState.update { it.copy(step = PatientSetupStep.GENDER, error = null) }
                true
            }
            PatientSetupStep.CONSENT -> {
                _uiState.update { it.copy(step = PatientSetupStep.LANGUAGE, error = null) }
                true
            }
            PatientSetupStep.SUCCESS -> false
        }
    }

    // --- Caregiver & Demo Sign-In mode ---
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun toggleSetupMode() = _uiState.update {
        it.copy(isCaregiverSetupMode = !it.isCaregiverSetupMode, error = null)
    }

    fun useDemoCredentials() = _uiState.update {
        it.copy(
            isCaregiverSetupMode = true,
            email = DemoData.PATIENT_EMAIL,
            password = DemoData.PATIENT_PASSWORD,
            error = null
        )
    }

    fun submitCaregiver() {
        val state = _uiState.value
        if (!state.canSubmitCaregiver) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.setupPatientDevice(state.email.trim(), state.password)
            _uiState.update {
                it.copy(isSubmitting = false, error = (result as? AuthResult.Failure)?.reason)
            }
        }
    }
}
