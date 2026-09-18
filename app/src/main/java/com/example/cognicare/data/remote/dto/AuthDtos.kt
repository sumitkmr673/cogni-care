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

@Serializable
data class AuthenticatedUserDto(
    val id: String,
    val email: String? = null,
    val display_name: String,
    val role: String,
    val is_active: Boolean
)
