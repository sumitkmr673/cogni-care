package com.example.cognicare.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLevelsTest {

    private val allLevels = MIN_GAME_LEVEL..MAX_GAME_LEVEL

    @Test
    fun thereAreTenLevels() {
        assertEquals(1, MIN_GAME_LEVEL)
        assertEquals(10, MAX_GAME_LEVEL)
    }

    @Test
    fun everyLevelIsDefinedForBothGames() {
        allLevels.forEach { level ->
            assertEquals(level, memoryMatchLevel(level).level)
            assertEquals(level, patternRecallLevel(level).level)
        }
    }

    @Test
    fun boardsNeverNeedMoreFacesThanTheEmojiPoolHas() {
        // MemoryMatchViewModel holds 10 faces; a board asking for more would deal duplicate pairs.
        assertTrue(allLevels.all { memoryMatchLevel(it).pairs <= 10 })
    }

    @Test
    fun difficultyNeverEasesOffAsLevelsRise() {
        allLevels.zipWithNext { lower, higher ->
            val a = memoryMatchLevel(lower)
            val b = memoryMatchLevel(higher)
            assertTrue("pairs dropped between $lower and $higher", b.pairs >= a.pairs)
            assertTrue("pause grew between $lower and $higher", b.mismatchPauseMs <= a.mismatchPauseMs)

            val p = patternRecallLevel(lower)
            val q = patternRecallLevel(higher)
            assertTrue("rounds dropped between $lower and $higher", q.winRound >= p.winRound)
            assertTrue("highlight slowed between $lower and $higher", q.highlightMs <= p.highlightMs)
        }
    }

    @Test
    fun evenTheHardestLevelStaysGentle() {
        // Guards the tuning against a future edit that makes level 10 punishing for a patient
        // living with dementia: the sequence stays short and every cue stays clearly visible.
        val hardestPattern = patternRecallLevel(MAX_GAME_LEVEL)
        assertTrue(hardestPattern.winRound <= 8)
        assertTrue(hardestPattern.highlightMs >= 400L)
        assertTrue(memoryMatchLevel(MAX_GAME_LEVEL).mismatchPauseMs >= 700L)
    }

    @Test
    fun outOfRangeLevelsAreClampedNotRejected() {
        assertEquals(MIN_GAME_LEVEL, clampLevel(0))
        assertEquals(MIN_GAME_LEVEL, clampLevel(-7))
        assertEquals(MAX_GAME_LEVEL, clampLevel(99))
        assertEquals(memoryMatchLevel(MIN_GAME_LEVEL), memoryMatchLevel(0))
        assertEquals(patternRecallLevel(MAX_GAME_LEVEL), patternRecallLevel(99))
    }

    @Test
    fun clearingMovesUpAndFailingRepeatsTheSameLevel() {
        assertEquals(2, nextLevelAfter(1, cleared = true))
        assertEquals(1, nextLevelAfter(1, cleared = false))
        assertEquals(5, nextLevelAfter(5, cleared = false))
    }

    @Test
    fun theLastLevelIsAPlateauNotAWall() {
        assertEquals(MAX_GAME_LEVEL, nextLevelAfter(MAX_GAME_LEVEL, cleared = true))
        assertEquals(MAX_GAME_LEVEL, nextLevelAfter(MAX_GAME_LEVEL, cleared = false))
    }

    @Test
    fun aPatientNeverDropsALevel() {
        allLevels.forEach { level ->
            assertTrue(nextLevelAfter(level, cleared = false) >= level)
            assertTrue(nextLevelAfter(level, cleared = true) >= level)
        }
    }
}
