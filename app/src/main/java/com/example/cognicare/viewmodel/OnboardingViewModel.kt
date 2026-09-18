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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val startingDemo = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> = combine(
        preferences.languageTag,
        preferences.consentAccepted,
        startingDemo,
        authRepository.hasPatientDeviceSetup
    ) { languageTag, consentAccepted, isStartingDemo, hasPatientDeviceSetup ->
        OnboardingUiState(AppLanguage.fromTag(languageTag), consentAccepted, isStartingDemo, hasPatientDeviceSetup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguageTag(language.tag) }
    }

    fun acceptConsent() {
        viewModelScope.launch { preferences.setConsentAccepted(true) }
    }

    // Mirrors the web kiosk's "Start with demo patient": skip straight to a signed-in demo
    // session with synthetic data, doing a real device setup against the seeded backend account
    // so the shortcut behaves exactly like a caregiver setting up the patient's device by hand.
    fun startDemoPatient() = startDemo {
        authRepository.setupPatientDevice(DemoData.PATIENT_EMAIL, DemoData.PATIENT_PASSWORD)
    }

    fun startDemoCaregiver() = startDemo {
        authRepository.signInCaregiver(DemoData.CAREGIVER_EMAIL, DemoData.CAREGIVER_PASSWORD)
    }

    private fun startDemo(signIn: suspend () -> AuthResult) {
        if (startingDemo.value) return
        startingDemo.value = true
        viewModelScope.launch {
            try {
                if (preferences.languageTag.first() == null) {
                    preferences.setLanguageTag(AppLanguage.ENGLISH.tag)
                }
                preferences.setConsentAccepted(true)
                signIn()
            } finally {
                startingDemo.value = false
            }
        }
    }
}
