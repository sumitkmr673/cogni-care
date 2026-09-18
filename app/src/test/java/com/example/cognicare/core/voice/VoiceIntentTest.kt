package com.example.cognicare.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceIntentTest {

    private val taken = VoiceIntent.ConfirmMedication(taken = true)
    private val notYet = VoiceIntent.ConfirmMedication(taken = false)

    // Mirrors the word lists in res/values*/strings.xml.
    private val english = listOf(
        taken to splitAnswerWords("yes, yeah, yep, took, taken, done, already"),
        notYet to splitAnswerWords("no, not yet, not, later, nope, haven't, didn't, forgot")
    )
    private val hindi = listOf(
        taken to splitAnswerWords("हाँ, हां, जी, जी हाँ, ले ली, ले लिया, खा ली, खा लिया, yes"),
        notYet to splitAnswerWords("नहीं, नही, अभी नहीं, बाद में, भूल गया, भूल गई, no")
    )
    private val bengali = listOf(
        taken to splitAnswerWords("হ্যাঁ, হাঁ, হ্যা, খেয়েছি, নিয়েছি, হয়ে গেছে, yes"),
        notYet to splitAnswerWords("না, এখনও না, এখনো না, পরে, ভুলে গেছি, খাইনি, no")
    )

    private fun interpret(spoken: String, candidates: List<Pair<VoiceIntent, List<String>>>) =
        KeywordVoiceInterpreter.interpret(spoken, candidates)

    @Test
    fun spokenAnswersResolveToTheSameIntentAsTheButtons() {
        assertEquals(taken, interpret("Yes, I took it", english))
        assertEquals(taken, interpret("I already had it", english))
        assertEquals(notYet, interpret("Not yet", english))
        assertEquals(notYet, interpret("No, I forgot", english))
    }

    @Test
    fun hindiAndBengaliAnswersAreUnderstood() {
        assertEquals(taken, interpret("हाँ, ले ली", hindi))
        assertEquals(notYet, interpret("अभी नहीं", hindi))
        assertEquals(taken, interpret("হ্যাঁ, খেয়েছি", bengali))
        assertEquals(notYet, interpret("এখনও না", bengali))
    }

    @Test
    fun ambiguousAnswersAreAskedAgainRatherThanGuessed() {
        // Both "no" and "took" — picking either could record the wrong thing about medication.
        assertNull(interpret("No, I took it", english))
    }

    @Test
    fun unrelatedSpeechMatchesNothing() {
        assertNull(interpret("What a lovely morning", english))
        assertNull(interpret("", english))
    }

    @Test
    fun wordsMatchWholeWordsOnly() {
        // "not" must not fire inside "nothing", nor "no" inside "know".
        assertNull(interpret("I know nothing about it", english))
    }

    @Test
    fun wordListsAreTrimmedAndSkipEmptyEntries() {
        assertEquals(listOf("yes", "not yet"), splitAnswerWords(" yes , , not yet ,"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun aSuggestionNeedsAtLeastTwoAnswers() {
        Suggestion(
            kind = SuggestionKind.MEDICATION_CHECK,
            questionRes = 1,
            options = listOf(SuggestionOption(2, 3, taken)),
            declineIntent = notYet
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun aSuggestionOffersNoMoreThanThreeAnswers() {
        val option = SuggestionOption(2, 3, taken)
        Suggestion(
            kind = SuggestionKind.GAME_INVITE,
            questionRes = 1,
            options = List(4) { option },
            declineIntent = notYet
        )
    }
}
