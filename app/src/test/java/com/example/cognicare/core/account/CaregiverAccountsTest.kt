package com.example.cognicare.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaregiverAccountsTest {

    @Test
    fun patientIdsAreNormalisedFromCommonTypingSlips() {
        listOf("PT-AB12CD34", "pt-ab12cd34", "PTAB12CD34", "AB12CD34", " pt ab12 cd34 ").forEach { typed ->
            assertEquals("typed '$typed'", "PT-AB12CD34", normalizePatientPublicId(typed))
        }
    }

    @Test
    fun thingsThatCannotBePatientIdsAreRejected() {
        listOf("", "PT-", "PT-AB12CD3", "PT-AB12CD345", "CG-AB12CD34", "PT-AB12CD3!", "hello").forEach { typed ->
            assertNull("typed '$typed'", normalizePatientPublicId(typed))
        }
    }

    @Test
    fun aCaregiverIdIsNotMistakenForAPatientId() {
        // Caregiver IDs look similar (CG-XXXXXXXX); linking one must fail before reaching the server.
        assertNull(normalizePatientPublicId("CG-DOCTOR01"))
    }

    @Test
    fun registrationReportsTheFirstProblemInFieldOrder() {
        assertEquals(RegistrationProblem.NAME_REQUIRED, registrationProblem("  ", "bad", "short"))
        assertEquals(RegistrationProblem.INVALID_EMAIL, registrationProblem("Ravi", "ravi@", "longenough"))
        assertEquals(RegistrationProblem.PASSWORD_TOO_SHORT, registrationProblem("Ravi", "ravi@example.com", "1234567"))
        assertNull(registrationProblem("Ravi", " ravi@example.com ", "12345678"))
    }

    @Test
    fun caregiverTypesMatchWhatTheBackendAccepts() {
        assertEquals(
            listOf("FAMILY", "DOCTOR", "PROFESSIONAL_CAREGIVER", "OTHER"),
            CaregiverType.entries.map { it.wireValue }
        )
    }
}
