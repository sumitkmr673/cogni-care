package com.example.cognicare.viewmodel

import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLocaleProvider
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.data.model.GameType
import com.example.cognicare.repository.AuthRepository
import com.example.cognicare.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Relationship words for the demo family. Real patients' relations will arrive as caregiver-entered
 * text from the backend, so the model keeps [com.example.cognicare.data.model.FamilyMember.relation]
 * a plain string; only this demo set needs translating.
 */
private val relationLabels = mapOf(
    "grandson" to R.string.relation_grandson,
    "son" to R.string.relation_son,
    "granddaughter" to R.string.relation_granddaughter,
    "daughter-in-law" to R.string.relation_daughter_in_law,
    "husband" to R.string.relation_husband
)

// Romanized Hindi words for the same relations, accepted regardless of app language.
private val relationHindiSynonyms = mapOf(
    "grandson" to listOf("pota"),
    "son" to listOf("beta"),
    "granddaughter" to listOf("poti"),
    "daughter-in-law" to listOf("bahu"),
    "husband" to listOf("pati", "shauhar")
)

/**
 * "Who is this?" using one zoomed-in face at a time from the family photo. Either the name or the
 * relationship counts; for the patient's own face, "me" counts too.
 */
@HiltViewModel
class FamilyIdentificationViewModel @Inject constructor(
    private val locale: AppLocaleProvider,
    careRepository: CareRepository,
    authRepository: AuthRepository
) : VoiceQuizViewModel(careRepository, authRepository, GameType.FAMILY_IDENTIFICATION) {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val members = DemoData.familyMembers
        val names = members.map { it.name }
        return members.shuffled().map { member ->
            val relation = relationLabels[member.relation]
                ?.let { locale.getString(it) }
                ?: member.relation
            val accepted = if (member.isPatient) {
                listOf(member.name, "me", "myself")
            } else {
                // Both words count, and the English and Hindi relation words stay accepted for
                // mixed-language answers.
                listOf(member.name, relation, member.relation) + relationHindiSynonyms[member.relation].orEmpty()
            }
            val answerLabel = if (member.isPatient) {
                locale.getString(R.string.family_answer_self, member.name)
            } else {
                locale.getString(R.string.family_answer_label, member.name, relation)
            }
            VoiceQuizQuestion(
                id = member.name,
                prompt = locale.getString(R.string.family_prompt),
                visual = QuizVisual.PhotoCrop(
                    drawableRes = member.photoRes,
                    left = member.faceLeft,
                    top = member.faceTop,
                    size = member.faceSize
                ),
                correctOption = member.name,
                answerLabel = answerLabel,
                acceptedAnswers = accepted,
                options = quizChoices(member.name, names)
            )
        }
    }
}
