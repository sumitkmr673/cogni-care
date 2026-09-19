package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.account.normalizePatientPublicId
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.repository.CareRepository
import com.example.cognicare.repository.LinkPatientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LinkPatientStatus {
    data object Idle : LinkPatientStatus
    data object Linking : LinkPatientStatus
    data class Linked(val patientName: String) : LinkPatientStatus
    data object InvalidId : LinkPatientStatus
    data object NotFound : LinkPatientStatus
    data object AlreadyLinked : LinkPatientStatus
    data object NetworkError : LinkPatientStatus
}

data class LinkPatientUiState(
    val input: String = "",
    val status: LinkPatientStatus = LinkPatientStatus.Idle
) {
    val canSubmit: Boolean get() = input.isNotBlank() && status != LinkPatientStatus.Linking
    val isError: Boolean
        get() = status is LinkPatientStatus.InvalidId || status is LinkPatientStatus.NotFound ||
            status is LinkPatientStatus.AlreadyLinked || status is LinkPatientStatus.NetworkError
}

@HiltViewModel
class LinkPatientViewModel @Inject constructor(
    private val careRepository: CareRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkPatientUiState())
    val uiState: StateFlow<LinkPatientUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) = _uiState.update {
        if (it.status == LinkPatientStatus.Linking) it else it.copy(input = value, status = LinkPatientStatus.Idle)
    }

    fun link() {
        val state = _uiState.value
        if (!state.canSubmit) return
        // Checked on the device first, so a typo gets an immediate, specific answer.
        val publicId = normalizePatientPublicId(state.input) ?: run {
            _uiState.update { it.copy(status = LinkPatientStatus.InvalidId) }
            return
        }

        _uiState.update { it.copy(status = LinkPatientStatus.Linking) }
        viewModelScope.launch {
            val status = when (val result = careRepository.linkPatient(publicId)) {
                is LinkPatientResult.Linked -> {
                    // Show the new patient straight away; the dashboard follows this preference.
                    preferences.setSelectedPatientId(result.patient.id)
                    LinkPatientStatus.Linked(result.patient.name)
                }
                LinkPatientResult.NotFound -> LinkPatientStatus.NotFound
                LinkPatientResult.AlreadyLinked -> LinkPatientStatus.AlreadyLinked
                LinkPatientResult.NetworkError -> LinkPatientStatus.NetworkError
            }
            _uiState.update {
                it.copy(status = status, input = if (status is LinkPatientStatus.Linked) "" else it.input)
            }
        }
    }
}
