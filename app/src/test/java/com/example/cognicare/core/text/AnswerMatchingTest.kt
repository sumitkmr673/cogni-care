package com.example.cognicare.core.text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMatchingTest {

    private val registeredName = "Meera Sharma (Demo)"

    @Test
    fun firstNameMatchesRegisteredFullName() {
        assertTrue(patientNameMatches("Meera", registeredName))
    }

    @Test
    fun fullNameAndSpokenSentenceMatch() {
        assertTrue(patientNameMatches("meera sharma", registeredName))
        assertTrue(patientNameMatches("My name is Meera.", registeredName))
    }

    @Test
    fun otherNamesAndBracketedLabelsDoNotMatch() {
        assertFalse(patientNameMatches("Priya", registeredName))
        assertFalse(patientNameMatches("Demo", registeredName))
        assertFalse(patientNameMatches("Meerab", registeredName))
    }

    @Test
    fun blankInputNeverMatches() {
        assertFalse(patientNameMatches("   ", registeredName))
    }
}
