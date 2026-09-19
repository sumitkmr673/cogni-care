package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.game.MAX_GAME_LEVEL
import com.example.cognicare.core.game.MIN_GAME_LEVEL
import com.example.cognicare.core.game.LevelResult
import com.example.cognicare.core.game.levelResultAfter
import com.example.cognicare.core.game.patternRecallLevel
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val roundsCompleted: Int = 0,
    /** Across the whole game, unlike [mistakesThisRound] which resets each round — reported to the backend. */
    val totalMistakes: Int = 0,
    /** Rounds needed to finish this game, set from the patient's level — see [patternRecallLevel]. */
    val winRound: Int = WIN_ROUND,
    val level: Int = MIN_GAME_LEVEL,
    val maxLevel: Int = MAX_GAME_LEVEL,
    /** False until the stored level has loaded and the first sequence is built. */
    val isReady: Boolean = false
) {
    val isComplete: Boolean get() = phase == PatternPhase.COMPLETE

    /** True when the patient finished every round rather than running out of attempts. */
    val clearedLevel: Boolean get() = roundsCompleted >= winRound

    val levelResult: LevelResult get() = levelResultAfter(level, clearedLevel)

    companion object {
        /** Default rounds, used at level 1 and by tests that construct a bare engine. */
        const val WIN_ROUND = 5

        /** Ending gently after repeated misses avoids an endless, discouraging retry loop. */
        const val MAX_MISTAKES_PER_ROUND = 3
    }
}

/**
 * Simon-says rules with no timing or Android dependencies, so every transition is testable.
 * Taps outside [PatternPhase.INPUT] are ignored, which is what keeps stray taps during
 * playback or pauses from corrupting a round.
 *
 * [winRound] and [level] come from the patient's stored level; both default to the level-1
 * values so an engine built with no arguments behaves exactly as it always has.
 */
class PatternRecallEngine(
    private val random: Random = Random.Default,
    private val winRound: Int = PatternRecallUiState.WIN_ROUND,
    private val level: Int = MIN_GAME_LEVEL
) {

    var state: PatternRecallUiState = PatternRecallUiState()
        private set

    fun startGame(): PatternRecallUiState {
        state = PatternRecallUiState(
            sequence = listOf(randomColor()),
            winRound = winRound,
            level = level,
            isReady = true
        )
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
            val totalMistakes = current.totalMistakes + 1
            return if (mistakes >= PatternRecallUiState.MAX_MISTAKES_PER_ROUND) {
                state = current.copy(
                    phase = PatternPhase.COMPLETE,
                    playerInput = emptyList(),
                    mistakesThisRound = mistakes,
                    totalMistakes = totalMistakes
                )
                TapOutcome.GAME_COMPLETE
            } else {
                state = current.copy(
                    phase = PatternPhase.MISTAKE,
                    playerInput = emptyList(),
                    mistakesThisRound = mistakes,
                    totalMistakes = totalMistakes
                )
                TapOutcome.MISTAKE
            }
        }

        val input = current.playerInput + color
        if (input.size < current.sequence.size) {
            state = current.copy(playerInput = input)
            return TapOutcome.CORRECT
        }

        val roundsCompleted = current.roundsCompleted + 1
        return if (current.round >= current.winRound) {
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

    fun clearedLevel(): Boolean = state.clearedLevel

    private fun randomColor(): PatternColor = PatternColor.entries[random.nextInt(PatternColor.entries.size)]
}

@HiltViewModel
class PatternRecallViewModel @Inject constructor(
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    // Built once the stored level is known, so rounds and playback speed match the patient.
    private var engine = PatternRecallEngine()

    private val _uiState = MutableStateFlow(PatternRecallUiState())
    val uiState: StateFlow<PatternRecallUiState> = _uiState.asStateFlow()

    /** The single playback/pause job; replaced (never duplicated) whenever a new one starts. */
    private var sequenceJob: Job? = null
    private var startedAtMillis = System.currentTimeMillis()
    private var hasReportedCompletion = false
    private var highlightMs = patternRecallLevel(MIN_GAME_LEVEL).highlightMs
    private var gapMs = patternRecallLevel(MIN_GAME_LEVEL).gapMs

    init {
        viewModelScope.launch {
            val tuning = patternRecallLevel(preferences.currentGameLevel(GameType.PATTERN_RECALL))
            highlightMs = tuning.highlightMs
            gapMs = tuning.gapMs
            engine = PatternRecallEngine(winRound = tuning.winRound, level = tuning.level)
            engine.startGame()
            startedAtMillis = System.currentTimeMillis()
            runSequence(pauseMs = 0L)
        }
    }

    fun onColorTap(color: PatternColor) {
        when (engine.tap(color)) {
            TapOutcome.IGNORED -> Unit
            TapOutcome.CORRECT -> publish()
            TapOutcome.GAME_COMPLETE -> {
                publish()
                reportCompletionOnce()
            }
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

    // No app-side backend game code exists for Pattern Recall (see repository/GameCodeMapping.kt),
    // so the network call no-ops there — but the level still advances locally, which is what the
    // patient actually experiences.
    private fun reportCompletionOnce() {
        if (hasReportedCompletion) return
        hasReportedCompletion = true
        viewModelScope.launch {
            val state = engine.state
            // The screen navigates away as soon as the game completes, cancelling this scope;
            // NonCancellable keeps the level from being silently lost.
            withContext(NonCancellable) {
                preferences.setGameLevel(GameType.PATTERN_RECALL, state.levelResult.nextLevel)
            }

            if (authRepository.session.first() == null) return@launch
            val elapsedMs = System.currentTimeMillis() - startedAtMillis
            careRepository.recordGameCompletion(
                GameType.PATTERN_RECALL,
                GameOutcome(
                    scorePercent = state.roundsCompleted * 100.0 / state.winRound,
                    correctAnswers = state.roundsCompleted,
                    totalQuestions = state.winRound,
                    responseTimeMs = elapsedMs.toInt(),
                    mistakes = state.totalMistakes,
                    difficultyLevel = state.level
                )
            )
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
                delay(highlightMs)
                engine.highlight(null)
                publish()
                delay(gapMs)
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
        const val ROUND_COMPLETE_PAUSE_MS = 900L
        const val MISTAKE_PAUSE_MS = 1_400L
    }
}
