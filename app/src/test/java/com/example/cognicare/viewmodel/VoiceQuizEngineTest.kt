package com.example.cognicare.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceQuizEngineTest {

    private fun question(id: String, answer: String) = VoiceQuizQuestion(
        id = id,
        prompt = "prompt",
        correctOption = answer,
        answerLabel = answer,
        acceptedAnswers = listOf(answer),
        options = listOf(answer, "Other")
    )

    @Test
    fun spokenSentenceContainingTheAnswerIsAccepted() {
        assertTrue(VoiceQuizEngine.isAccepted("It's Friday today.", listOf("Friday")))
    }

    @Test
    fun partialWordIsNotAccepted() {
        assertFalse(VoiceQuizEngine.isAccepted("homework", listOf("home")))
        assertFalse(VoiceQuizEngine.isAccepted("Fri", listOf("Friday")))
    }

    @Test
    fun anyAcceptedAnswerCounts() {
        assertTrue(VoiceQuizEngine.isAccepted("that's my daughter", listOf("Priya", "daughter")))
    }

    @Test
    fun secondAnswerToSameQuestionIsIgnored() {
        val engine = VoiceQuizEngine(listOf(question("q1", "Home")))
        assertTrue(engine.submit("home", fromSpeech = true))
        assertFalse(engine.submit("Hospital", fromSpeech = false))
        assertEquals(AnswerFeedback.CORRECT, engine.state.value.feedback)
        assertEquals(1, engine.state.value.correctCount)
    }

    @Test
    fun tappedAnswerDoesNotShowAsHeard() {
        val engine = VoiceQuizEngine(listOf(question("q1", "Home")))
        engine.submit("Other", fromSpeech = false)
        assertNull(engine.state.value.heardAnswer)
        assertEquals(AnswerFeedback.INCORRECT, engine.state.value.feedback)
    }

    @Test
    fun advancingPastLastQuestionCompletes() {
        val engine = VoiceQuizEngine(listOf(question("q1", "A"), question("q2", "B")))
        engine.submit("A", fromSpeech = false)
        engine.advance()
        assertEquals(1, engine.state.value.questionIndex)
        assertEquals(AnswerFeedback.NONE, engine.state.value.feedback)
        engine.submit("B", fromSpeech = false)
        engine.advance()
        assertTrue(engine.state.value.isComplete)
    }
}
