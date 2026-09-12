package com.example.cognicare.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFormattingTest {

    @Test
    fun initialsIgnoreParentheticalLabels() {
        assertEquals("MS", initialsOf("Meera Sharma (Demo)"))
    }

    @Test
    fun initialsUseAtMostTwoWords() {
        assertEquals("AM", initialsOf("Ananya Mehta Rao"))
    }

    @Test
    fun firstNameTrimsWhitespace() {
        assertEquals("Ananya", firstNameOf("  Ananya Mehta"))
    }
}
