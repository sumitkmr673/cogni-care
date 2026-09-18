package com.example.cognicare.data.model

/**
 * What a finished game reports to the backend (POST /games/sessions/{id}/result). The patient
 * never sees any of this — only the "Great job today!" screen — it exists purely for the
 * caregiver dashboard's trends.
 */
data class GameOutcome(
    val scorePercent: Double,
    val accuracyPercent: Double? = null,
    val correctAnswers: Int? = null,
    val totalQuestions: Int? = null,
    val responseTimeMs: Int? = null,
    val mistakes: Int? = null,
    val difficultyLevel: Int = 1
)
