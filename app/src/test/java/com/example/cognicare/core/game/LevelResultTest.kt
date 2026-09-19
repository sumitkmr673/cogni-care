package com.example.cognicare.core.game

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelResultTest {

    @Test
    fun clearingALevelOffersTheNextOne() {
        assertEquals(LevelResult(nextLevel = 4, leveledUp = true), levelResultAfter(3, cleared = true))
    }

    @Test
    fun notClearingOffersTheSameLevelAgainWithoutClaimingALevelUp() {
        assertEquals(LevelResult(nextLevel = 3, leveledUp = false), levelResultAfter(3, cleared = false))
    }

    @Test
    fun clearingTheTopLevelIsNotALevelUp() {
        // "Play level 11" must never appear; the patient replays level 10.
        assertEquals(LevelResult(nextLevel = MAX_GAME_LEVEL, leveledUp = false), levelResultAfter(MAX_GAME_LEVEL, cleared = true))
    }

    @Test
    fun theButtonAndTheSavedLevelAlwaysAgree() {
        (MIN_GAME_LEVEL..MAX_GAME_LEVEL).forEach { level ->
            listOf(true, false).forEach { cleared ->
                assertEquals(nextLevelAfter(level, cleared), levelResultAfter(level, cleared).nextLevel)
            }
        }
    }
}
