package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val totalPairs: Int = 0
) {
    val isSolved: Boolean get() = totalPairs > 0 && matchedPairs == totalPairs
}

/** Six pairs on a 3-column grid: a gentle 4x3 board, easy to scan for low vision. */
@HiltViewModel
class MemoryMatchViewModel @Inject constructor() : ViewModel() {

    private val emojis = listOf("🌸", "🐘", "🍎", "🌙", "☀️", "🐦")

    private val _uiState = MutableStateFlow(MemoryMatchUiState())
    val uiState: StateFlow<MemoryMatchUiState> = _uiState.asStateFlow()

    private var isResolving = false

    init {
        startNewBoard()
    }

    private fun startNewBoard() {
        val deck = (emojis + emojis)
            .shuffled()
            .mapIndexed { index, emoji -> MemoryCard(id = index, emoji = emoji) }
        _uiState.value = MemoryMatchUiState(cards = deck, matchedPairs = 0, totalPairs = emojis.size)
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
            delay(if (first.emoji == second.emoji) MATCH_PAUSE_MS else MISMATCH_PAUSE_MS)
            _uiState.update { current ->
                val matched = first.emoji == second.emoji
                current.copy(
                    cards = current.cards.map { card ->
                        when {
                            card.id != first.id && card.id != second.id -> card
                            matched -> card.copy(isMatched = true, isFaceUp = true)
                            else -> card.copy(isFaceUp = false)
                        }
                    },
                    matchedPairs = if (matched) current.matchedPairs + 1 else current.matchedPairs
                )
            }
            isResolving = false
        }
    }

    private companion object {
        const val MATCH_PAUSE_MS = 500L
        const val MISMATCH_PAUSE_MS = 900L
    }
}
