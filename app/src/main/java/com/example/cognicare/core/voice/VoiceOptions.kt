package com.example.cognicare.core.voice

import com.example.cognicare.data.model.GameType

/**
 * What the voice service's model is shown for each [VoiceIntent]: a stable id it must answer
 * with, and a plain-English description of what that answer means.
 *
 * The descriptions are for the model only and never shown to the patient, so they stay in
 * English whatever the app language — Qwen matches a Hindi or Bengali answer against English
 * descriptions reliably, and one fixed wording keeps its behaviour the same across languages.
 */
fun VoiceIntent.voiceId(): String = when (this) {
    is VoiceIntent.ConfirmMedication -> if (taken) "MEDICATION_TAKEN" else "MEDICATION_NOT_YET"
    is VoiceIntent.RespondToGameInvite -> if (accepted) "GAME_YES" else "GAME_LATER"
    VoiceIntent.OpenGames -> "GAMES_LIST"
    is VoiceIntent.OpenGame -> gameType.name
    VoiceIntent.GoHome -> "GO_HOME"
}

fun VoiceIntent.voiceLabel(): String = when (this) {
    is VoiceIntent.ConfirmMedication ->
        if (taken) "Yes, I have taken my medicine" else "Not yet, I have not taken it"
    is VoiceIntent.RespondToGameInvite ->
        if (accepted) "Yes, let's play a game now" else "Maybe later, not now"
    VoiceIntent.OpenGames -> "Show me the games / I want to play (no particular game named)"
    is VoiceIntent.OpenGame -> gameType.voiceDescription()
    VoiceIntent.GoHome -> "Go back to the home screen"
}

private fun GameType.voiceDescription(): String = when (this) {
    GameType.MEMORY_MATCH -> "Memory Match - turn over cards and find the matching picture pairs"
    GameType.PATTERN_RECALL -> "Pattern Recall - remember the order of the coloured tiles"
    GameType.OBJECT_NAMING -> "Object Naming - say the name of the object in the picture"
    GameType.DAILY_RECALL -> "Daily Recall - talk about my day and what I did"
    GameType.ORIENTATION -> "Orientation - what day, date and place it is"
    GameType.FAMILY_IDENTIFICATION -> "Family Identification - recognise the faces of my family"
}

/** The question on screen, in English for the model; null for the free-form assistant. */
fun SuggestionKind.voiceQuestion(): String = when (this) {
    SuggestionKind.MEDICATION_CHECK -> "Did you take your medicine today?"
    SuggestionKind.GAME_INVITE -> "Would you like to play a game now?"
}

/** What the model may answer with, and how each answer maps back to an intent. */
data class VoiceChoiceSet(val options: List<VoiceIntent>) {
    init {
        require(options.map { it.voiceId() }.toSet().size == options.size) { "Voice option ids must be unique" }
    }

    fun intentFor(id: String?): VoiceIntent? = options.firstOrNull { it.voiceId() == id }
}
