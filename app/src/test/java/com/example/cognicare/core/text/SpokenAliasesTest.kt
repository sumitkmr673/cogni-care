package com.example.cognicare.core.text

import com.example.cognicare.viewmodel.speechHintOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpokenAliasesTest {

    // As Object Naming builds them: every language's name plus the romanised spellings.
    private val apple = listOf("Apple", "सेब", "আপেল") + SpokenAliases.objects.getValue("apple")
    private val dog = listOf("Dog", "कुत्ता", "কুকুর", "puppy") + SpokenAliases.objects.getValue("dog")

    @Test
    fun `a hindi answer written in latin letters names the apple`() {
        assertTrue(isAcceptedAnswer("Seb", apple))
        assertTrue(isAcceptedAnswer("yeh seb hai", apple))
    }

    @Test
    fun `native script and english still count`() {
        assertTrue(isAcceptedAnswer("सेब", apple))
        assertTrue(isAcceptedAnswer("it is an apple", apple))
        assertTrue(isAcceptedAnswer("কুকুর", dog))
    }

    @Test
    fun `whispers mishearing is still not counted as correct`() {
        assertFalse(isAcceptedAnswer("see", apple))
        assertFalse(isAcceptedAnswer("sab kuch", apple))
    }

    @Test
    fun `no spelling is shared by two objects, places or weekdays`() {
        for (group in listOf(SpokenAliases.objects.values, SpokenAliases.places.values, SpokenAliases.weekdays.values)) {
            val all = group.flatten()
            assertEquals("duplicate alias in $group", all.size, all.toSet().size)
        }
    }

    @Test
    fun `every weekday has romanised spellings`() {
        assertEquals((Calendar.SUNDAY..Calendar.SATURDAY).toSet(), SpokenAliases.weekdays.keys)
    }

    @Test
    fun `the whisper hint lists each word once, in order`() {
        assertEquals("Apple, seb, Dog, kutta", speechHintOf(listOf(listOf("Apple", "seb"), listOf("Dog", "kutta", "Apple"))))
    }
}
