package com.example.cognicare.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/dashboard.py and reminder.py.

@Serializable
data class PatientSummaryDto(
    val id: String,
    /** "PT-XXXXXXXX" — what another caregiver types to link this patient. */
    val public_id: String? = null,
    val display_name: String,
    val preferred_language: String? = null,
    val timezone: String? = null,
    val profile_photo_ref: String? = null
)

@Serializable
data class PatientsResponseDto(
    val patients: List<PatientSummaryDto>
)

/** POST /patients/link */
@Serializable
data class PatientLinkRequestDto(
    val public_id: String
)

@Serializable
data class PatientProfileDto(
    val id: String,
    val public_id: String? = null,
    val display_name: String,
    val preferred_language: String? = null,
    val timezone: String? = null,
    val profile_photo_ref: String? = null,
    val date_of_birth: String? = null,
    val gender: String? = null
)

@Serializable
data class CaregiverRelationshipDto(
    val caregiver_id: String,
    val display_name: String,
    val caregiver_type: String,
    val is_primary: Boolean
)

@Serializable
data class SessionResultDto(
    val score: Double,
    val accuracy: Double? = null,
    val correct_answers: Int? = null,
    val total_questions: Int? = null,
    val response_time_ms: Int? = null,
    val mistakes: Int? = null
)

@Serializable
data class RecentGameSessionDto(
    val id: String,
    val game_id: String,
    val game_code: String,
    val game_name: String,
    val started_at: String,
    val completed_at: String? = null,
    val status: String,
    val difficulty_level: Int,
    val result: SessionResultDto? = null
)

@Serializable
data class PerformancePointDto(
    val metric_date: String,
    val memory_score: Double? = null,
    val attention_score: Double? = null,
    val average_accuracy: Double? = null,
    val average_response_time_ms: Int? = null,
    val games_completed: Int,
    val total_sessions: Int
)

@Serializable
data class ReminderItemDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val reminder_type: String,
    val scheduled_at: String,
    val is_recurring: Boolean,
    val recurrence_rule: String? = null
)

@Serializable
data class DashboardResponseDto(
    val patient: PatientProfileDto,
    val caregiver_relationship: CaregiverRelationshipDto? = null,
    val recent_sessions: List<RecentGameSessionDto>,
    val latest_performance: PerformancePointDto? = null,
    val active_reminders: List<ReminderItemDto>
)

@Serializable
data class PerformanceHistoryResponseDto(
    val patient_id: String,
    val metrics: List<PerformancePointDto>
)
