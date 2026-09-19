package com.example.cognicare.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.text.isAcceptedAnswer
import com.example.cognicare.data.model.GameOutcome
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface QuizVisual {
    data class Emoji(val emoji: String) : QuizVisual

    /**
     * A square region of a photo, shown zoomed in. Fractions keep it resolution-independent:
     * [left] and [size] are fractions of the photo's width, [top] of its height.
     */
    data class PhotoCrop(
        @DrawableRes val drawableRes: Int,
        val left: Float,
        val top: Float,
        val size: Float
    ) : QuizVisual
}

data class VoiceQuizQuestion(
    val id: String,
    val prompt: String,
    /** The tap option that counts as correct; must be one of [options]. */
    val correctOption: String,
    /** How the answer is phrased when gently revealed, e.g. "Priya, your daughter". */
    val answerLabel: String,
    /** Words that count as correct when spoken or tapped. */
    val acceptedAnswers: List<String>,
    val options: List<String>,
    val visual: QuizVisual? = null,
    /**
     * Words to steer Whisper's spelling, e.g. every choice in every language. Must cover all the
     * choices alike, never only the correct one. Null: the on-screen [options].
     */
    val speechHint: String? = null
)

/** Whisper's hint from word groups: one group per choice, deduplicated, in order. */
fun speechHintOf(groups: List<List<String>>): String = groups.flatten().distinct().joinToString(", ")

enum class AnswerFeedback { NONE, CORRECT, INCORRECT }

data class VoiceQuizUiState(
    val questions: List<VoiceQuizQuestion> = emptyList(),
    val questionIndex: Int = 0,
    /** Only set for spoken answers, so the patient can see what was heard. */
    val heardAnswer: String? = null,
    val feedback: AnswerFeedback = AnswerFeedback.NONE,
    val correctCount: Int = 0,
    val isComplete: Boolean = false
) {
    val currentQuestion: VoiceQuizQuestion? get() = questions.getOrNull(questionIndex)
    val isAwaitingAnswer: Boolean get() = feedback == AnswerFeedback.NONE && !isComplete
}

/** Step-through-and-check flow shared by the voice-answer games. No Android dependencies. */
class VoiceQuizEngine(questions: List<VoiceQuizQuestion>) {

    private val _state = MutableStateFlow(VoiceQuizUiState(questions = questions))
    val state: StateFlow<VoiceQuizUiState> = _state.asStateFlow()

    /** Returns false when ignored: blank input, or the current question is already answered. */
    fun submit(answer: String, fromSpeech: Boolean): Boolean {
        val current = _state.value
        val question = current.currentQuestion ?: return false
        if (!current.isAwaitingAnswer || answer.isBlank()) return false

        val correct = isAccepted(answer, question.acceptedAnswers)
        _state.value = current.copy(
            heardAnswer = answer.trim().takeIf { fromSpeech },
            feedback = if (correct) AnswerFeedback.CORRECT else AnswerFeedback.INCORRECT,
            correctCount = current.correctCount + if (correct) 1 else 0
        )
        return true
    }

    fun advance() {
        val current = _state.value
        val next = current.questionIndex + 1
        _state.value = if (next >= current.questions.size) {
            current.copy(isComplete = true)
        } else {
            current.copy(questionIndex = next, heardAnswer = null, feedback = AnswerFeedback.NONE)
        }
    }

    companion object {
        /** Whole-word match, so "It's Friday today" accepts "friday" but "homework" does not accept "home". */
        fun isAccepted(answer: String, accepted: List<String>): Boolean = isAcceptedAnswer(answer, accepted)
    }
}

/** Correct option plus up to [count] - 1 distractors from [pool], shuffled. */
fun quizChoices(correct: String, pool: List<String>, count: Int = 3): List<String> =
    (pool.filter { it != correct }.distinct().shuffled().take(count - 1) + correct).shuffled()

/**
 * Base for the voice games. Answers are never blocked or retried: the patient hears a gentle
 * reveal and moves on, and no score is shown on the patient side. [gameType] identifies which
 * backend game code the finished session reports under (see [com.example.cognicare.repository.toBackendCode]).
 */
abstract class VoiceQuizViewModel(
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository,
    private val gameType: GameType
) : ViewModel() {

    private val engine: VoiceQuizEngine by lazy { VoiceQuizEngine(buildQuestions()) }
    private val startedAtMillis = System.currentTimeMillis()
    private var hasReportedCompletion = false

    val uiState: StateFlow<VoiceQuizUiState>
        get() = engine.state

    protected abstract fun buildQuestions(): List<VoiceQuizQuestion>

    fun onSpeechResult(text: String) = answer(text, fromSpeech = true)

    fun onOptionSelected(option: String) = answer(option, fromSpeech = false)

    private fun answer(text: String, fromSpeech: Boolean) {
        if (!engine.submit(text, fromSpeech)) return
        val pause = if (engine.state.value.feedback == AnswerFeedback.CORRECT) CORRECT_PAUSE_MS else REVEAL_PAUSE_MS
        viewModelScope.launch {
            delay(pause)
            engine.advance()
            val finished = engine.state.value
            if (finished.isComplete && !hasReportedCompletion) {
                hasReportedCompletion = true
                reportCompletion(finished)
            }
        }
    }

    private suspend fun reportCompletion(finished: VoiceQuizUiState) {
        // Games are only reachable from the patient graph, but a stale/expired session could
        // still slip through; skip silently rather than let CareRepository fail on it.
        if (authRepository.session.first() == null) return

        val total = finished.questions.size
        if (total == 0) return
        val correct = finished.correctCount
        val elapsedMs = System.currentTimeMillis() - startedAtMillis

        careRepository.recordGameCompletion(
            gameType,
            GameOutcome(
                scorePercent = correct * 100.0 / total,
                accuracyPercent = correct * 100.0 / total,
                correctAnswers = correct,
                totalQuestions = total,
                responseTimeMs = (elapsedMs / total).toInt(),
                mistakes = total - correct
            )
        )
    }

    private companion object {
        const val CORRECT_PAUSE_MS = 1_600L
        const val REVEAL_PAUSE_MS = 2_800L
    }
}
