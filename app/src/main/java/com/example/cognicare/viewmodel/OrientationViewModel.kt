package com.example.cognicare.viewmodel

import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.core.text.SpokenAliases
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Orientation to time and place, adapted from the MMSE orientation items for use at home
 * (year, month, date, weekday, place). Season and state/county/floor are left out: season
 * is ambiguous across NER climates, and the others don't apply at home.
 *
 * Correct answers come from the device clock, and month and weekday names follow the chosen
 * language. Results are indicators for caregiver trends only; this is not an MMSE
 * administration and nothing here maps to a diagnosis.
 */
@HiltViewModel
class OrientationViewModel @Inject constructor(
    private val locale: AppLocaleProvider,
    careRepository: CareRepository,
    authRepository: AuthRepository
) : VoiceQuizViewModel(careRepository, authRepository, GameType.ORIENTATION) {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val now = Calendar.getInstance()
        val symbols = DateFormatSymbols.getInstance(locale.locale)

        val year = now.get(Calendar.YEAR).toString()
        val month = symbols.months[now.get(Calendar.MONTH)]
        val date = now.get(Calendar.DAY_OF_MONTH)
        val weekday = symbols.weekdays[now.get(Calendar.DAY_OF_WEEK)]
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val home = locale.getString(R.string.orientation_place_home)
        val places = listOf(
            home,
            locale.getString(R.string.orientation_place_hospital),
            locale.getString(R.string.orientation_place_market)
        )
        // Every choice in every language (native script and romanised), for mixed-language answers.
        val placeWords = listOf(
            locale.inEveryLanguage(R.string.orientation_place_home) + listOf("house") + SpokenAliases.places["home"].orEmpty(),
            locale.inEveryLanguage(R.string.orientation_place_hospital) + SpokenAliases.places["hospital"].orEmpty(),
            locale.inEveryLanguage(R.string.orientation_place_market) + SpokenAliases.places["market"].orEmpty()
        )
        val allSymbols = AppLanguage.entries.filter { it.isTranslated }
            .map { DateFormatSymbols.getInstance(Locale.forLanguageTag(it.tag)) }
        // By Calendar value: weekdays[1] is Sunday, months[0] is January (as in DateFormatSymbols).
        val weekdayWords = (Calendar.SUNDAY..Calendar.SATURDAY).associateWith { day ->
            allSymbols.map { it.weekdays[day] } + SpokenAliases.weekdays[day].orEmpty()
        }
        val monthWords = (0..11).associateWith { index -> allSymbols.map { it.months[index] } }
        val monthOptions = quizChoices(month, symbols.months.filter { it.isNotBlank() })
        val weekdayOptions = quizChoices(weekday, symbols.weekdays.filter { it.isNotBlank() })
        // Whisper's hint covers only the choices on screen (all of them alike): all twelve months
        // in three languages would overflow its prompt.
        val monthHint = speechHintOf(monthOptions.map { monthWords.getValue(symbols.months.indexOf(it)) })
        val weekdayHint = speechHintOf(weekdayOptions.map { weekdayWords.getValue(symbols.weekdays.indexOf(it)) })
        val nearbyDates = listOf(-3, -2, -1, 1, 2, 3)
            .map { date + it }
            .filter { it in 1..daysInMonth }
            .map(Int::toString)

        return listOf(
            VoiceQuizQuestion(
                id = "year",
                prompt = locale.getString(R.string.orientation_q_year),
                visual = QuizVisual.Emoji("📅"),
                correctOption = year,
                answerLabel = year,
                acceptedAnswers = listOf(year),
                options = quizChoices(year, listOf(-2, -1, 1).map { (year.toInt() + it).toString() })
            ),
            VoiceQuizQuestion(
                id = "month",
                prompt = locale.getString(R.string.orientation_q_month),
                visual = QuizVisual.Emoji("🗓️"),
                correctOption = month,
                answerLabel = month,
                acceptedAnswers = (listOf(month) + monthWords.getValue(now.get(Calendar.MONTH))).distinct(),
                options = monthOptions,
                speechHint = monthHint
            ),
            VoiceQuizQuestion(
                id = "date",
                prompt = locale.getString(R.string.orientation_q_date),
                visual = QuizVisual.Emoji("📆"),
                correctOption = date.toString(),
                answerLabel = ordinal(date),
                acceptedAnswers = listOf(date.toString(), ordinal(date)),
                options = quizChoices(date.toString(), nearbyDates)
            ),
            VoiceQuizQuestion(
                id = "weekday",
                prompt = locale.getString(R.string.orientation_q_day),
                visual = QuizVisual.Emoji("☀️"),
                correctOption = weekday,
                answerLabel = weekday,
                acceptedAnswers = (listOf(weekday) + weekdayWords.getValue(now.get(Calendar.DAY_OF_WEEK))).distinct(),
                options = weekdayOptions,
                speechHint = weekdayHint
            ),
            VoiceQuizQuestion(
                id = "place",
                prompt = locale.getString(R.string.orientation_q_place),
                visual = QuizVisual.Emoji("🏠"),
                correctOption = home,
                answerLabel = home,
                acceptedAnswers = placeWords[0],
                options = quizChoices(home, places),
                speechHint = speechHintOf(placeWords)
            )
        )
    }

    /** English ordinals only; other languages accept the plain number. */
    private fun ordinal(day: Int): String {
        if (locale.language.tag != "en") return day.toString()
        val suffix = when {
            day % 100 in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$day$suffix"
    }
}
