package com.example.cognicare.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.BuildConfig
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.core.time.isSameDay
import com.example.cognicare.core.voice.KeywordVoiceInterpreter
import com.example.cognicare.core.voice.Suggestion
import com.example.cognicare.core.voice.SuggestionOption
import com.example.cognicare.core.voice.Suggestions
import com.example.cognicare.core.voice.VoiceCommandInterpreter
import com.example.cognicare.core.voice.VoiceIntent
import com.example.cognicare.core.voice.acknowledgementFor
import com.example.cognicare.core.voice.resolveAssistantCommand
import com.example.cognicare.core.voice.voiceWordsRes
import com.example.cognicare.core.voice.VoiceChoiceSet
import com.example.cognicare.core.voice.voiceId
import com.example.cognicare.core.voice.voiceLabel
import com.example.cognicare.core.voice.voiceQuestion
import com.example.cognicare.data.model.GameType
import com.example.cognicare.core.voice.splitAnswerWords
import com.example.cognicare.data.model.ReminderKind
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import com.example.cognicare.repository.VoiceChoice
import com.example.cognicare.repository.VoiceOptionRequest
import com.example.cognicare.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
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
    data class Game(val gameType: GameType) : AssistantNavigation
    data object Home : AssistantNavigation
}

/**
 * Drives both the floating voice assistant and the proactive suggestion dialogs.
 *
 * Two paths into one outcome: [onSuggestionSpeech] (voice) and [onSuggestionOption] (tap) both
 * end in [respond] with a [VoiceIntent], so medication confirmations and game invitations are
 * handled identically however the patient answered. The tap path is entirely local, which keeps
 * suggestions answerable with no connection at all.
 *
 * Spoken answers go to the voice service (Qwen on the GPU machine) first. When it can't be
 * reached, the on-device keyword [interpreter] decides instead, so voice keeps working offline.
 */
@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val localeProvider: AppLocaleProvider,
    private val careRepository: CareRepository,
    private val authRepository: AuthRepository,
    private val voiceRepository: VoiceRepository
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
    private var speechJob: Job? = null

    init {
        startDemoMedicationTrigger()
    }

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

    /** Voice path: Qwen when the voice service is reachable, keyword matching when it isn't. */
    fun onSuggestionSpeech(spoken: String) {
        val suggestion = _uiState.value.suggestion ?: return
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            val intent = interpretSpeech(
                spoken = spoken,
                question = suggestion.kind.voiceQuestion(),
                intents = suggestion.options.map { it.intent }
            ) { interpreter.interpret(spoken, suggestion.options.map { it.intent to wordsFor(it) }) }
            // The patient may have tapped an answer while the service was thinking.
            if (_uiState.value.suggestion != suggestion) return@launch
            if (intent == null) {
                _uiState.update { it.copy(suggestionNotUnderstood = true) }
            } else {
                respond(intent)
            }
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
            is VoiceIntent.ConfirmMedication -> if (intent.taken) markTodaysMedicationDone()
            is VoiceIntent.RespondToGameInvite -> if (intent.accepted) navigate(AssistantNavigation.Games)
            VoiceIntent.OpenGames -> navigate(AssistantNavigation.Games)
            is VoiceIntent.OpenGame -> navigate(AssistantNavigation.Game(intent.gameType))
            VoiceIntent.GoHome -> navigate(AssistantNavigation.Home)
        }
    }

    // ---- Free-form assistant (the floating robot button) --------------------------------------

    fun onAssistantSpeech(spoken: String) {
        val gameCandidates = GameType.entries.map { game ->
            VoiceIntent.OpenGame(game) to splitAnswerWords(localeProvider.getString(game.voiceWordsRes()))
        }
        val generalCandidates = listOf(
            VoiceIntent.OpenGames to splitAnswerWords(localeProvider.getString(R.string.assistant_games_words)),
            VoiceIntent.GoHome to splitAnswerWords(localeProvider.getString(R.string.assistant_home_words))
        )
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            val intent = interpretSpeech(
                spoken = spoken,
                question = null,
                intents = (gameCandidates + generalCandidates).map { it.first }
            ) { resolveAssistantCommand(spoken, gameCandidates, generalCandidates, interpreter) }
            when (intent) {
                null -> showAssistantMessage(R.string.assistant_not_understood)
                else -> {
                    showAssistantMessage(acknowledgementFor(intent))
                    handle(intent)
                }
            }
        }
    }

    /**
     * What the patient meant, according to Qwen; [onDevice] decides only when the voice service
     * is unavailable. Qwen's "unclear" is final — it is never overruled by a keyword guess.
     */
    private suspend fun interpretSpeech(
        spoken: String,
        question: String?,
        intents: List<VoiceIntent>,
        onDevice: () -> VoiceIntent?
    ): VoiceIntent? {
        val choices = VoiceChoiceSet(intents)
        val options = intents.map { VoiceOptionRequest(it.voiceId(), it.voiceLabel()) }
        return when (val choice = voiceRepository.interpret(spoken, question, options, localeProvider.language.tag)) {
            is VoiceChoice.Chosen -> choices.intentFor(choice.id)
            VoiceChoice.NotUnderstood -> null
            VoiceChoice.Unavailable -> onDevice()
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

    /** Ticks off today's medication reminder, so the home screen agrees with what was said. */
    private fun markTodaysMedicationDone() {
        viewModelScope.launch {
            runCatching {
                val session = authRepository.session.firstOrNull() ?: return@launch
                val now = System.currentTimeMillis()
                careRepository.observeReminders(session.userId).first()
                    .firstOrNull { it.kind == ReminderKind.MEDICATION && !it.completed && isSameDay(it.scheduledTime, now) }
                    ?.let { careRepository.setReminderCompleted(it.id, true) }
            }
        }
    }

    private fun wordsFor(option: SuggestionOption): List<String> =
        splitAnswerWords(localeProvider.getString(option.wordsRes))

    /**
     * STUB — stands in for real reminder scheduling, which this project does not have yet (no
     * WorkManager/AlarmManager, and nothing fires while the app is closed). While the patient area
     * is open it asks the medication question once in the mid-afternoon window.
     *
     * Debug builds also ask shortly after opening, so the dialog can be demonstrated at any hour.
     * The once-a-day memory is in-process only and resets when the app restarts.
     */
    private fun startDemoMedicationTrigger() {
        viewModelScope.launch {
            var askedOn: LocalDate? = null
            if (BuildConfig.DEBUG) {
                delay(DEMO_DELAY_MS)
                showSuggestion(Suggestions.medicationCheck)
                askedOn = LocalDate.now()
            }
            while (true) {
                val now = LocalTime.now()
                val today = LocalDate.now()
                if (askedOn != today && now >= MEDICATION_WINDOW_START && now < MEDICATION_WINDOW_END) {
                    showSuggestion(Suggestions.medicationCheck)
                    askedOn = today
                }
                delay(TRIGGER_POLL_MS)
            }
        }
    }

    private companion object {
        const val ACKNOWLEDGEMENT_MS = 2_200L
        const val ASSISTANT_MESSAGE_MS = 4_500L
        const val DEMO_DELAY_MS = 12_000L
        const val TRIGGER_POLL_MS = 60_000L
        val MEDICATION_WINDOW_START: LocalTime = LocalTime.of(15, 0)
        val MEDICATION_WINDOW_END: LocalTime = LocalTime.of(20, 0)
    }
}
