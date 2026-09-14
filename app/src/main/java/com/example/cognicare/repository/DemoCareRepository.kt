package com.example.cognicare.repository

import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoCareRepository @Inject constructor(
    private val locale: AppLocaleProvider
) : CareRepository {

    // Only completion is state; titles are rebuilt per read so they follow the chosen language.
    private val completedIds = MutableStateFlow(emptySet<String>())

    override fun observePatients(patientIds: List<String>): Flow<List<PatientProfile>> =
        flowOf(DemoData.patients.filter { it.id in patientIds })

    override fun observeReminders(patientId: String): Flow<List<Reminder>> =
        completedIds.map { completed ->
            DemoData.reminders(locale, completed)
                .filter { it.patientId == patientId }
                .sortedBy { it.scheduledTime }
        }

    override fun observeDashboard(patientId: String): Flow<PatientDashboard?> =
        completedIds.map { completed ->
            val patient = DemoData.patients.firstOrNull { it.id == patientId } ?: return@map null
            val now = System.currentTimeMillis()
            val upcoming = DemoData.reminders(locale, completed, now)
                .filter { it.patientId == patientId && !it.completed && it.scheduledTime >= now }
                .sortedBy { it.scheduledTime }
                .take(3)
            DemoData.dashboard(patient, upcoming, now)
        }

    override suspend fun setReminderCompleted(reminderId: String, completed: Boolean) {
        completedIds.update { current ->
            if (completed) current + reminderId else current - reminderId
        }
    }
}
