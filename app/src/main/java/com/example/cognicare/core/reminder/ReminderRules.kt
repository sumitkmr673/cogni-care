package com.example.cognicare.core.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** The reminder types the backend accepts (models/reminder.py), in the web portal's order. */
enum class ReminderType(val wireValue: String) {
    GAME("GAME"),
    MEDICATION("MEDICATION"),
    APPOINTMENT("APPOINTMENT"),
    ACTIVITY("ACTIVITY"),
    OTHER("OTHER");

    companion object {
        fun fromWire(value: String): ReminderType = entries.firstOrNull { it.wireValue == value } ?: OTHER
    }
}

/** Same limit as the backend's ReminderCreateRequest.title. */
const val MAX_REMINDER_TITLE = 150

enum class ReminderProblem { MISSING_TITLE, TITLE_TOO_LONG, MISSING_TIME }

/** What is wrong with a reminder before sending it, or null when it can be saved. */
fun reminderProblem(title: String, scheduledAt: Long?): ReminderProblem? = when {
    title.isBlank() -> ReminderProblem.MISSING_TITLE
    title.trim().length > MAX_REMINDER_TITLE -> ReminderProblem.TITLE_TOO_LONG
    scheduledAt == null -> ReminderProblem.MISSING_TIME
    else -> null
}

/**
 * Whether this caregiver may pause, edit or delete a reminder: the backend allows the patient's
 * primary caregiver, or whoever created it. Buttons are hidden otherwise rather than failing.
 */
fun canManageReminder(isPrimaryCaregiver: Boolean, myPublicId: String?, creatorPublicId: String?): Boolean =
    isPrimaryCaregiver || (myPublicId != null && myPublicId == creatorPublicId)

/**
 * A date from Material's date picker (midnight UTC of the chosen day) plus a clock time, as the
 * moment that time happens on that day in [zone]. The picker's UTC convention would otherwise
 * shift the day for anyone east or west of Greenwich.
 */
fun combineDateAndTime(pickerDateUtcMillis: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val day = Instant.ofEpochMilli(pickerDateUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return day.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

/** The reverse, to open the pickers on a reminder's current day: midnight UTC of its local date. */
fun pickerDateUtcMillis(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val day: LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

/** ISO-8601 with the local offset, e.g. 2026-09-20T14:30:00+05:30, which the backend stores as is. */
fun toBackendDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
