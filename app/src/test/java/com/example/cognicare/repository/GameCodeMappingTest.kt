package com.example.cognicare.repository

import com.example.cognicare.core.game.MAX_GAME_LEVEL
import com.example.cognicare.core.game.MIN_GAME_LEVEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCodeMappingTest {

    @Test
    fun everyAppLevelMapsToADifficultyTheBackendAccepts() {
        // The backend rejects anything outside 1..3 with a 422, and the app only logs that —
        // so an out-of-range value here means results silently disappear.
        (MIN_GAME_LEVEL..MAX_GAME_LEVEL).forEach { level ->
            val tier = appLevelToBackendDifficulty(level)
            assertTrue("level $level -> $tier", tier in 1..BACKEND_MAX_DIFFICULTY)
        }
    }

    @Test
    fun levelsAreBandedIntoThreeTiers() {
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 2, 3, 3, 3), (1..10).map(::appLevelToBackendDifficulty))
    }

    @Test
    fun tiersNeverGoDownAsLevelsRise() {
        (MIN_GAME_LEVEL until MAX_GAME_LEVEL).forEach { level ->
            assertTrue(appLevelToBackendDifficulty(level + 1) >= appLevelToBackendDifficulty(level))
        }
    }
}
