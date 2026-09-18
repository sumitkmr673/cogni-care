package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-time setup: a real backend login for the patient, after which saying or typing their name is
 * all that's needed. See [com.example.cognicare.repository.AuthRepository.setupPatientDevice]. */
@HiltViewModel
class PatientDeviceSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientDeviceSetupUiState())
    val uiState: StateFlow<PatientDeviceSetupUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun useDemoCredentials() = _uiState.update {
        it.copy(email = DemoData.PATIENT_EMAIL, password = DemoData.PATIENT_PASSWORD, error = null)
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.setupPatientDevice(state.email.trim(), state.password)
            _uiState.update {
                it.copy(isSubmitting = false, error = (result as? AuthResult.Failure)?.reason)
            }
        }
    }
}
