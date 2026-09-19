package com.example.cognicare.data.model

import androidx.annotation.DrawableRes

enum class SyncStatus { PENDING, SYNCED, FAILED }

/** The four cognitive domains the scoring service reports on. */
enum class CognitiveDomain { MEMORY, EXECUTIVE, LANGUAGE, VISUOSPATIAL }

enum class GameType(val domain: CognitiveDomain) {
    MEMORY_MATCH(CognitiveDomain.MEMORY),
    PATTERN_RECALL(CognitiveDomain.EXECUTIVE),
    OBJECT_NAMING(CognitiveDomain.LANGUAGE),
    DAILY_RECALL(CognitiveDomain.MEMORY),
    ORIENTATION(CognitiveDomain.EXECUTIVE),
    FAMILY_IDENTIFICATION(CognitiveDomain.VISUOSPATIAL)
}

enum class Trend { IMPROVING, STABLE, DECLINING }

// WALK and MEAL are local-only categories from the original design; ACTIVITY and OTHER are the
// backend's own reminder_type values (see cogni-care/backend/app/models/reminder.py) and cover
// anything the backend sends that doesn't fit the others.
enum class ReminderKind { GAME, MEDICATION, WALK, MEAL, APPOINTMENT, ACTIVITY, OTHER }

enum class AlertType { MISSED_ACTIVITY, PERFORMANCE_DROP, SYNC_OVERDUE }

/**
 * A person the patient should recognise, located in a shared family photo. The face box is a
 * square given as fractions: [faceLeft] and [faceSize] of the photo's width, [faceTop] of its height.
 * [isPatient] marks the patient themself, asked as "Who is this?" → "me".
 */
data class FamilyMember(
    val name: String,
    val relation: String,
    @DrawableRes val photoRes: Int,
    val faceLeft: Float,
    val faceTop: Float,
    val faceSize: Float,
    val isPatient: Boolean = false
)

data class PatientProfile(
    val id: String,
    val name: String,
    val languageTag: String,
    val timeZoneId: String,
    /** "PT-XXXXXXXX": shown to caregivers so they can share it for linking. */
    val publicId: String? = null
)

data class GameSession(
    val id: String,
    val patientId: String,
    val gameType: GameType,
    val startedAt: Long,
    val completedAt: Long?,
    val rawResultPayload: String,
    val syncStatus: SyncStatus
)

/**
 * Shaped like a FHIR Observation (subject, code, value, effective time) so a
 * FHIR/ABDM adapter can map it later without reshaping stored data.
 */
data class CognitiveScoreInsight(
    val id: String,
    val patientId: String,
    val domain: CognitiveDomain,
    val score: Double,
    val trend: Trend,
    val generatedAt: Long
) {
    val isNonDiagnostic: Boolean get() = true
}

data class Reminder(
    val id: String,
    val patientId: String,
    val title: String,
    val kind: ReminderKind,
    val scheduledTime: Long,
    val isRecurring: Boolean,
    val completed: Boolean
)

data class CareAlert(
    val id: String,
    val patientId: String,
    val caretakerId: String,
    val type: AlertType,
    val message: String,
    val timestamp: Long,
    val acknowledged: Boolean
)

data class CareSuggestion(
    val id: String,
    val patientId: String,
    val suggestionText: String,
    val generatedAt: Long,
    val humanReviewed: Boolean,
    val reviewedBy: String? = null
)
