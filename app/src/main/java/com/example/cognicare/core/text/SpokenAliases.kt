package com.example.cognicare.core.text

import java.util.Calendar

/**
 * Hindi and Bengali answers as they come out in Latin letters. Patients mix languages ("yeh seb
 * hai"), and both Whisper in English mode and Android's en-IN recogniser write such words in
 * Latin script, so "सेब" arrives as "seb". The native-script names come from the translated
 * string resources; this only adds the romanised spellings, including common variants.
 *
 * Keyed by the English word so the list is readable and testable without resources. Spellings that
 * are also common words with another meaning are left out ("sab" is "all", "gadi" is "car"), so a
 * wrong answer can't be counted right by accident.
 */
object SpokenAliases {

    val objects: Map<String, List<String>> = mapOf(
        "watch" to listOf("ghadi", "ghari", "ghodi", "ghori"),
        "pencil" to listOf("pensil", "painsil"),
        "apple" to listOf("seb", "sev", "saeb", "apel"),
        "key" to listOf("chabi", "chaabi", "chavi", "chaabhi"),
        "dog" to listOf("kutta", "kuttaa", "kutha", "kukur")
    )

    val places: Map<String, List<String>> = mapOf(
        "home" to listOf("ghar", "gharpe", "bari", "baadi"),
        "hospital" to listOf("aspatal", "haspatal", "hospitol"),
        "market" to listOf("bazaar", "bazar", "bajar")
    )

    /** By [Calendar] day-of-week constant (Sunday = 1). Hindi first, then Bengali. */
    val weekdays: Map<Int, List<String>> = mapOf(
        Calendar.SUNDAY to listOf("ravivar", "raviwar", "itvar", "itwar", "robibar", "robibaar"),
        Calendar.MONDAY to listOf("somvar", "somwar", "shombar", "sombar"),
        Calendar.TUESDAY to listOf("mangalvar", "mangalwar", "mongolbar", "mangalbar"),
        Calendar.WEDNESDAY to listOf("budhvar", "budhwar", "budhbar"),
        Calendar.THURSDAY to listOf("guruvar", "guruwar", "brihaspativar", "brihospotibar", "bishudbar"),
        Calendar.FRIDAY to listOf("shukravar", "shukrawar", "sukravar", "shukrobar", "sukrobar"),
        Calendar.SATURDAY to listOf("shanivar", "shaniwar", "shonibar", "sonibar")
    )
}
