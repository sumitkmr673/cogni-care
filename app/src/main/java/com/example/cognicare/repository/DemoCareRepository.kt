package com.example.cognicare.repository

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
class DemoCareRepository @Inject constructor() : CareRepository {

    private val reminders = MutableStateFlow(DemoData.reminders())

    override fun observePatients(patientIds: List<String>): Flow<List<PatientProfile>> =
        flowOf(DemoData.patients.filter { it.id in patientIds })

    override fun observeReminders(patientId: String): Flow<List<Reminder>> =
        reminders.map { all -> all.filter { it.patientId == patientId }.sortedBy { it.scheduledTime } }

    override fun observeDashboard(patientId: String): Flow<PatientDashboard?> =
        reminders.map { all ->
            val patient = DemoData.patients.firstOrNull { it.id == patientId } ?: return@map null
            val now = System.currentTimeMillis()
            val upcoming = all
                .filter { it.patientId == patientId && !it.completed && it.scheduledTime >= now }
                .sortedBy { it.scheduledTime }
                .take(3)
            DemoData.dashboard(patient, upcoming, now)
        }

    override suspend fun setReminderCompleted(reminderId: String, completed: Boolean) {
        reminders.update { all ->
            all.map { if (it.id == reminderId) it.copy(completed = completed) else it }
        }
    }
}
