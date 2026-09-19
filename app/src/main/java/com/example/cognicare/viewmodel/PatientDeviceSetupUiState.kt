package com.example.cognicare.viewmodel

import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.repository.AuthFailure

enum class PatientSetupStep {
    WELCOME,
    NAME,
    BIRTH_DATE,
    GENDER,
    LANGUAGE,
    CONSENT,
    SUCCESS
}

const val MIN_BIRTH_YEAR = 1900
const val MAX_BIRTH_YEAR = 2026

fun maxDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

data class PatientDeviceSetupUiState(
    val step: PatientSetupStep = PatientSetupStep.WELCOME,
    val patientName: String = "",
    val birthDay: String = "15",
    val birthMonth: String = "08",
    val birthYear: String = "1952",
    val gender: String? = null,
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH,
    val consentAccepted: Boolean = false,
    val patientPublicId: String? = null,
    val pendingSession: AuthSession? = null,
    val isCaregiverSetupMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: AuthFailure? = null,
    val dobError: Boolean = false
) {
    val selectedDay: Int
        get() = birthDay.toIntOrNull() ?: 15

    val selectedMonth: Int
        get() = birthMonth.toIntOrNull() ?: 8

    val selectedYear: Int
        get() = birthYear.toIntOrNull() ?: 1952

    val isDobValid: Boolean
        get() {
            val d = birthDay.toIntOrNull() ?: return false
            val m = birthMonth.toIntOrNull() ?: return false
            val y = birthYear.toIntOrNull() ?: return false
            if (y !in MIN_BIRTH_YEAR..MAX_BIRTH_YEAR) return false
            if (m !in 1..12) return false
            if (d !in 1..maxDaysInMonth(m, y)) return false
            return true
        }

    val formattedDobIso: String?
        get() {
            val d = birthDay.toIntOrNull() ?: return null
            val m = birthMonth.toIntOrNull() ?: return null
            val y = birthYear.toIntOrNull() ?: return null
            if (!isDobValid) return null
            return "%04d-%02d-%02d".format(y, m, d)
        }

    val canProceed: Boolean
        get() = when (step) {
            PatientSetupStep.WELCOME -> !isSubmitting
            PatientSetupStep.NAME -> patientName.trim().isNotBlank() && !isSubmitting
            PatientSetupStep.BIRTH_DATE -> isDobValid && !isSubmitting
            PatientSetupStep.GENDER -> gender != null && !isSubmitting
            PatientSetupStep.LANGUAGE -> selectedLanguage.isTranslated && !isSubmitting
            PatientSetupStep.CONSENT -> consentAccepted && !isSubmitting
            PatientSetupStep.SUCCESS -> pendingSession != null && !isSubmitting
        }

    val canSubmitCaregiver: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}
