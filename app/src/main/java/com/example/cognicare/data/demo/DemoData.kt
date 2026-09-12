package com.example.cognicare.data.demo

import com.example.cognicare.core.time.startOfDay
import com.example.cognicare.data.model.DailyScorePoint
import com.example.cognicare.data.model.GamePerformance
import com.example.cognicare.data.model.GameSession
import com.example.cognicare.data.model.GameType
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.data.model.ReminderKind
import com.example.cognicare.data.model.SyncStatus

/** Synthetic demonstration data. Replaced by Room-backed data in the sync step. */
object DemoData {
    const val PATIENT_ID = "demo-patient-meera"
    const val PATIENT_PIN = "1234"
    const val CAREGIVER_ID = "demo-caregiver-ananya"
    const val CAREGIVER_EMAIL = "ananya.mehta@example.com"
    const val CAREGIVER_PASSWORD = "demo123"

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    val meera = PatientProfile(
        id = PATIENT_ID,
        name = "Meera Sharma (Demo)",
        languageTag = "en",
        timeZoneId = "Asia/Kolkata"
    )

    val patients: List<PatientProfile> = listOf(meera)

    fun reminders(now: Long = System.currentTimeMillis()): List<Reminder> {
        val today = startOfDay(now)
        return listOf(
            Reminder("reminder-walk", PATIENT_ID, "Morning walk", ReminderKind.WALK, today + 7 * HOUR + 30 * MINUTE, isRecurring = true, completed = false),
            Reminder("reminder-medicine", PATIENT_ID, "Take morning medicine", ReminderKind.MEDICATION, today + 9 * HOUR, isRecurring = true, completed = false),
            Reminder("reminder-lunch", PATIENT_ID, "Lunch with family", ReminderKind.MEAL, today + 13 * HOUR, isRecurring = true, completed = false),
            Reminder("reminder-game", PATIENT_ID, "Complete today's memory game", ReminderKind.GAME, today + 15 * HOUR + 30 * MINUTE, isRecurring = true, completed = false),
            Reminder("reminder-evening-walk", PATIENT_ID, "Evening walk", ReminderKind.WALK, today + DAY + 18 * HOUR, isRecurring = true, completed = false),
            Reminder("reminder-care-review", PATIENT_ID, "Care review appointment", ReminderKind.APPOINTMENT, today + 3 * DAY + 16 * HOUR + 30 * MINUTE, isRecurring = false, completed = false)
        )
    }

    fun dashboard(
        patient: PatientProfile,
        upcomingReminders: List<Reminder>,
        now: Long = System.currentTimeMillis()
    ): PatientDashboard {
        val today = startOfDay(now)
        val memory = listOf(78f, 79f, 81f, 80f, 83f, 84f, 84f, 86f, 87f, 88f)
        val attention = listOf(80f, 82f, 81f, 84f, 85f, 86f, 88f, 88f, 89f, 90f)
        val trend = memory.indices.map { index ->
            DailyScorePoint(
                dayStartMillis = today - (memory.lastIndex - index) * DAY,
                memory = memory[index],
                attention = attention[index]
            )
        }

        return PatientDashboard(
            patient = patient,
            gamesCompletedToday = 1,
            averageAccuracyPercent = 92,
            memoryScore = 88,
            attentionScore = 90,
            averageResponseSeconds = 0.9,
            trend = trend,
            gamePerformance = listOf(
                GamePerformance(GameType.DAILY_RECALL, completedCount = 2, latestLevel = 2, accuracyPercent = 90),
                GamePerformance(GameType.MEMORY_MATCH, completedCount = 2, latestLevel = 3, accuracyPercent = 90),
                GamePerformance(GameType.ORIENTATION, completedCount = 2, latestLevel = 2, accuracyPercent = 87),
                GamePerformance(GameType.FAMILY_IDENTIFICATION, completedCount = 2, latestLevel = 3, accuracyPercent = 92),
                GamePerformance(GameType.OBJECT_NAMING, completedCount = 2, latestLevel = 2, accuracyPercent = 88)
            ),
            recentSessions = listOf(
                GameSession("session-3", patient.id, GameType.DAILY_RECALL, now - 2 * HOUR - 6 * MINUTE, now - 2 * HOUR, "{}", SyncStatus.PENDING),
                GameSession("session-2", patient.id, GameType.MEMORY_MATCH, now - DAY - 3 * HOUR, now - DAY - 3 * HOUR + 7 * MINUTE, "{}", SyncStatus.SYNCED),
                GameSession("session-1", patient.id, GameType.FAMILY_IDENTIFICATION, now - 2 * DAY - 5 * HOUR, now - 2 * DAY - 5 * HOUR + 9 * MINUTE, "{}", SyncStatus.SYNCED)
            ),
            upcomingReminders = upcomingReminders,
            lastSyncedAt = now - 2 * HOUR
        )
    }
}
