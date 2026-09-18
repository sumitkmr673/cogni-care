package com.example.cognicare.core.voice

import androidx.annotation.StringRes
import com.example.cognicare.core.text.isAcceptedAnswer

/**
 * Everything the voice assistant or a suggestion dialog can result in. A spoken answer and a
 * tapped button resolve to the same [VoiceIntent], so whatever handles it never needs to know
 * which input the patient used — and the tap path, which needs no network, is always available.
 */
sealed interface VoiceIntent {
    /** Answer to "Did you take your medicine?" — `false` is "not yet", never a failure. */
    data class ConfirmMedication(val taken: Boolean) : VoiceIntent

    /** Answer to an invitation to play; `false` is "maybe later". */
    data class RespondToGameInvite(val accepted: Boolean) : VoiceIntent

    data object OpenGames : VoiceIntent

    data object GoHome : VoiceIntent
}

enum class SuggestionKind { MEDICATION_CHECK, GAME_INVITE }

/**
 * One large answer button in a suggestion dialog. [wordsRes] is a comma-separated list, in the
 * patient's language, of what counts as saying this answer out loud.
 */
data class SuggestionOption(
    @StringRes val labelRes: Int,
    @StringRes val wordsRes: Int,
    val intent: VoiceIntent
)

/**
 * A proactive question shown on top of whatever the patient is doing. Every kind — medication,
 * game invitations, anything added later — is rendered by the same `SuggestionDialog`; only this
 * data differs. [declineIntent] is what closing the dialog without answering means: the gentle
 * "not now" option, so dismissing is treated exactly like saying "not yet".
 */
data class Suggestion(
    val kind: SuggestionKind,
    @StringRes val questionRes: Int,
    val options: List<SuggestionOption>,
    val declineIntent: VoiceIntent
) {
    init {
        require(options.size in 2..3) { "A suggestion offers two or three answers, not ${options.size}" }
    }
}

/**
 * Turns what the patient said into an intent, choosing among [candidates] (each intent paired with
 * the words that mean it). Returns null when nothing — or more than one thing — matched, so the
 * dialog can gently ask again rather than guess.
 *
 * The keyword version below runs fully on the device. A server-side interpreter (Whisper + Qwen
 * in cogni-care/Models) can implement this same interface later; the dialog and ViewModel will
 * not change, and the tap buttons keep working when that server is unreachable.
 */
fun interface VoiceCommandInterpreter {
    fun interpret(spoken: String, candidates: List<Pair<VoiceIntent, List<String>>>): VoiceIntent?
}

object KeywordVoiceInterpreter : VoiceCommandInterpreter {
    override fun interpret(spoken: String, candidates: List<Pair<VoiceIntent, List<String>>>): VoiceIntent? {
        val matched = candidates.filter { (_, words) -> isAcceptedAnswer(spoken, words) }
        // "No, I took it" hits both answers; asking again is kinder than picking one.
        return matched.singleOrNull()?.first
    }
}

/** Splits a comma-separated word-list string resource into individual phrases. */
fun splitAnswerWords(csv: String): List<String> =
    csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
