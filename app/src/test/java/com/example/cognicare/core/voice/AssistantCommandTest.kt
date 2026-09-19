package com.example.cognicare.core.voice

import com.example.cognicare.data.model.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssistantCommandTest {

    // Mirrors res/values/strings.xml (assistant_game_*_words, assistant_games_words, assistant_home_words).
    private val games = listOf(
        VoiceIntent.OpenGame(GameType.MEMORY_MATCH) to splitAnswerWords("memory match, matching, match the pairs, pairs, cards"),
        VoiceIntent.OpenGame(GameType.PATTERN_RECALL) to splitAnswerWords("pattern, patterns, pattern recall, colours, colors"),
        VoiceIntent.OpenGame(GameType.OBJECT_NAMING) to splitAnswerWords("object, objects, object naming, name the picture, pictures"),
        VoiceIntent.OpenGame(GameType.DAILY_RECALL) to splitAnswerWords("daily recall, my day, about my day, talk about my day"),
        VoiceIntent.OpenGame(GameType.ORIENTATION) to splitAnswerWords("orientation, what day, where are we, date"),
        VoiceIntent.OpenGame(GameType.FAMILY_IDENTIFICATION) to splitAnswerWords("family, faces, family identification, my family, relatives")
    )
    private val general = listOf(
        VoiceIntent.OpenGames to splitAnswerWords("game, games, play"),
        VoiceIntent.GoHome to splitAnswerWords("home, back, main screen")
    )
    private val hindiGames = listOf(
        VoiceIntent.OpenGame(GameType.MEMORY_MATCH) to splitAnswerWords("जोड़ी, जोड़ियाँ, जोड़ी मिलान, मिलान, कार्ड, memory match"),
        VoiceIntent.OpenGame(GameType.FAMILY_IDENTIFICATION) to splitAnswerWords("परिवार, परिवार पहचान, चेहरे, family")
    )
    private val hindiGeneral = listOf(VoiceIntent.OpenGames to splitAnswerWords("खेल, गेम, खेलना, game, play"))

    private fun resolve(spoken: String) = resolveAssistantCommand(spoken, games, general)

    @Test
    fun aNamedGameWinsOverTheGenericPlay() {
        assertEquals(VoiceIntent.OpenGame(GameType.MEMORY_MATCH), resolve("play memory match"))
        assertEquals(VoiceIntent.OpenGame(GameType.PATTERN_RECALL), resolve("I want to play the colours game"))
        assertEquals(VoiceIntent.OpenGame(GameType.FAMILY_IDENTIFICATION), resolve("let's look at my family"))
    }

    @Test
    fun noGameNamedFallsBackToTheGamesList() {
        assertEquals(VoiceIntent.OpenGames, resolve("I want to play a game"))
    }

    @Test
    fun namingTwoGamesOpensTheListInsteadOfGuessing() {
        assertEquals(VoiceIntent.OpenGames, resolve("play memory match or pattern"))
    }

    @Test
    fun goingHomeStillWorks() {
        assertEquals(VoiceIntent.GoHome, resolve("take me home"))
    }

    @Test
    fun unrelatedSpeechIsNotUnderstood() {
        assertNull(resolve("what a lovely morning"))
    }

    @Test
    fun gamesCanBeAskedForInHindi() {
        assertEquals(
            VoiceIntent.OpenGame(GameType.MEMORY_MATCH),
            resolveAssistantCommand("जोड़ी मिलान खेलना है", hindiGames, hindiGeneral)
        )
    }
}
