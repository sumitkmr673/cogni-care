package com.example.cognicare.data.demo

import androidx.annotation.StringRes
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.core.time.startOfDay
import com.example.cognicare.data.model.FamilyMember
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.data.model.ReminderKind

/**
 * Three kinds of content live here now that the app talks to the real backend
 * (cogni-care/backend, seeded by `python -m app.scripts.seed_demo`):
 *
 * - Real seeded account credentials, for the Welcome screen's "start with the demo X" shortcuts.
 * - [familyMembers]: the Family Identification game's content, purely local — the backend has no
 *   concept of a patient's family photo or relations, only game session results.
 * - [reminders]: a local fallback schedule. The backend has no endpoint for a patient to read
 *   their own reminders (that list is caregiver/doctor-only — see [RemoteCareRepository]),
 *   so this is what the patient's home screen falls back to.
 */
object DemoData {
    // Matches backend/app/scripts/seed_demo.py exactly. Development credentials, not for reuse
    // anywhere real — see that repo's README for the full list of ten seeded accounts.
    const val PATIENT_EMAIL = "demo.patient@cogni-care.example"
    const val PATIENT_PASSWORD = "DemoPatientOnly-2026!"
    const val CAREGIVER_EMAIL = "demo.caregiver@cogni-care.example"
    const val CAREGIVER_PASSWORD = "DemoCaregiverOnly-2026!"

    /** First name on the seeded demo patient account ("Meera Sharma (Demo)"), which the patient says or types to sign in. */
    const val PATIENT_NAME = "Meera"

    private const val PLACEHOLDER_PATIENT_ID = "local-fallback"
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    // Face boxes are measured in pixels on the 612x408 stock photo (res/drawable/family_photo.jpg).
    private const val PHOTO_WIDTH = 612f
    private const val PHOTO_HEIGHT = 408f

    private fun familyMember(
        name: String,
        relation: String,
        left: Int,
        top: Int,
        size: Int,
        isPatient: Boolean = false
    ) = FamilyMember(
        name = name,
        relation = relation,
        photoRes = R.drawable.family_photo,
        faceLeft = left / PHOTO_WIDTH,
        faceTop = top / PHOTO_HEIGHT,
        faceSize = size / PHOTO_WIDTH,
        isPatient = isPatient
    )

    /** Meera's family, left to right in the photo. Meera herself is the woman in the white sweater. */
    val familyMembers: List<FamilyMember> = listOf(
        familyMember("Arjun", "grandson", left = 25, top = 120, size = 105),
        familyMember("Rohan", "son", left = 140, top = 68, size = 92),
        familyMember("Aditi", "granddaughter", left = 207, top = 94, size = 66),
        familyMember("Priya", "daughter-in-law", left = 272, top = 108, size = 80),
        familyMember("Meera", "self", left = 356, top = 138, size = 76, isPatient = true),
        familyMember("Vikram", "husband", left = 430, top = 105, size = 95)
    )

    /**
     * Reminder titles live in resources so the fallback reads in the chosen language. Real
     * reminders (from the caregiver dashboard) are caregiver-entered text, which is why
     * [Reminder.title] itself stays a plain string.
     */
    private data class ReminderTemplate(
        val id: String,
        @StringRes val titleRes: Int,
        val kind: ReminderKind,
        val offsetMillis: Long,
        val isRecurring: Boolean = true
    )

    private val reminderTemplates = listOf(
        ReminderTemplate("reminder-walk", R.string.reminder_morning_walk, ReminderKind.WALK, 7 * HOUR + 30 * MINUTE),
        ReminderTemplate("reminder-medicine", R.string.reminder_medicine, ReminderKind.MEDICATION, 9 * HOUR),
        ReminderTemplate("reminder-lunch", R.string.reminder_lunch, ReminderKind.MEAL, 13 * HOUR),
        ReminderTemplate("reminder-game", R.string.reminder_game, ReminderKind.GAME, 15 * HOUR + 30 * MINUTE),
        ReminderTemplate("reminder-evening-walk", R.string.reminder_evening_walk, ReminderKind.WALK, DAY + 18 * HOUR),
        ReminderTemplate("reminder-care-review", R.string.reminder_care_review, ReminderKind.APPOINTMENT, 3 * DAY + 16 * HOUR + 30 * MINUTE, isRecurring = false)
    )

    /** [patientId] is stamped onto each entry by the caller (see [RemoteCareRepository]); it is not known here. */
    fun reminders(
        locale: AppLocaleProvider,
        completedIds: Set<String> = emptySet(),
        now: Long = System.currentTimeMillis(),
        patientId: String = PLACEHOLDER_PATIENT_ID
    ): List<Reminder> {
        val today = startOfDay(now)
        return reminderTemplates.map { template ->
            Reminder(
                id = template.id,
                patientId = patientId,
                title = locale.getString(template.titleRes),
                kind = template.kind,
                scheduledTime = today + template.offsetMillis,
                isRecurring = template.isRecurring,
                completed = template.id in completedIds
            )
        }
    }
}
