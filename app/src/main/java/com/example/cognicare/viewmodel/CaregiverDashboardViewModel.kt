package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import com.example.cognicare.repository.LinkPatientFailure
import com.example.cognicare.repository.LinkPatientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaregiverDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    // Bumped after a successful patient link so the patients list (a single-shot flow) re-fetches.
    private val refreshTick = MutableStateFlow(0)
    private val dialogState = MutableStateFlow(LinkPatientDialogState())

    private val dataFlow: Flow<DashboardData> = combine(
        authRepository.session.filterNotNull(),
        refreshTick
    ) { session, _ -> session }
        .flatMapLatest { session ->
            combine(
                careRepository.observePatients(session.linkedPatientIds),
                preferences.selectedPatientId
            ) { patients, selectedId ->
                patients to (patients.firstOrNull { it.id == selectedId } ?: patients.firstOrNull())
            }.flatMapLatest { (patients, selected) ->
                val dashboard = selected?.let { careRepository.observeDashboard(it.id) } ?: flowOf(null)
                dashboard.map { DashboardData(session.displayName, patients, it) }
            }
        }

    val uiState: StateFlow<CaregiverDashboardUiState> = combine(dataFlow, dialogState) { data, dialog ->
        CaregiverDashboardUiState(
            caregiverName = data.caregiverName,
            patients = data.patients,
            dashboard = data.dashboard,
            isLoading = false,
            isLinkPatientDialogVisible = dialog.isVisible,
            linkPublicId = dialog.publicId,
            isLinkingPatient = dialog.isLinking,
            linkPatientError = dialog.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaregiverDashboardUiState())

    /** Returns the write job so a screen can wait for it before leaving (which clears this ViewModel). */
    fun selectPatient(patientId: String): Job =
        viewModelScope.launch { preferences.setSelectedPatientId(patientId) }

    // --- Link an existing patient by their public ID ---
    fun onShowLinkPatientDialog() = dialogState.update { it.copy(isVisible = true, error = null) }

    fun onDismissLinkPatientDialog() = dialogState.update { LinkPatientDialogState() }

    fun onLinkPublicIdChange(value: String) = dialogState.update { it.copy(publicId = value.uppercase(), error = null) }

    fun onSubmitLinkPatient() {
        val current = dialogState.value
        if (current.publicId.trim().isBlank() || current.isLinking) return

        dialogState.update { it.copy(isLinking = true, error = null) }
        viewModelScope.launch {
            when (val result = careRepository.linkPatient(current.publicId.trim())) {
                is LinkPatientResult.Success -> {
                    dialogState.update { LinkPatientDialogState() }
                    preferences.setSelectedPatientId(result.patient.id)
                    refreshTick.update { it + 1 }
                }
                is LinkPatientResult.Failure -> dialogState.update { it.copy(isLinking = false, error = result.reason) }
            }
        }
    }

    private data class DashboardData(
        val caregiverName: String,
        val patients: List<PatientProfile>,
        val dashboard: PatientDashboard?
    )

    private data class LinkPatientDialogState(
        val isVisible: Boolean = false,
        val publicId: String = "",
        val isLinking: Boolean = false,
        val error: LinkPatientFailure? = null
    )
}
