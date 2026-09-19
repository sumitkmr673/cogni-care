package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.account.CaregiverType
import com.example.cognicare.core.account.RegistrationProblem
import com.example.cognicare.core.account.registrationProblem
import com.example.cognicare.repository.AuthFailure
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.AuthResult
import com.example.cognicare.repository.CaregiverRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaregiverRegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val type: CaregiverType = CaregiverType.FAMILY,
    val isSubmitting: Boolean = false,
    /** A field the form itself rejected, shown under that field. */
    val problem: RegistrationProblem? = null,
    /** What the server said, shown under the button. */
    val failure: AuthFailure? = null
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

@HiltViewModel
class CaregiverRegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaregiverRegisterUiState())
    val uiState: StateFlow<CaregiverRegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = edit { it.copy(name = value) }
    fun onEmailChange(value: String) = edit { it.copy(email = value) }
    fun onPasswordChange(value: String) = edit { it.copy(password = value) }
    fun onPhoneChange(value: String) = edit { it.copy(phone = value) }
    fun onTypeChange(value: CaregiverType) = edit { it.copy(type = value) }

    /** On success the session is saved and the app switches to the caregiver screens by itself. */
    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        registrationProblem(state.name, state.email, state.password)?.let { problem ->
            _uiState.update { it.copy(problem = problem) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, problem = null, failure = null) }
        viewModelScope.launch {
            val result = authRepository.registerCaregiver(
                CaregiverRegistration(
                    name = state.name,
                    email = state.email,
                    password = state.password,
                    type = state.type,
                    phone = state.phone
                )
            )
            _uiState.update {
                it.copy(isSubmitting = false, failure = (result as? AuthResult.Failure)?.reason)
            }
        }
    }

    // Any edit clears the previous error, so it never lingers under a field the user just fixed.
    private fun edit(change: (CaregiverRegisterUiState) -> CaregiverRegisterUiState) =
        _uiState.update { change(it).copy(problem = null, failure = null) }
}
