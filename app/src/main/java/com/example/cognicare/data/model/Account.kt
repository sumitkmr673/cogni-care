package com.example.cognicare.data.model

/** Two audiences: the patient, and the caregiver. Doctor is a caregiver designation. */
enum class UserRole {
    PATIENT,
    CAREGIVER;

    val isCaregiver: Boolean get() = this == CAREGIVER
}

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val languagePref: String,
    val linkedPatientIds: List<String> = emptyList(),
    val publicId: String? = null,
    val patientId: String? = null
)

data class AuthSession(
    val userId: String,
    val displayName: String,
    val role: UserRole,
    val accessToken: String,
    val linkedPatientIds: List<String> = emptyList(),
    val publicId: String? = null,
    val patientId: String? = null
)
