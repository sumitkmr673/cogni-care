package com.example.cognicare.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/auth.py. snake_case matches the backend's JSON exactly, so no
// custom SerialName mapping is needed as long as these names stay in sync with that file.

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class TokenResponseDto(
    val access_token: String,
    val token_type: String = "bearer"
)

/** POST /auth/register. Creates a caregiver account; it does not sign in — the app logs in after. */
@Serializable
data class CaregiverRegisterRequestDto(
    val email: String,
    val password: String,
    val display_name: String,
    val caregiver_type: String,
    val phone: String? = null
)

@Serializable
data class CaregiverRegisterResponseDto(
    val id: String,
    val public_id: String? = null,
    val email: String,
    val display_name: String,
    val caregiver_type: String? = null
)

@Serializable
data class AuthenticatedUserDto(
    val id: String,
    val email: String? = null,
    val display_name: String,
    val role: String,
    val is_active: Boolean
)
