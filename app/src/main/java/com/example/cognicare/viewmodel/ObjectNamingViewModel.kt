package com.example.cognicare.viewmodel

import androidx.annotation.StringRes
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private data class NamedObject(val emoji: String, @StringRes val nameRes: Int, val synonyms: List<String> = emptyList())

// Watch and pencil mirror the MMSE naming item; the rest are everyday objects. Romanized
// regional-language words are accepted alongside English and the app's own language string, so a
// patient who answers in Hindi/Bengali/Assamese still counts as correct regardless of app language.
private val namingObjects = listOf(
    NamedObject("⌚", R.string.object_watch, listOf("wristwatch", "ghadi", "ghari")),
    NamedObject("✏️", R.string.object_pencil),
    NamedObject("🍎", R.string.object_apple, listOf("seb", "saib")),
    // "chabi"/"chaabi" is the common Hindi/Urdu romanization for "key".
    NamedObject("🔑", R.string.object_key, listOf("keys", "chabi", "chaabi")),
    // "kutta" (Hindi) and "kukur" (Bengali/Assamese) both mean dog.
    NamedObject("🐶", R.string.object_dog, listOf("puppy", "kutta", "kutte", "kukur"))
)

@HiltViewModel
class ObjectNamingViewModel @Inject constructor(
    private val locale: AppLocaleProvider,
    careRepository: CareRepository,
    authRepository: AuthRepository
) : VoiceQuizViewModel(careRepository, authRepository, GameType.OBJECT_NAMING) {

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
