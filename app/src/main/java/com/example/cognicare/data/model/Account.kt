package com.example.cognicare.data.model

/** Two audiences: the patient, and the doctor who acts as the patient's caregiver. */
enum class UserRole {
    PATIENT,
    DOCTOR;

    val isCaregiver: Boolean get() = this == DOCTOR
}

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val languagePref: String,
    val linkedPatientIds: List<String> = emptyList()
)

data class AuthSession(
    val userId: String,
    val displayName: String,
    val role: UserRole,
    val accessToken: String,
    val linkedPatientIds: List<String> = emptyList()
)
