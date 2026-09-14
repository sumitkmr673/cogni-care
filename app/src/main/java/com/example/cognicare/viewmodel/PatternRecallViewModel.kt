package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class PatternColor { RED, GREEN, BLUE, YELLOW }

enum class PatternPhase {
    /** The sequence is playing; the board is locked. */
    SHOWING,

    /** The patient's turn. The only phase that accepts taps. */
    INPUT,

    /** Brief pause after a correct round; the board is locked. */
    ROUND_COMPLETE,

    /** Brief pause after a wrong tap before the same round replays; the board is locked. */
    MISTAKE,

    COMPLETE
}

enum class TapOutcome { IGNORED, CORRECT, ROUND_COMPLETE, MISTAKE, GAME_COMPLETE }

data class PatternRecallUiState(
    val sequence: List<PatternColor> = emptyList(),
    val playerInput: List<PatternColor> = emptyList(),
    val phase: PatternPhase = PatternPhase.SHOWING,
    val highlightedColor: PatternColor? = null,
    val round: Int = 1,
    val mistakesThisRound: Int = 0,
    val roundsCompleted: Int = 0
) {
    val isComplete: Boolean get() = phase == PatternPhase.COMPLETE

    companion object {
        const val WIN_ROUND = 5

        /** Ending gently after repeated misses avoids an endless, discouraging retry loop. */
        const val MAX_MISTAKES_PER_ROUND = 3
    }
}

/**
 * Simon-says rules with no timing or Android dependencies, so every transition is testable.
 * Taps outside [PatternPhase.INPUT] are ignored, which is what keeps stray taps during
 * playback or pauses from corrupting a round.
 */
class PatternRecallEngine(private val random: Random = Random.Default) {

    var state: PatternRecallUiState = PatternRecallUiState()
        private set

    fun startGame(): PatternRecallUiState {
        state = PatternRecallUiState(sequence = listOf(randomColor()))
        return state
    }

    fun highlight(color: PatternColor?) {
        if (state.phase == PatternPhase.SHOWING) state = state.copy(highlightedColor = color)
    }

    fun beginInput() {
        if (state.phase == PatternPhase.SHOWING) {
            state = state.copy(phase = PatternPhase.INPUT, playerInput = emptyList(), highlightedColor = null)
        }
    }

    fun tap(color: PatternColor): TapOutcome {
        val current = state
        if (current.phase != PatternPhase.INPUT) return TapOutcome.IGNORED

        if (current.sequence.getOrNull(current.playerInput.size) != color) {
            val mistakes = current.mistakesThisRound + 1
            return if (mistakes >= PatternRecallUiState.MAX_MISTAKES_PER_ROUND) {
                state = current.copy(phase = PatternPhase.COMPLETE, playerInput = emptyList(), mistakesThisRound = mistakes)
                TapOutcome.GAME_COMPLETE
            } else {
                state = current.copy(phase = PatternPhase.MISTAKE, playerInput = emptyList(), mistakesThisRound = mistakes)
                TapOutcome.MISTAKE
            }
        }

        val input = current.playerInput + color
        if (input.size < current.sequence.size) {
            state = current.copy(playerInput = input)
            return TapOutcome.CORRECT
        }

        val roundsCompleted = current.roundsCompleted + 1
        return if (current.round >= PatternRecallUiState.WIN_ROUND) {
            state = current.copy(playerInput = input, phase = PatternPhase.COMPLETE, roundsCompleted = roundsCompleted)
            TapOutcome.GAME_COMPLETE
        } else {
            state = current.copy(playerInput = input, phase = PatternPhase.ROUND_COMPLETE, roundsCompleted = roundsCompleted)
            TapOutcome.ROUND_COMPLETE
        }
    }

    fun nextRound() {
        val current = state
        if (current.phase != PatternPhase.ROUND_COMPLETE) return
        state = current.copy(
            round = current.round + 1,
            sequence = current.sequence + randomColor(),
            playerInput = emptyList(),
            phase = PatternPhase.SHOWING,
            mistakesThisRound = 0
        )
    }

    fun replayRound() {
        if (state.phase == PatternPhase.MISTAKE) {
            state = state.copy(phase = PatternPhase.SHOWING, playerInput = emptyList())
        }
    }

    private fun randomColor(): PatternColor = PatternColor.entries[random.nextInt(PatternColor.entries.size)]
}

@HiltViewModel
class PatternRecallViewModel @Inject constructor() : ViewModel() {

    private val engine = PatternRecallEngine()

    private val _uiState = MutableStateFlow(engine.startGame())
    val uiState: StateFlow<PatternRecallUiState> = _uiState.asStateFlow()

    /** The single playback/pause job; replaced (never duplicated) whenever a new one starts. */
    private var sequenceJob: Job? = null

    init {
        runSequence(pauseMs = 0L)
    }

    fun onColorTap(color: PatternColor) {
        when (engine.tap(color)) {
            TapOutcome.IGNORED -> Unit
            TapOutcome.CORRECT, TapOutcome.GAME_COMPLETE -> publish()
            TapOutcome.ROUND_COMPLETE -> {
                publish()
                runSequence(ROUND_COMPLETE_PAUSE_MS) { engine.nextRound() }
            }
            TapOutcome.MISTAKE -> {
                publish()
                runSequence(MISTAKE_PAUSE_MS) { engine.replayRound() }
            }
        }
    }

    private fun runSequence(pauseMs: Long, prepare: () -> Unit = {}) {
        sequenceJob?.cancel()
        sequenceJob = viewModelScope.launch {
            delay(pauseMs)
            prepare()
            publish()
            delay(START_DELAY_MS)
            for (color in engine.state.sequence) {
                engine.highlight(color)
                publish()
                delay(HIGHLIGHT_MS)
                engine.highlight(null)
                publish()
                delay(GAP_MS)
            }
            engine.beginInput()
            publish()
        }
    }

    private fun publish() {
        _uiState.value = engine.state
    }

    private companion object {
        const val START_DELAY_MS = 500L
        const val HIGHLIGHT_MS = 650L
        const val GAP_MS = 300L
        const val ROUND_COMPLETE_PAUSE_MS = 900L
        const val MISTAKE_PAUSE_MS = 1_400L
    }
}
