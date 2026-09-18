package com.example.cognicare.core.time

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DayPart {
    MORNING, AFTERNOON, EVENING;

    companion object {
        fun at(millis: Long = System.currentTimeMillis()): DayPart {
            val hour = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> MORNING
                hour < 17 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun isSameDay(first: Long, second: Long): Boolean = startOfDay(first) == startOfDay(second)

fun formatDayMonth(millis: Long): String = format("d MMM", millis)

fun formatTime(millis: Long): String = format("h:mm a", millis)

fun formatFullDate(millis: Long): String = format("EEEE, d MMMM", millis)

fun formatWeekdayDateTime(millis: Long): String = format("EEE, d MMM · h:mm a", millis)

private fun format(pattern: String, millis: Long): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

/** Parses a backend ISO-8601 timestamp (e.g. "2026-09-15T10:00:00+00:00") to epoch millis. */
fun parseIsoDateTimeMillis(iso: String): Long? =
    runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrNull()

/** Parses a backend plain date (e.g. "2026-09-15") to local midnight, epoch millis. */
fun parseIsoDateMillis(isoDate: String): Long? =
    runCatching {
        LocalDate.parse(isoDate).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
