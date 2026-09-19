package com.example.cognicare.core.game

/**
 * Per-level tuning for the two touch-only games, kept as plain data so the curve can be read,
 * reviewed and tested without running a game.
 *
 * The ramp is deliberately gentle. These are elderly patients living with dementia: a level is
 * meant to feel slightly more interesting than the last, never like a test being failed. Board
 * size grows slowly, and the pauses that help a patient remember shorten only a little, so even
 * level 10 keeps more thinking time than a typical puzzle game would give.
 */

const val MIN_GAME_LEVEL = 1
const val MAX_GAME_LEVEL = 10

data class MemoryMatchLevel(
    val level: Int,
    val pairs: Int,
    /** How long a mismatched pair stays visible — the main memory aid, so it shrinks slowly. */
    val mismatchPauseMs: Long
)

data class PatternRecallLevel(
    val level: Int,
    /** Rounds needed to finish; the sequence grows by one colour each round. */
    val winRound: Int,
    val highlightMs: Long,
    val gapMs: Long
)

private val memoryMatchPairs = listOf(3, 3, 4, 4, 5, 6, 6, 7, 8, 9)
private val memoryMatchPauses = listOf(1200L, 1200L, 1100L, 1100L, 1000L, 950L, 900L, 850L, 800L, 750L)

private val patternWinRounds = listOf(3, 3, 4, 4, 5, 5, 6, 6, 7, 8)
private val patternHighlights = listOf(800L, 760L, 720L, 680L, 650L, 620L, 580L, 550L, 520L, 480L)
private val patternGaps = listOf(420L, 400L, 380L, 360L, 340L, 320L, 300L, 290L, 280L, 260L)

/** Clamps out-of-range input rather than throwing: a stored level must never crash a game. */
fun clampLevel(level: Int): Int = level.coerceIn(MIN_GAME_LEVEL, MAX_GAME_LEVEL)

fun memoryMatchLevel(level: Int): MemoryMatchLevel {
    val safe = clampLevel(level)
    return MemoryMatchLevel(
        level = safe,
        pairs = memoryMatchPairs[safe - 1],
        mismatchPauseMs = memoryMatchPauses[safe - 1]
    )
}

fun patternRecallLevel(level: Int): PatternRecallLevel {
    val safe = clampLevel(level)
    return PatternRecallLevel(
        level = safe,
        winRound = patternWinRounds[safe - 1],
        highlightMs = patternHighlights[safe - 1],
        gapMs = patternGaps[safe - 1]
    )
}

/**
 * Where the patient goes next. Clearing moves up one; not clearing repeats the same level.
 * Nothing ever moves a patient *down* — losing ground is discouraging, and a bad day at the
 * current level says more about the day than about the patient's ability.
 */
fun nextLevelAfter(current: Int, cleared: Boolean): Int =
    if (cleared) clampLevel(current + 1) else clampLevel(current)

/** What a finished game means for the next one: the level to play, and whether it went up. */
data class LevelResult(val nextLevel: Int, val leveledUp: Boolean)

/**
 * The single source for both the saved level and the "Play level N / Play again" button, so the
 * button can never promise a level the patient did not actually reach. At the top level, clearing
 * it again is not a level-up — the patient replays level 10.
 */
fun levelResultAfter(current: Int, cleared: Boolean): LevelResult {
    val next = nextLevelAfter(current, cleared)
    return LevelResult(nextLevel = next, leveledUp = next > clampLevel(current))
}
