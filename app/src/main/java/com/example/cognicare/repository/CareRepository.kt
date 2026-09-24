package com.example.cognicare.repository

import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GameType
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Single source of care data for both the patient and caretaker graphs, backed by the
 * cogni-care FastAPI backend. The Room-backed offline queue is a later step; for now every call
 * is a live network fetch and [recordGameCompletion] fails silently if offline.
 */
interface CareRepository {
    fun observePatients(patientIds: List<String>): Flow<List<PatientProfile>>

    fun observeReminders(patientId: String): Flow<List<Reminder>>

    fun observeDashboard(patientId: String): Flow<PatientDashboard?>

    suspend fun setReminderCompleted(reminderId: String, completed: Boolean)

    /** Starts and submits a backend game session for the signed-in patient. No-ops (and never
     * throws) for a [GameType] with no backend counterpart — see [toBackendCode]. */
    suspend fun recordGameCompletion(gameType: GameType, outcome: GameOutcome)

    /** Links an existing patient (by their public ID) to the signed-in caregiver, becoming the
     * primary caregiver if the patient doesn't have one yet, or a secondary one otherwise. */
    suspend fun linkPatient(publicId: String): LinkPatientResult
}

sealed interface LinkPatientResult {
    data class Success(val patient: PatientProfile) : LinkPatientResult
    data class Failure(val reason: LinkPatientFailure) : LinkPatientResult
}

enum class LinkPatientFailure {
    NOT_FOUND,
    ALREADY_LINKED,
    NETWORK_ERROR
}
