package com.example.cognicare.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors voice_service/app/schemas.py (the app-only voice service, not the shared backend).

@Serializable
data class VoiceOptionDto(val id: String, val label: String)

@Serializable
data class VoiceContextDto(
    val language: String?,
    val question: String?,
    val options: List<VoiceOptionDto>
)

@Serializable
data class InterpretTextRequestDto(val transcript: String, val context: VoiceContextDto)

@Serializable
data class InterpretResponseDto(
    val transcript: String,
    /** One of the ids sent, or null when the answer was unclear or contradicted itself. */
    val choice: String? = null,
    val language: String? = null,
    val timings_ms: Map<String, Int> = emptyMap()
)

@Serializable
data class TranscribeResponseDto(
    val transcript: String,
    /** The language Whisper heard (it may differ from the app's for mixed speech). */
    val language: String? = null,
    val timings_ms: Map<String, Int> = emptyMap()
)
