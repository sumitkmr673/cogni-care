package com.example.cognicare.viewmodel

import androidx.annotation.StringRes
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.core.text.SpokenAliases
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private data class NamedObject(
    val emoji: String,
    @StringRes val nameRes: Int,
    /** Key into [SpokenAliases.objects]. */
    val aliasKey: String,
    val synonyms: List<String> = emptyList()
)

// Watch and pencil mirror the MMSE naming item; the rest are everyday objects.
private val namingObjects = listOf(
    NamedObject("⌚", R.string.object_watch, "watch", listOf("wristwatch")),
    NamedObject("✏️", R.string.object_pencil, "pencil"),
    NamedObject("🍎", R.string.object_apple, "apple"),
    NamedObject("🔑", R.string.object_key, "key", listOf("keys")),
    NamedObject("🐶", R.string.object_dog, "dog", listOf("puppy"))
)

@HiltViewModel
class ObjectNamingViewModel @Inject constructor(
    private val locale: AppLocaleProvider,
    careRepository: CareRepository,
    authRepository: AuthRepository
) : VoiceQuizViewModel(careRepository, authRepository, GameType.OBJECT_NAMING) {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val names = namingObjects.map { locale.getString(it.nameRes) }
        // Each object's name in every language, native script and romanised: patients mix
        // languages, so "seb" or "सेब" names the apple even in the English app.
        val spokenNames = namingObjects.map { item ->
            locale.inEveryLanguage(item.nameRes) + item.synonyms + SpokenAliases.objects[item.aliasKey].orEmpty()
        }
        return namingObjects.mapIndexed { index, item ->
            val name = names[index]
            val options = quizChoices(name, names)
            VoiceQuizQuestion(
                id = name,
                prompt = locale.getString(R.string.object_naming_prompt),
                visual = QuizVisual.Emoji(item.emoji),
                correctOption = name,
                answerLabel = name,
                acceptedAnswers = spokenNames[index],
                options = options,
                // Only the choices on screen, all of them alike, so the prompt stays short.
                speechHint = speechHintOf(options.map { spokenNames[names.indexOf(it)] })
            )
        }
    }
}
