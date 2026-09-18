package com.example.cognicare.repository

import com.example.cognicare.data.model.GameType

/**
 * The backend's catalog (cogni-care/backend/app/scripts/seed_demo.py, GAME_DEFINITIONS) has five
 * games; the app has six. PATTERN_RECALL has no backend counterpart, so its results are kept on
 * the device only — [RemoteCareRepository.recordGameCompletion] skips the network call for it
 * rather than mislabel it under an unrelated backend game.
 */
private val gameTypeToBackendCode: Map<GameType, String> = mapOf(
    GameType.DAILY_RECALL to "DAILY_RECALL",
    GameType.FAMILY_IDENTIFICATION to "FAMILY_IDENTIFICATION",
    GameType.ORIENTATION to "ORIENTATION",
    GameType.OBJECT_NAMING to "OBJECT_IDENTIFICATION",
    GameType.MEMORY_MATCH to "OBJECT_MATCHING"
)

private val backendCodeToGameType: Map<String, GameType> =
    gameTypeToBackendCode.entries.associate { (gameType, code) -> code to gameType }

fun GameType.toBackendCode(): String? = gameTypeToBackendCode[this]

fun backendCodeToGameType(code: String): GameType? = backendCodeToGameType[code]

/** Highest `difficulty_level` the backend accepts — `le=3` in schemas/gameplay.py, and a
 *  `CHECK (difficulty_level IN (1, 2, 3))` on game_sessions. */
const val BACKEND_MAX_DIFFICULTY = 3

/**
 * The app has ten levels ([com.example.cognicare.core.game.MAX_GAME_LEVEL]); the backend has
 * three difficulty tiers. Sending a level above 3 is rejected with a 422, and because
 * [RemoteCareRepository.recordGameCompletion] only logs failures, every result from a patient
 * past level 3 would be lost without anyone noticing. So levels are banded here, at the one
 * place the app talks to the backend: 1–3 → 1, 4–7 → 2, 8–10 → 3.
 *
 * The caregiver dashboard therefore shows the tier, not the exact level. If the backend widens
 * its range to 10, this becomes `level.coerceIn(1, 10)` and nothing else changes.
 */
fun appLevelToBackendDifficulty(level: Int): Int = when {
    level <= 3 -> 1
    level <= 7 -> 2
    else -> BACKEND_MAX_DIFFICULTY
}
