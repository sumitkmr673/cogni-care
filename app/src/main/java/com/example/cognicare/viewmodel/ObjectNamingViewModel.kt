package com.example.cognicare.viewmodel

import androidx.annotation.StringRes
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private data class NamedObject(val emoji: String, @StringRes val nameRes: Int, val synonyms: List<String> = emptyList())

// Watch and pencil mirror the MMSE naming item; the rest are everyday objects.
private val namingObjects = listOf(
    NamedObject("⌚", R.string.object_watch, listOf("wristwatch")),
    NamedObject("✏️", R.string.object_pencil),
    NamedObject("🍎", R.string.object_apple),
    NamedObject("🔑", R.string.object_key, listOf("keys")),
    NamedObject("🐶", R.string.object_dog, listOf("puppy"))
)

@HiltViewModel
class ObjectNamingViewModel @Inject constructor(
    private val locale: AppLocaleProvider
) : VoiceQuizViewModel() {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val names = namingObjects.map { locale.getString(it.nameRes) }
        return namingObjects.mapIndexed { index, item ->
            val name = names[index]
            VoiceQuizQuestion(
                id = name,
                prompt = locale.getString(R.string.object_naming_prompt),
                visual = QuizVisual.Emoji(item.emoji),
                correctOption = name,
                answerLabel = name,
                // The English synonyms stay accepted so a mixed-language answer still counts.
                acceptedAnswers = listOf(name) + item.synonyms,
                options = quizChoices(name, names)
            )
        }
    }
}
