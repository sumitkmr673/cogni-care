package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.Reminder
import com.example.cognicare.data.model.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientHomeUiStateTest {

    @Test
    fun defaultStateIsLoadingWithNoReminders() {
        val state = PatientHomeUiState()
        assertTrue(state.isLoading)
        assertNull(state.nextReminder)
        assertFalse(state.isRemindersUnavailable)
        assertEquals("", state.firstName)
        assertNull(state.publicId)
    }

    @Test
    fun stateHoldsNextReminderCorrectly() {
        val reminder = Reminder(
            id = "rem-1",
            patientId = "pat-123",
            title = "Morning Medicine",
            scheduledTime = 1726650000000L,
            kind = ReminderKind.MEDICATION,
            isRecurring = false,
            completed = false
        )
        val state = PatientHomeUiState(
            firstName = "Ramesh",
            publicId = "PT-ABC12345",
            nextReminder = reminder,
            isLoading = false
        )
        assertFalse(state.isLoading)
        assertEquals("Ramesh", state.firstName)
        assertEquals("PT-ABC12345", state.publicId)
        assertEquals(reminder, state.nextReminder)
        assertFalse(state.isRemindersUnavailable)
    }

    @Test
    fun stateReflectsUnavailableReminders() {
        val state = PatientHomeUiState(
            firstName = "Sita",
            publicId = "PT-XYZ98765",
            isRemindersUnavailable = true,
            isLoading = false
        )
        assertFalse(state.isLoading)
        assertTrue(state.isRemindersUnavailable)
        assertNull(state.nextReminder)
    }
}
