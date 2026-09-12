package com.example.cognicare.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PatternRecallEngineTest {

    private fun newEngine() = PatternRecallEngine(Random(42)).also { it.startGame() }

    private fun PatternRecallEngine.playRoundCorrectly(): TapOutcome {
        beginInput()
        var outcome = TapOutcome.IGNORED
        state.sequence.forEach { outcome = tap(it) }
        return outcome
    }

    private fun wrongColorFor(expected: PatternColor) = PatternColor.entries.first { it != expected }

    @Test
    fun tapsAreIgnoredWhileSequenceIsShowing() {
        val engine = newEngine()
        assertEquals(TapOutcome.IGNORED, engine.tap(engine.state.sequence.first()))
        assertEquals(PatternPhase.SHOWING, engine.state.phase)
    }

    @Test
    fun extraTapDuringRoundCompletePauseIsIgnored() {
        val engine = newEngine()
        assertEquals(TapOutcome.ROUND_COMPLETE, engine.playRoundCorrectly())
        // This stray tap used to start a second, overlapping playback.
        assertEquals(TapOutcome.IGNORED, engine.tap(PatternColor.RED))
        assertEquals(PatternPhase.ROUND_COMPLETE, engine.state.phase)
    }

    @Test
    fun nextRoundKeepsSequenceAndAddsOneColor() {
        val engine = newEngine()
        val firstSequence = engine.state.sequence
        engine.playRoundCorrectly()
        engine.nextRound()
        assertEquals(2, engine.state.round)
        assertEquals(firstSequence.size + 1, engine.state.sequence.size)
        assertEquals(firstSequence, engine.state.sequence.take(firstSequence.size))
        assertEquals(PatternPhase.SHOWING, engine.state.phase)
    }

    @Test
    fun wrongTapReplaysSameRound() {
        val engine = newEngine()
        engine.beginInput()
        val sequence = engine.state.sequence
        assertEquals(TapOutcome.MISTAKE, engine.tap(wrongColorFor(sequence.first())))
        assertEquals(TapOutcome.IGNORED, engine.tap(sequence.first()))
        engine.replayRound()
        assertEquals(PatternPhase.SHOWING, engine.state.phase)
        assertEquals(sequence, engine.state.sequence)
        assertTrue(engine.state.playerInput.isEmpty())
    }

    @Test
    fun repeatedMistakesEndTheGameGently() {
        val engine = newEngine()
        val wrong = wrongColorFor(engine.state.sequence.first())
        repeat(PatternRecallUiState.MAX_MISTAKES_PER_ROUND - 1) {
            engine.beginInput()
            assertEquals(TapOutcome.MISTAKE, engine.tap(wrong))
            engine.replayRound()
        }
        engine.beginInput()
        assertEquals(TapOutcome.GAME_COMPLETE, engine.tap(wrong))
        assertTrue(engine.state.isComplete)
    }

    @Test
    fun finishingTheLastRoundCompletesTheGame() {
        val engine = newEngine()
        repeat(PatternRecallUiState.WIN_ROUND - 1) {
            assertEquals(TapOutcome.ROUND_COMPLETE, engine.playRoundCorrectly())
            engine.nextRound()
        }
        assertEquals(TapOutcome.GAME_COMPLETE, engine.playRoundCorrectly())
        assertTrue(engine.state.isComplete)
        assertEquals(PatternRecallUiState.WIN_ROUND, engine.state.roundsCompleted)
    }
}
