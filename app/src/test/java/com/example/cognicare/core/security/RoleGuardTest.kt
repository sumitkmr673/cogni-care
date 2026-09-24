package com.example.cognicare.core.security

import com.example.cognicare.data.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleGuardTest {

    @Test
    fun patientCannotReachCaregiverArea() {
        assertTrue(UserRole.PATIENT.canAccess(AppArea.PATIENT))
        assertFalse(UserRole.PATIENT.canAccess(AppArea.CAREGIVER))
    }

    @Test
    fun caregiverOnlyReachesCaregiverArea() {
        assertTrue(UserRole.CAREGIVER.canAccess(AppArea.CAREGIVER))
        assertFalse(UserRole.CAREGIVER.canAccess(AppArea.PATIENT))
    }
}
