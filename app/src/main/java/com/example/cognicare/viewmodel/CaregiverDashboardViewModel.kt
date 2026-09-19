package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CaregiverDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val manualRefresh = MutableStateFlow(0)

    /**
     * Re-fetches every [AUTO_REFRESH_MS] while a caregiver screen is on screen, so games the
     * patient plays on their own phone show up without reopening the app. It runs only while the
     * state is collected (WhileSubscribed below), so nothing polls in the background.
     */
    private val refreshTicks = combine(
        flow {
            var tick = 0
            while (true) {
                emit(tick++)
                delay(AUTO_REFRESH_MS)
            }
        },
        manualRefresh
    ) { tick, manual -> tick to manual }

    val uiState: StateFlow<CaregiverDashboardUiState> = combine(
        authRepository.session.filterNotNull(),
        refreshTicks
    ) { session, _ -> session }
        .flatMapLatest { session ->
            combine(
                careRepository.observePatients(session.linkedPatientIds),
                preferences.selectedPatientId
            ) { patients, selectedId ->
                patients to (patients.firstOrNull { it.id == selectedId } ?: patients.firstOrNull())
            }.flatMapLatest { (patients, selected) ->
                val dashboard = selected?.let { careRepository.observeDashboard(it.id) } ?: flowOf(null)
                dashboard.map {
                    CaregiverDashboardUiState(
                        caregiverName = session.displayName,
                        patients = patients,
                        dashboard = it,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaregiverDashboardUiState())

    /** Returns the write job so a screen can wait for it before leaving (which clears this ViewModel). */
    fun selectPatient(patientId: String): Job =
        viewModelScope.launch { preferences.setSelectedPatientId(patientId) }

    /** Fetches again now, e.g. from a pull-to-refresh, instead of waiting for the next tick. */
    fun refresh() {
        manualRefresh.update { it + 1 }
    }

    private companion object {
        const val AUTO_REFRESH_MS = 30_000L
    }
}
