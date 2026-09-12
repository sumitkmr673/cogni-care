package com.example.cognicare.viewmodel

import android.content.Context
import com.example.cognicare.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Orientation to time and place, adapted from the MMSE orientation items for use at home
 * (year, month, date, weekday, place). Season and state/county/floor are left out: season
 * is ambiguous across NER climates, and the others don't apply at home.
 *
 * Correct answers come from the device clock. Results are indicators for caregiver trends
 * only; this is not an MMSE administration and nothing here maps to a diagnosis.
 */
@HiltViewModel
class OrientationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceQuizViewModel() {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val now = Calendar.getInstance()
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())

        val year = now.get(Calendar.YEAR).toString()
        val month = symbols.months[now.get(Calendar.MONTH)]
        val date = now.get(Calendar.DAY_OF_MONTH)
        val weekday = symbols.weekdays[now.get(Calendar.DAY_OF_WEEK)]
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

        val home = context.getString(R.string.orientation_place_home)
        val places = listOf(
            home,
            context.getString(R.string.orientation_place_hospital),
            context.getString(R.string.orientation_place_market)
        )
        val nearbyDates = listOf(-3, -2, -1, 1, 2, 3)
            .map { date + it }
            .filter { it in 1..daysInMonth }
            .map(Int::toString)

        return listOf(
            VoiceQuizQuestion(
                id = "year",
                prompt = context.getString(R.string.orientation_q_year),
                visual = QuizVisual.Emoji("📅"),
                correctOption = year,
                answerLabel = year,
                acceptedAnswers = listOf(year),
                options = quizChoices(year, listOf(-2, -1, 1).map { (year.toInt() + it).toString() })
            ),
            VoiceQuizQuestion(
                id = "month",
                prompt = context.getString(R.string.orientation_q_month),
                visual = QuizVisual.Emoji("🗓️"),
                correctOption = month,
                answerLabel = month,
                acceptedAnswers = listOf(month),
                options = quizChoices(month, symbols.months.filter { it.isNotBlank() })
            ),
            VoiceQuizQuestion(
                id = "date",
                prompt = context.getString(R.string.orientation_q_date),
                visual = QuizVisual.Emoji("📆"),
                correctOption = date.toString(),
                answerLabel = ordinal(date),
                acceptedAnswers = listOf(date.toString(), ordinal(date)),
                options = quizChoices(date.toString(), nearbyDates)
            ),
            VoiceQuizQuestion(
                id = "weekday",
                prompt = context.getString(R.string.orientation_q_day),
                visual = QuizVisual.Emoji("☀️"),
                correctOption = weekday,
                answerLabel = weekday,
                acceptedAnswers = listOf(weekday),
                options = quizChoices(weekday, symbols.weekdays.filter { it.isNotBlank() })
            ),
            VoiceQuizQuestion(
                id = "place",
                prompt = context.getString(R.string.orientation_q_place),
                visual = QuizVisual.Emoji("🏠"),
                correctOption = home,
                answerLabel = home,
                acceptedAnswers = listOf(home, "home", "house"),
                options = quizChoices(home, places)
            )
        )
    }

    private fun ordinal(day: Int): String {
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
