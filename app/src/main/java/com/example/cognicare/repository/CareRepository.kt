package com.example.cognicare.repository

import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Single source of care data for both the patient and caretaker graphs.
 * The demo implementation keeps it in memory; the sync step backs it with Room.
 */
interface CareRepository {
    fun observePatients(patientIds: List<String>): Flow<List<PatientProfile>>

    fun observeReminders(patientId: String): Flow<List<Reminder>>

    fun observeDashboard(patientId: String): Flow<PatientDashboard?>

    suspend fun setReminderCompleted(reminderId: String, completed: Boolean)
}
