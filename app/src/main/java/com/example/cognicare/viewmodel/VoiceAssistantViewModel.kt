package com.example.cognicare.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.core.voice.KeywordVoiceInterpreter
import com.example.cognicare.core.voice.Suggestion
import com.example.cognicare.core.voice.SuggestionOption
import com.example.cognicare.core.voice.VoiceCommandInterpreter
import com.example.cognicare.core.voice.VoiceIntent
import com.example.cognicare.core.voice.acknowledgementFor
import com.example.cognicare.core.voice.splitAnswerWords
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceAssistantUiState(
    /** The question on screen, or null when no dialog is open. */
    val suggestion: Suggestion? = null,
    /** After an answer, the dialog shows this warm reply briefly before closing itself. */
    @StringRes val acknowledgementRes: Int? = null,
    /** Speech was heard but matched no answer; the dialog invites another try or a tap. */
    val suggestionNotUnderstood: Boolean = false,
    /** Short reply shown beside the floating assistant after a spoken command. */
    @StringRes val assistantMessageRes: Int? = null
)

/** One-shot navigation the overlay performs; the ViewModel never holds a NavController. */
sealed interface AssistantNavigation {
    data object Games : AssistantNavigation
    data object Home : AssistantNavigation
}

/**
 * Drives both the floating voice assistant and the proactive suggestion dialogs.
 *
 * Two paths into one outcome: [onSuggestionSpeech] (voice) and [onSuggestionOption] (tap) both
 * end in [respond] with a [VoiceIntent], so medication confirmations and game invitations are
 * handled identically however the patient answered. The tap path is entirely local, which keeps
 * suggestions answerable with no connection at all.
 */
@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val localeProvider: AppLocaleProvider,
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val interpreter: VoiceCommandInterpreter = KeywordVoiceInterpreter

    private val _uiState = MutableStateFlow(VoiceAssistantUiState())
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    private val _navigation = Channel<AssistantNavigation>(Channel.BUFFERED)
    val navigation: Flow<AssistantNavigation> = _navigation.receiveAsFlow()

    /** Suggestions that fired while another was open; shown one after another, never stacked. */
    private val queued = ArrayDeque<Suggestion>()
    private var closeJob: Job? = null
    private var assistantMessageJob: Job? = null

    // No unsolicited demo medication popups — reminders are schedule-driven and patient-managed.

    // ---- Suggestions -------------------------------------------------------------------------

    fun showSuggestion(suggestion: Suggestion) {
        val current = _uiState.value
        if (current.suggestion != null || current.acknowledgementRes != null) {
            if (queued.none { it.kind == suggestion.kind } && current.suggestion?.kind != suggestion.kind) {
                queued.addLast(suggestion)
            }
            return
        }
        _uiState.update { it.copy(suggestion = suggestion, suggestionNotUnderstood = false) }
    }

    /** Voice path: the same interpreter a server-side model would replace. */
    fun onSuggestionSpeech(spoken: String) {
        val suggestion = _uiState.value.suggestion ?: return
        val candidates = suggestion.options.map { it.intent to wordsFor(it) }
        val intent = interpreter.interpret(spoken, candidates)
        if (intent == null) {
            _uiState.update { it.copy(suggestionNotUnderstood = true) }
        } else {
            respond(intent)
        }
    }

    /** Tap path: needs nothing but the device. */
    fun onSuggestionOption(option: SuggestionOption) = respond(option.intent)

    /** Closing the dialog is the gentle "not now" — never treated as a refusal or failure. */
    fun dismissSuggestion() {
        val suggestion = _uiState.value.suggestion ?: return
        respond(suggestion.declineIntent)
    }

    private fun respond(intent: VoiceIntent) {
        if (_uiState.value.acknowledgementRes != null) return // already answered; ignore double taps
        handle(intent)
        _uiState.update {
            it.copy(acknowledgementRes = acknowledgementFor(intent), suggestionNotUnderstood = false)
        }
        closeJob?.cancel()
        closeJob = viewModelScope.launch {
            delay(ACKNOWLEDGEMENT_MS)
            _uiState.update { it.copy(suggestion = null, acknowledgementRes = null) }
            queued.removeFirstOrNull()?.let(::showSuggestion)
        }
    }

    private fun handle(intent: VoiceIntent) {
        when (intent) {
            is VoiceIntent.ConfirmMedication -> Unit
            is VoiceIntent.RespondToGameInvite -> if (intent.accepted) navigate(AssistantNavigation.Games)
            VoiceIntent.OpenGames -> navigate(AssistantNavigation.Games)
            VoiceIntent.GoHome -> navigate(AssistantNavigation.Home)
        }
    }

    // ---- Free-form assistant (the floating robot button) --------------------------------------

    fun onAssistantSpeech(spoken: String) {
        val candidates = listOf(
            VoiceIntent.OpenGames to splitAnswerWords(localeProvider.getString(R.string.assistant_games_words)),
            VoiceIntent.GoHome to splitAnswerWords(localeProvider.getString(R.string.assistant_home_words))
        )
        when (val intent = interpreter.interpret(spoken, candidates)) {
            null -> showAssistantMessage(R.string.assistant_not_understood)
            else -> {
                showAssistantMessage(acknowledgementFor(intent))
                handle(intent)
            }
        }
    }

    fun clearAssistantMessage() {
        assistantMessageJob?.cancel()
        _uiState.update { it.copy(assistantMessageRes = null) }
    }

    private fun showAssistantMessage(@StringRes res: Int) {
        _uiState.update { it.copy(assistantMessageRes = res) }
        assistantMessageJob?.cancel()
        assistantMessageJob = viewModelScope.launch {
            delay(ASSISTANT_MESSAGE_MS)
            _uiState.update { it.copy(assistantMessageRes = null) }
        }
    }

    // ---- Side effects -------------------------------------------------------------------------

    private fun navigate(target: AssistantNavigation) {
        viewModelScope.launch { _navigation.send(target) }
    }

    private fun wordsFor(option: SuggestionOption): List<String> =
        splitAnswerWords(localeProvider.getString(option.wordsRes))

    private companion object {
        const val ACKNOWLEDGEMENT_MS = 2_200L
        const val ASSISTANT_MESSAGE_MS = 4_500L
    }
}
