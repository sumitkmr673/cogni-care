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
    val is_active: Boolean,
    val caregiver_id: String? = null,
    val patient_id: String? = null,
    val public_id: String? = null,
    val caregiver_type: String? = null,
    val phone: String? = null
)

@Serializable
data class DeviceLoginRequestDto(
    val device_identifier: String,
    val device_key: String
)

@Serializable
data class DeviceProvisionRequestDto(
    val device_name: String? = null,
    val client_device_id: String? = null
)

@Serializable
data class DeviceProvisionResponseDto(
    val device_id: String,
    val device_identifier: String,
    val device_key: String,
    val patient_id: String,
    val patient_public_id: String,
    val display_name: String,
    val status: String,
    val created_at: String
)

@Serializable
data class PatientRegisterRequestDto(
    val display_name: String,
    val date_of_birth: String? = null,
    val gender: String? = null,
    val preferred_language: String? = null,
    val client_device_id: String? = null,
    val device_name: String? = null
)

@Serializable
data class PatientLoginByIdRequestDto(
    val public_id: String,
    val client_device_id: String? = null,
    val device_name: String? = null
)

@Serializable
data class PatientRegisterResponseDto(
    val patient_id: String,
    val patient_public_id: String,
    val display_name: String,
    val device_identifier: String,
    val device_key: String,
    val token: TokenResponseDto
)

@Serializable
data class DeviceRevokeResponseDto(
    val device_identifier: String,
    val status: String,
    val revoked_at: String
)
