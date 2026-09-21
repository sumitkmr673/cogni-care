package com.example.cognicare.core.game

import com.example.cognicare.data.model.GameOutcome

/**
 * What a finished Pattern Recall game reports to the backend, where it counts toward the Memory
 * score (PATTERN_RECALL is a MEMORY game).
 *
 * A wrong tap replays the round, so every round takes one or more attempts: a completed round is a
 * correct attempt and each mistake a wrong one. That mirrors Memory Match, and it is what makes
 * the caregiver's accuracy tell a clean run from a struggling one — finishing every round after
 * many mistakes is not the same as finishing them first time.
 *
 * - `score` is progress through the level (rounds completed of rounds needed), so it still shows
 *   how far a patient got when they ran out of attempts.
 * - `accuracy`, `correct_answers` and `total_questions` are attempts: they always agree with each
 *   other (correct / total = accuracy) and correct never exceeds total, as the backend requires.
 * - `response_time_ms` is the average time per attempt. It includes the moments the pattern is
 *   shown, which the game controls, so it is a pace indicator rather than a reaction time.
 */
fun patternRecallOutcome(
    roundsCompleted: Int,
    roundsNeeded: Int,
    totalMistakes: Int,
    elapsedMs: Long,
    level: Int
): GameOutcome {
    val attempts = (roundsCompleted + totalMistakes).coerceAtLeast(1)
    return GameOutcome(
        scorePercent = roundsCompleted * 100.0 / roundsNeeded.coerceAtLeast(1),
        accuracyPercent = roundsCompleted * 100.0 / attempts,
        correctAnswers = roundsCompleted,
        totalQuestions = attempts,
        responseTimeMs = (elapsedMs.coerceAtLeast(0) / attempts).toInt(),
        mistakes = totalMistakes,
        difficultyLevel = level
    )
}
