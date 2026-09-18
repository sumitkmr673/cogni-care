package com.example.cognicare.core.text

import java.util.Locale

/** Whole-word match, so "It's Friday today" accepts "friday" but "homework" does not accept "home". */
fun isAcceptedAnswer(answer: String, accepted: List<String>): Boolean {
    val spoken = " ${normalizeForMatching(answer)} "
    return accepted.any { token ->
        val word = normalizeForMatching(token)
        word.isNotEmpty() && spoken.contains(" $word ")
    }
}

fun normalizeForMatching(text: String): String =
    text.lowercase(Locale.getDefault())
        .replace(Regex("[^\\p{L}\\p{N}\\p{M}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Whether what the patient said or typed is their own name. The first name or the full name both
 * count, and bracketed labels in the account name such as "(Demo)" are ignored, so "Meera",
 * "meera sharma" and "My name is Meera" all match "Meera Sharma (Demo)".
 */
fun patientNameMatches(input: String, registeredName: String): Boolean {
    val cleaned = registeredName.replace(Regex("\\(.*?\\)"), " ").trim()
    if (cleaned.isBlank()) return false
    val firstName = cleaned.substringBefore(' ')
    return isAcceptedAnswer(input, listOf(cleaned, firstName))
}
