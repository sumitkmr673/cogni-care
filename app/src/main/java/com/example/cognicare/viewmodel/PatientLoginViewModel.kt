package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everyday patient sign-in by saying or typing their name. See [AuthRepository.signInPatient]. */
@HiltViewModel
class PatientLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val form = MutableStateFlow(PatientLoginUiState())

    val uiState: StateFlow<PatientLoginUiState> = combine(
        form,
        authRepository.patientUnlockStatus
    ) { current, status ->
        current.copy(isLockedOut = status.isLockedOut)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientLoginUiState())

    fun onTypedNameChange(value: String) = form.update { it.copy(typedName = value, error = null) }

    /** A spoken name is checked straight away, so the patient doesn't also have to tap a button. */
    fun onSpeechResult(text: String) {
        if (text.isBlank()) return
        form.update { it.copy(heardName = text.trim(), error = null) }
        verify(text)
    }

    fun submitTypedName() {
        val name = form.value.typedName
        if (name.isBlank()) return
        form.update { it.copy(heardName = null) }
        verify(name)
    }

    private fun verify(name: String) {
        if (form.value.isVerifying || uiState.value.isLockedOut) return
        form.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            // On success the root observes the saved session and swaps to the patient graph.
            when (val result = authRepository.signInPatient(name)) {
                is AuthResult.Success -> form.update { it.copy(isVerifying = false) }
                is AuthResult.Failure -> form.update { it.copy(isVerifying = false, error = result.reason) }
            }
        }
    }
}
