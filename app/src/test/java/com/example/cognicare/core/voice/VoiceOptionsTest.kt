package com.example.cognicare.core.voice

import com.example.cognicare.data.model.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceOptionsTest {

    private val assistantIntents: List<VoiceIntent> =
        GameType.entries.map { VoiceIntent.OpenGame(it) } + VoiceIntent.OpenGames + VoiceIntent.GoHome

    @Test
    fun `every assistant option has a distinct id the service accepts`() {
        val ids = assistantIntents.map { it.voiceId() }
        assertEquals(ids.size, ids.toSet().size)
        // voice_service/app/schemas.py: ids match ^[A-Za-z0-9_\-]+$, at most 10 options.
        assertTrue(ids.all { it.matches(Regex("^[A-Za-z0-9_\\-]{1,64}$")) })
        assertTrue(ids.size <= 10)
    }

    @Test
    fun `labels fit the service's limit`() {
        val intents = assistantIntents + Suggestions.medicationCheck.options.map { it.intent } +
            Suggestions.gameInvite.options.map { it.intent }
        assertTrue(intents.all { it.voiceLabel().length in 1..120 })
    }

    @Test
    fun `the service's answer maps back to the intent it stands for`() {
        val choices = VoiceChoiceSet(Suggestions.medicationCheck.options.map { it.intent })
        assertEquals(VoiceIntent.ConfirmMedication(taken = true), choices.intentFor("MEDICATION_TAKEN"))
        assertEquals(VoiceIntent.ConfirmMedication(taken = false), choices.intentFor("MEDICATION_NOT_YET"))
    }

    @Test
    fun `an id that was not offered is never acted on`() {
        val choices = VoiceChoiceSet(Suggestions.medicationCheck.options.map { it.intent })
        assertNull(choices.intentFor("GO_HOME"))
        assertNull(choices.intentFor(null))
    }

    @Test
    fun `a named game comes back as that game`() {
        val choices = VoiceChoiceSet(assistantIntents)
        assertEquals(VoiceIntent.OpenGame(GameType.PATTERN_RECALL), choices.intentFor("PATTERN_RECALL"))
        assertEquals(VoiceIntent.OpenGames, choices.intentFor("GAMES_LIST"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate ids are rejected`() {
        VoiceChoiceSet(listOf(VoiceIntent.GoHome, VoiceIntent.GoHome))
    }
}
