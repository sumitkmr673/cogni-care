package com.example.cognicare.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/gameplay.py.

@Serializable
data class GameSummaryDto(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val description: String? = null
)

@Serializable
data class GamesResponseDto(
    val games: List<GameSummaryDto>
)

@Serializable
data class StartGameSessionRequestDto(
    val difficulty_level: Int = 1
)

@Serializable
data class GameSessionDto(
    val id: String,
    val game_id: String,
    val patient_id: String,
    val difficulty_level: Int,
    val status: String,
    val started_at: String,
    val completed_at: String? = null
)

@Serializable
data class SubmitGameResultRequestDto(
    val score: Double,
    val accuracy: Double? = null,
    val correct_answers: Int? = null,
    val total_questions: Int? = null,
    val response_time_ms: Int? = null,
    val mistakes: Int? = null
)

@Serializable
data class GameResultDto(
    val id: String,
    val session_id: String,
    val score: Double,
    val accuracy: Double? = null,
    val correct_answers: Int? = null,
    val total_questions: Int? = null,
    val response_time_ms: Int? = null,
    val mistakes: Int? = null,
    val session_status: String,
    val completed_at: String? = null
)
