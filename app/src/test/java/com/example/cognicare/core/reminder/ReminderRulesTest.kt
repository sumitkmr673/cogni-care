package com.example.cognicare.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderRulesTest {

    private val india = ZoneId.of("Asia/Kolkata")

    @Test
    fun `the picked day and time are kept in the caregiver's own time zone`() {
        // Material's date picker gives 20 Sep at midnight UTC; 2:30 pm is meant in India.
        val pickerDay = ZonedDateTime.of(2026, 9, 20, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val millis = combineDateAndTime(pickerDay, 14, 30, india)
        assertEquals(ZonedDateTime.of(2026, 9, 20, 14, 30, 0, 0, india).toInstant().toEpochMilli(), millis)
    }

    @Test
    fun `an early-morning reminder does not slip to the previous day`() {
        val lateEveningUtc = ZonedDateTime.of(2026, 9, 26, 0, 17, 0, 0, india).toInstant().toEpochMilli()
        val day = pickerDateUtcMillis(lateEveningUtc, india)
        assertEquals(lateEveningUtc, combineDateAndTime(day, 0, 17, india))
    }

    @Test
    fun `times go to the backend with the local offset`() {
        val millis = ZonedDateTime.of(2026, 9, 20, 14, 30, 0, 0, india).toInstant().toEpochMilli()
        assertEquals("2026-09-20T14:30:00+05:30", toBackendDateTime(millis, india))
    }

    @Test
    fun `only the primary caregiver or the creator may change a reminder`() {
        assertTrue(canManageReminder(isPrimaryCaregiver = true, myPublicId = "CG-B", creatorPublicId = "CG-A"))
        assertTrue(canManageReminder(isPrimaryCaregiver = false, myPublicId = "CG-A", creatorPublicId = "CG-A"))
        assertFalse(canManageReminder(isPrimaryCaregiver = false, myPublicId = "CG-B", creatorPublicId = "CG-A"))
        assertFalse(canManageReminder(isPrimaryCaregiver = false, myPublicId = null, creatorPublicId = null))
    }

    @Test
    fun `a reminder needs a short title and a time`() {
        assertEquals(ReminderProblem.MISSING_TITLE, reminderProblem("  ", 1L))
        assertEquals(ReminderProblem.TITLE_TOO_LONG, reminderProblem("x".repeat(151), 1L))
        assertEquals(ReminderProblem.MISSING_TIME, reminderProblem("Walk", null))
        assertNull(reminderProblem("Walk", 1L))
    }

    @Test
    fun `unknown backend types are shown as other`() {
        assertEquals(ReminderType.MEDICATION, ReminderType.fromWire("MEDICATION"))
        assertEquals(ReminderType.OTHER, ReminderType.fromWire("SOMETHING_NEW"))
    }
}
