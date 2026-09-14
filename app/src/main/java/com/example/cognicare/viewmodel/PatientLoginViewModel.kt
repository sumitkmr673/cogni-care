package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class PatientLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientLoginUiState())
    val uiState: StateFlow<PatientLoginUiState> = _uiState.asStateFlow()

    fun onDigit(digit: Int) {
        val state = _uiState.value
        if (state.isVerifying || state.pin.length >= PatientLoginUiState.PIN_LENGTH) return

        val pin = state.pin + digit
        _uiState.value = state.copy(pin = pin, showError = false)
        if (pin.length == PatientLoginUiState.PIN_LENGTH) verify(pin)
    }

    fun onBackspace() {
        _uiState.update {
            if (it.isVerifying) it else it.copy(pin = it.pin.dropLast(1), showError = false)
        }
    }

    private fun verify(pin: String) {
        _uiState.update { it.copy(isVerifying = true) }
        viewModelScope.launch {
            // On success the root observes the saved session and swaps to the patient graph.
            when (authRepository.signInPatient(pin)) {
                is AuthResult.Success -> _uiState.update { it.copy(isVerifying = false) }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(pin = "", isVerifying = false, showError = true)
                }
            }
        }
    }
}
