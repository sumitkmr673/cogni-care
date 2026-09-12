package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.time.isSameDay
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository
) : ViewModel() {

    val uiState: StateFlow<PatientHomeUiState> = authRepository.session
        .filterNotNull()
        .flatMapLatest { session ->
            careRepository.observeReminders(session.userId).map { reminders ->
                val now = System.currentTimeMillis()
                PatientHomeUiState(
                    firstName = session.displayName.substringBefore(' '),
                    todayReminders = reminders.filter { isSameDay(it.scheduledTime, now) },
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientHomeUiState())

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            careRepository.setReminderCompleted(reminder.id, !reminder.completed)
        }
    }
}
