package com.example.cognicare.viewmodel

import com.example.cognicare.core.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatientDeviceSetupUiStateTest {

    @Test
    fun initialStepIsWelcome() {
        val state = PatientDeviceSetupUiState()
        assertEquals(PatientSetupStep.WELCOME, state.step)
        assertTrue(state.canProceed)
    }

    @Test
    fun nameStepRequiresNonBlankName() {
        val emptyState = PatientDeviceSetupUiState(step = PatientSetupStep.NAME, patientName = "   ")
        assertFalse(emptyState.canProceed)

        val validState = PatientDeviceSetupUiState(step = PatientSetupStep.NAME, patientName = "Ramesh Sharma")
        assertTrue(validState.canProceed)
    }

    @Test
    fun birthDateValidationWorksCorrectly() {
        val invalidDay = PatientDeviceSetupUiState(
            step = PatientSetupStep.BIRTH_DATE,
            birthDay = "35",
            birthMonth = "05",
            birthYear = "1950"
        )
        assertFalse(invalidDay.isDobValid)
        assertFalse(invalidDay.canProceed)

        val invalidMonth = PatientDeviceSetupUiState(
            step = PatientSetupStep.BIRTH_DATE,
            birthDay = "15",
            birthMonth = "13",
            birthYear = "1950"
        )
        assertFalse(invalidMonth.isDobValid)

        val invalidYear = PatientDeviceSetupUiState(
            step = PatientSetupStep.BIRTH_DATE,
            birthDay = "15",
            birthMonth = "05",
            birthYear = "1850"
        )
        assertFalse(invalidYear.isDobValid)

        val validDob = PatientDeviceSetupUiState(
            step = PatientSetupStep.BIRTH_DATE,
            birthDay = "15",
            birthMonth = "05",
            birthYear = "1950"
        )
        assertTrue(validDob.isDobValid)
        assertTrue(validDob.canProceed)
        assertEquals("1950-05-15", validDob.formattedDobIso)
    }

    @Test
    fun genderStepRequiresSelection() {
        val unselected = PatientDeviceSetupUiState(step = PatientSetupStep.GENDER, gender = null)
        assertFalse(unselected.canProceed)

        val womanSelected = PatientDeviceSetupUiState(step = PatientSetupStep.GENDER, gender = "Woman")
        assertTrue(womanSelected.canProceed)

        val manSelected = PatientDeviceSetupUiState(step = PatientSetupStep.GENDER, gender = "Man")
        assertTrue(manSelected.canProceed)

        val otherSelected = PatientDeviceSetupUiState(step = PatientSetupStep.GENDER, gender = "Prefer not to say")
        assertTrue(otherSelected.canProceed)
    }

    @Test
    fun languageStepRequiresSupportedLanguage() {
        val supportedEnglish = PatientDeviceSetupUiState(step = PatientSetupStep.LANGUAGE, selectedLanguage = AppLanguage.ENGLISH)
        assertTrue(supportedEnglish.canProceed)

        val supportedHindi = PatientDeviceSetupUiState(step = PatientSetupStep.LANGUAGE, selectedLanguage = AppLanguage.HINDI)
        assertTrue(supportedHindi.canProceed)

        val supportedBengali = PatientDeviceSetupUiState(step = PatientSetupStep.LANGUAGE, selectedLanguage = AppLanguage.BENGALI)
        assertTrue(supportedBengali.canProceed)

        val untranslatedAssamese = PatientDeviceSetupUiState(step = PatientSetupStep.LANGUAGE, selectedLanguage = AppLanguage.ASSAMESE)
        assertFalse(untranslatedAssamese.canProceed)
    }

    @Test
    fun consentStepRequiresAcceptedCheckbox() {
        val unaccepted = PatientDeviceSetupUiState(step = PatientSetupStep.CONSENT, consentAccepted = false)
        assertFalse(unaccepted.canProceed)

        val accepted = PatientDeviceSetupUiState(step = PatientSetupStep.CONSENT, consentAccepted = true)
        assertTrue(accepted.canProceed)
    }

    @Test
    fun dateClampingAndLeapYearsHandledCorrectly() {
        // February non-leap year (1951) has 28 days
        assertEquals(28, maxDaysInMonth(2, 1951))
        // February leap year (1952) has 29 days
        assertEquals(29, maxDaysInMonth(2, 1952))
        // April has 30 days
        assertEquals(30, maxDaysInMonth(4, 1950))
        // May has 31 days
        assertEquals(31, maxDaysInMonth(5, 1950))

        // Feb 31 is invalid
        val feb31 = PatientDeviceSetupUiState(
            step = PatientSetupStep.BIRTH_DATE,
            birthDay = "31",
            birthMonth = "02",
            birthYear = "1952"
        )
        assertFalse(feb31.isDobValid)
        assertFalse(feb31.canProceed)
    }

    @Test
    fun yearRangeEnforcesExactExistingBounds() {
        assertEquals(1900, MIN_BIRTH_YEAR)
        assertEquals(2026, MAX_BIRTH_YEAR)

        val minYear = PatientDeviceSetupUiState(step = PatientSetupStep.BIRTH_DATE, birthDay = "01", birthMonth = "01", birthYear = "1900")
        assertTrue(minYear.isDobValid)

        val maxYear = PatientDeviceSetupUiState(step = PatientSetupStep.BIRTH_DATE, birthDay = "01", birthMonth = "01", birthYear = "2026")
        assertTrue(maxYear.isDobValid)

        val underMin = PatientDeviceSetupUiState(step = PatientSetupStep.BIRTH_DATE, birthDay = "01", birthMonth = "01", birthYear = "1899")
        assertFalse(underMin.isDobValid)

        val overMax = PatientDeviceSetupUiState(step = PatientSetupStep.BIRTH_DATE, birthDay = "01", birthMonth = "01", birthYear = "2027")
        assertFalse(overMax.isDobValid)
    }
}
