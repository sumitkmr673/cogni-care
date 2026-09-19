package com.example.cognicare.core.voice

import androidx.annotation.StringRes
import com.example.cognicare.R
import com.example.cognicare.data.model.GameType

/** The proactive prompts the app can show. Adding one is a new entry here — no new dialog. */
object Suggestions {

    // Naturally a yes/no question, so two buttons rather than inventing a third.
    val medicationCheck = Suggestion(
        kind = SuggestionKind.MEDICATION_CHECK,
        questionRes = R.string.suggestion_medication_question,
        options = listOf(
            SuggestionOption(
                R.string.suggestion_medication_yes,
                R.string.suggestion_medication_yes_words,
                VoiceIntent.ConfirmMedication(taken = true)
            ),
            SuggestionOption(
                R.string.suggestion_medication_not_yet,
                R.string.suggestion_medication_not_yet_words,
                VoiceIntent.ConfirmMedication(taken = false)
            )
        ),
        declineIntent = VoiceIntent.ConfirmMedication(taken = false)
    )

    val gameInvite = Suggestion(
        kind = SuggestionKind.GAME_INVITE,
        questionRes = R.string.suggestion_game_question,
        options = listOf(
            SuggestionOption(
                R.string.suggestion_game_yes,
                R.string.suggestion_game_yes_words,
                VoiceIntent.RespondToGameInvite(accepted = true)
            ),
            SuggestionOption(
                R.string.suggestion_game_later,
                R.string.suggestion_game_later_words,
                VoiceIntent.RespondToGameInvite(accepted = false)
            )
        ),
        declineIntent = VoiceIntent.RespondToGameInvite(accepted = false)
    )
}

/** The warm one-line reply shown after an answer. None of them scold or hurry the patient. */
@StringRes
fun acknowledgementFor(intent: VoiceIntent): Int = when (intent) {
    is VoiceIntent.ConfirmMedication ->
        if (intent.taken) R.string.suggestion_ack_medication_taken else R.string.suggestion_ack_medication_not_yet
    is VoiceIntent.RespondToGameInvite ->
        if (intent.accepted) R.string.suggestion_ack_game_yes else R.string.suggestion_ack_game_later
    VoiceIntent.OpenGames -> R.string.suggestion_ack_game_yes
    VoiceIntent.GoHome -> R.string.assistant_going_home
    is VoiceIntent.OpenGame -> R.string.suggestion_ack_game_yes
}

/** What the patient might say to ask for this game, in their language (comma-separated). */
@StringRes
fun GameType.voiceWordsRes(): Int = when (this) {
    GameType.MEMORY_MATCH -> R.string.assistant_game_memory_match_words
    GameType.PATTERN_RECALL -> R.string.assistant_game_pattern_recall_words
    GameType.OBJECT_NAMING -> R.string.assistant_game_object_naming_words
    GameType.DAILY_RECALL -> R.string.assistant_game_daily_recall_words
    GameType.ORIENTATION -> R.string.assistant_game_orientation_words
    GameType.FAMILY_IDENTIFICATION -> R.string.assistant_game_family_identification_words
}
