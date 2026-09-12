package com.example.cognicare.viewmodel

import android.content.Context
import com.example.cognicare.R
import com.example.cognicare.data.demo.DemoData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * "Who is this?" using one zoomed-in face at a time from the family photo. Either the name or the
 * relationship counts; for the patient's own face, "me" counts too.
 */
@HiltViewModel
class FamilyIdentificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceQuizViewModel() {

    override fun buildQuestions(): List<VoiceQuizQuestion> {
        val members = DemoData.familyMembers
        val names = members.map { it.name }
        return members.shuffled().map { member ->
            val accepted = if (member.isPatient) {
                listOf(member.name, "me", "myself")
            } else {
                listOf(member.name, member.relation)
            }
            val answerLabel = if (member.isPatient) {
                context.getString(R.string.family_answer_self, member.name)
            } else {
                context.getString(R.string.family_answer_label, member.name, member.relation)
            }
            VoiceQuizQuestion(
                id = member.name,
                prompt = context.getString(R.string.family_prompt),
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
