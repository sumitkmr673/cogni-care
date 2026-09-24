package com.example.cognicare.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.time.isSameDay
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<PatientHomeUiState> = authRepository.session
        .filterNotNull()
        .flatMapLatest { session ->
            val targetPatientId = session.patientId ?: session.userId
            combine(
                careRepository.observeReminders(targetPatientId),
                preferences.patientDob,
                preferences.patientGender
            ) { reminders, dob, gender ->
                val now = System.currentTimeMillis()
                val todayReminders = reminders.filter { isSameDay(it.scheduledTime, now) }
                // Next relevant reminder: upcoming today (or within the last 15 minutes)
                val upcoming = todayReminders.filter { it.scheduledTime >= (now - 15 * 60 * 1000L) }
                val next = upcoming.minByOrNull { it.scheduledTime }
                PatientHomeUiState(
                    firstName = session.displayName.substringBefore(' '),
                    fullName = session.displayName,
                    publicId = session.publicId,
                    dateOfBirth = dob,
                    gender = gender,
                    nextReminder = next,
                    isRemindersUnavailable = false,
                    isLoading = false
                )
            }
            .catch { error ->
                Log.w("PatientHomeViewModel", "Failed to fetch reminders for patient $targetPatientId", error)
                val dob = preferences.patientDob.firstOrNull()
                val gender = preferences.patientGender.firstOrNull()
                emit(
                    PatientHomeUiState(
                        firstName = session.displayName.substringBefore(' '),
                        fullName = session.displayName,
                        publicId = session.publicId,
                        dateOfBirth = dob,
                        gender = gender,
                        nextReminder = null,
                        isRemindersUnavailable = true,
                        isLoading = false
                    )
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientHomeUiState())
}
