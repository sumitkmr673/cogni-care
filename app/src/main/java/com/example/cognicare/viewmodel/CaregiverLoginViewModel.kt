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

@HiltViewModel
class CaregiverLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaregiverLoginUiState())
    val uiState: StateFlow<CaregiverLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun useDemoCredentials() = _uiState.update {
        it.copy(email = DemoData.CAREGIVER_EMAIL, password = DemoData.CAREGIVER_PASSWORD, error = null)
    }

    fun signIn() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val email = state.email.trim()
        if (!EMAIL_PATTERN.matches(email)) {
            _uiState.update { it.copy(error = CaregiverLoginError.INVALID_EMAIL) }
            return
        }

        _uiState.update { it.copy(isSigningIn = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signInCaregiver(email, state.password)
            _uiState.update {
                it.copy(
                    isSigningIn = false,
                    error = if (result is AuthResult.Failure) CaregiverLoginError.INVALID_CREDENTIALS else null
                )
            }
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
