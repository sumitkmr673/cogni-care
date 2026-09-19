package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.game.MAX_GAME_LEVEL
import com.example.cognicare.core.game.MIN_GAME_LEVEL
import com.example.cognicare.core.game.LevelResult
import com.example.cognicare.core.game.levelResultAfter
import com.example.cognicare.core.game.memoryMatchLevel
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MemoryCard(
    val id: Int,
    val emoji: String,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

data class MemoryMatchUiState(
    val cards: List<MemoryCard> = emptyList(),
    val matchedPairs: Int = 0,
    val totalPairs: Int = 0,
    val mismatchedAttempts: Int = 0,
    val level: Int = MIN_GAME_LEVEL,
    val maxLevel: Int = MAX_GAME_LEVEL,
    /** True once the board is dealt; until then the screen has nothing to show. */
    val isReady: Boolean = false
) {
    val isSolved: Boolean get() = totalPairs > 0 && matchedPairs == totalPairs

    /** A clean enough board moves the patient up. The allowance is generous on purpose:
     *  one wrong turn per pair still counts as clearing the level. */
    val clearedLevel: Boolean get() = isSolved && mismatchedAttempts <= totalPairs

    val levelResult: LevelResult get() = levelResultAfter(level, clearedLevel)
}

/**
 * Board size and the mismatch pause both come from the patient's current level — see
 * [memoryMatchLevel]. Cards lay out in rows of three on screen, so pair counts are chosen to
 * fill those rows reasonably rather than to grow smoothly.
 */
@HiltViewModel
class MemoryMatchViewModel @Inject constructor(
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    // Needs at least as many entries as the largest board (9 pairs at level 10). Chosen to be
    // recognisable at a glance and culturally neutral for patients in the North East.
    private val emojis = listOf("🌸", "🐘", "🍎", "🌙", "☀️", "🐦", "🐟", "🍌", "🏠", "🍵")

    private val _uiState = MutableStateFlow(MemoryMatchUiState())
    val uiState: StateFlow<MemoryMatchUiState> = _uiState.asStateFlow()

    private var isResolving = false
    private var startedAtMillis = System.currentTimeMillis()
    private var hasReportedCompletion = false
    private var mismatchPauseMs = memoryMatchLevel(MIN_GAME_LEVEL).mismatchPauseMs

    init {
        viewModelScope.launch {
            startNewBoard(preferences.currentGameLevel(GameType.MEMORY_MATCH))
        }
    }

    private fun startNewBoard(level: Int) {
        val tuning = memoryMatchLevel(level)
        mismatchPauseMs = tuning.mismatchPauseMs
        val faces = emojis.shuffled().take(tuning.pairs)
        val deck = (faces + faces)
            .shuffled()
            .mapIndexed { index, emoji -> MemoryCard(id = index, emoji = emoji) }
        startedAtMillis = System.currentTimeMillis()
        _uiState.value = MemoryMatchUiState(
            cards = deck,
            matchedPairs = 0,
            totalPairs = tuning.pairs,
            level = tuning.level,
            isReady = true
        )
    }

    fun onCardClick(cardId: Int) {
        if (isResolving) return
        val state = _uiState.value
        val clicked = state.cards.firstOrNull { it.id == cardId } ?: return
        if (clicked.isFaceUp || clicked.isMatched) return

        val faceUpUnmatched = state.cards.filter { it.isFaceUp && !it.isMatched }
        if (faceUpUnmatched.size >= 2) return

        _uiState.update { current ->
            current.copy(cards = current.cards.map { if (it.id == cardId) it.copy(isFaceUp = true) else it })
        }

        val nowFaceUp = _uiState.value.cards.filter { it.isFaceUp && !it.isMatched }
        if (nowFaceUp.size == 2) {
            resolvePair(nowFaceUp[0], nowFaceUp[1])
        }
    }

    private fun resolvePair(first: MemoryCard, second: MemoryCard) {
        isResolving = true
        viewModelScope.launch {
            val matched = first.emoji == second.emoji
            delay(if (matched) MATCH_PAUSE_MS else mismatchPauseMs)
            var resolved = _uiState.value
            _uiState.update { current ->
                current.copy(
                    cards = current.cards.map { card ->
                        when {
                            card.id != first.id && card.id != second.id -> card
                            matched -> card.copy(isMatched = true, isFaceUp = true)
                            else -> card.copy(isFaceUp = false)
                        }
                    },
                    matchedPairs = if (matched) current.matchedPairs + 1 else current.matchedPairs,
                    mismatchedAttempts = if (matched) current.mismatchedAttempts else current.mismatchedAttempts + 1
                ).also { resolved = it }
            }
            isResolving = false
            if (resolved.isSolved) reportCompletionOnce(resolved)
        }
    }

    private fun reportCompletionOnce(finished: MemoryMatchUiState) {
        if (hasReportedCompletion) return
        hasReportedCompletion = true
        viewModelScope.launch {
            // The write outlives this scope: the screen navigates away the moment the board is
            // solved, which would otherwise cancel the level being saved.
            withContext(NonCancellable) {
                preferences.setGameLevel(GameType.MEMORY_MATCH, finished.levelResult.nextLevel)
            }

            if (authRepository.session.first() == null) return@launch
            val elapsedMs = System.currentTimeMillis() - startedAtMillis
            val attempts = finished.totalPairs + finished.mismatchedAttempts
            careRepository.recordGameCompletion(
                GameType.MEMORY_MATCH,
                GameOutcome(
                    scorePercent = finished.totalPairs * 100.0 / attempts,
                    accuracyPercent = finished.totalPairs * 100.0 / attempts,
                    correctAnswers = finished.totalPairs,
                    totalQuestions = attempts,
                    responseTimeMs = elapsedMs.toInt(),
                    mistakes = finished.mismatchedAttempts,
                    difficultyLevel = finished.level
                )
            )
        }
    }

    private companion object {
        const val MATCH_PAUSE_MS = 500L
    }
}
