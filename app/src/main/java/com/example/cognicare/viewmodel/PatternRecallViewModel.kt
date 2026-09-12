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
import kotlin.random.Random

enum class PatternColor { RED, GREEN, BLUE, YELLOW }

enum class PatternPhase { SHOWING, INPUT, MISTAKE }

data class PatternRecallUiState(
    val sequence: List<PatternColor> = emptyList(),
    val playerInput: List<PatternColor> = emptyList(),
    val phase: PatternPhase = PatternPhase.SHOWING,
    val highlightedIndex: Int = -1,
    val round: Int = 1
) {
    val isSolved: Boolean get() = round > WIN_ROUND

    companion object {
        const val WIN_ROUND = 5
    }
}

/** Simon-says: the sequence grows by one colour each round; five rounds completes the game. */
@HiltViewModel
class PatternRecallViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PatternRecallUiState())
    val uiState: StateFlow<PatternRecallUiState> = _uiState.asStateFlow()

    init {
        startRound(sequence = listOf(randomColor()))
    }

    private fun startRound(sequence: List<PatternColor>) {
        _uiState.value = _uiState.value.copy(
            sequence = sequence,
            playerInput = emptyList(),
            phase = PatternPhase.SHOWING,
            highlightedIndex = -1
        )
        playbackSequence(sequence)
    }

    private fun playbackSequence(sequence: List<PatternColor>) {
        viewModelScope.launch {
            delay(START_DELAY_MS)
            sequence.indices.forEach { index ->
                _uiState.update { it.copy(highlightedIndex = index) }
                delay(HIGHLIGHT_MS)
                _uiState.update { it.copy(highlightedIndex = -1) }
                delay(GAP_MS)
            }
            _uiState.update { it.copy(phase = PatternPhase.INPUT) }
        }
    }

    fun onColorTap(color: PatternColor) {
        val state = _uiState.value
        if (state.phase != PatternPhase.INPUT) return

        val nextInput = state.playerInput + color
        val expected = state.sequence.getOrNull(nextInput.lastIndex)
        if (expected != color) {
            showMistakeThenRetry(state.sequence)
            return
        }

        _uiState.update { it.copy(playerInput = nextInput) }

        if (nextInput.size == state.sequence.size) {
            advanceRound(state)
        }
    }

    private fun advanceRound(state: PatternRecallUiState) {
        val nextRoundNumber = state.round + 1
        if (nextRoundNumber > PatternRecallUiState.WIN_ROUND) {
            _uiState.update { it.copy(round = nextRoundNumber, phase = PatternPhase.INPUT) }
            return
        }
        viewModelScope.launch {
            delay(ROUND_COMPLETE_PAUSE_MS)
            _uiState.update { it.copy(round = nextRoundNumber) }
            startRound(state.sequence + randomColor())
        }
    }

    private fun showMistakeThenRetry(sequence: List<PatternColor>) {
        _uiState.update { it.copy(phase = PatternPhase.MISTAKE) }
        viewModelScope.launch {
            delay(MISTAKE_PAUSE_MS)
            startRound(sequence)
        }
    }

    private fun randomColor(): PatternColor = PatternColor.entries[Random.nextInt(PatternColor.entries.size)]

    private companion object {
        const val START_DELAY_MS = 500L
        const val HIGHLIGHT_MS = 550L
        const val GAP_MS = 250L
        const val ROUND_COMPLETE_PAUSE_MS = 700L
        const val MISTAKE_PAUSE_MS = 1200L
    }
}
