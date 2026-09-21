package com.example.cognicare.core.game

import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.backendCodeToGameType
import com.example.cognicare.repository.toBackendCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternRecallOutcomeTest {

    @Test
    fun `a clean run is fully accurate`() {
        val outcome = patternRecallOutcome(roundsCompleted = 5, roundsNeeded = 5, totalMistakes = 0, elapsedMs = 50_000, level = 2)
        assertEquals(100.0, outcome.scorePercent, 0.001)
        assertEquals(100.0, outcome.accuracyPercent!!, 0.001)
        assertEquals(5, outcome.correctAnswers)
        assertEquals(5, outcome.totalQuestions)
        assertEquals(10_000, outcome.responseTimeMs)
        assertEquals(2, outcome.difficultyLevel)
    }

    @Test
    fun `finishing every round after mistakes still shows the struggle`() {
        val outcome = patternRecallOutcome(roundsCompleted = 5, roundsNeeded = 5, totalMistakes = 5, elapsedMs = 100_000, level = 1)
        assertEquals("progress is complete", 100.0, outcome.scorePercent, 0.001)
        assertEquals("but half the attempts were wrong", 50.0, outcome.accuracyPercent!!, 0.001)
        assertEquals(5, outcome.mistakes)
    }

    @Test
    fun `running out of attempts reports how far they got`() {
        val outcome = patternRecallOutcome(roundsCompleted = 2, roundsNeeded = 5, totalMistakes = 3, elapsedMs = 40_000, level = 1)
        assertEquals(40.0, outcome.scorePercent, 0.001)
        assertEquals(40.0, outcome.accuracyPercent!!, 0.001)
    }

    @Test
    fun `the numbers always satisfy the backend's rules`() {
        // schemas/gameplay.py: accuracy 0..100, correct_answers <= total_questions, total_questions > 0.
        for (rounds in 0..6) for (mistakes in 0..8) {
            val o = patternRecallOutcome(rounds, roundsNeeded = 5, totalMistakes = mistakes, elapsedMs = 30_000, level = 1)
            assertTrue(o.accuracyPercent!! in 0.0..100.0)
            assertTrue(o.correctAnswers!! <= o.totalQuestions!!)
            assertTrue(o.totalQuestions!! > 0)
            assertTrue(o.responseTimeMs!! >= 0)
        }
    }

    @Test
    fun `accuracy is correct over total, as on every other game`() {
        val o = patternRecallOutcome(roundsCompleted = 3, roundsNeeded = 5, totalMistakes = 1, elapsedMs = 8_000, level = 1)
        assertEquals(o.correctAnswers!! * 100.0 / o.totalQuestions!!, o.accuracyPercent!!, 0.001)
    }

    @Test
    fun `pattern recall reports to the backend rather than staying on the device`() {
        assertEquals("PATTERN_RECALL", GameType.PATTERN_RECALL.toBackendCode())
        assertEquals(GameType.PATTERN_RECALL, backendCodeToGameType("PATTERN_RECALL"))
        // Every game the app has now has a backend counterpart.
        GameType.entries.forEach { assertNotNull("$it", it.toBackendCode()) }
    }
}
