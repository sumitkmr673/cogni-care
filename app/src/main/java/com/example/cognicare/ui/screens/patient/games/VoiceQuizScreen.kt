package com.example.cognicare.ui.screens.patient.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognicare.R
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LargeChoiceButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.VoiceAnswerCapture
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.AnswerFeedback
import com.example.cognicare.viewmodel.QuizVisual
import com.example.cognicare.viewmodel.VoiceQuizQuestion
import kotlin.math.roundToInt
import com.example.cognicare.viewmodel.VoiceQuizUiState

/** Shared layout for the voice games: picture, question, big speak button, tap answers. */
@Composable
fun VoiceQuizScreen(
    eyebrow: String,
    title: String,
    state: VoiceQuizUiState,
    onSpeechResult: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val latestOnComplete by rememberUpdatedState(onComplete)
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) latestOnComplete()
    }
    val question = state.currentQuestion ?: return

    PatientScreen(showHeader = false) {
        BackTextButton(onClick = onBack)
        Spacer(Modifier.height(8.dp))
        Eyebrow(eyebrow)
        Spacer(Modifier.height(8.dp))
        ScreenTitle(title)
        Spacer(Modifier.height(16.dp))
        QuestionProgress(state)
        Spacer(Modifier.height(24.dp))

        when (val visual = question.visual) {
            is QuizVisual.Emoji -> EmojiTile(visual.emoji, Modifier.align(Alignment.CenterHorizontally))
            is QuizVisual.PhotoCrop -> PhotoCropTile(visual, Modifier.align(Alignment.CenterHorizontally))
            null -> Unit
        }
        if (question.visual != null) Spacer(Modifier.height(20.dp))
        Text(
            text = question.prompt,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        VoiceAnswerCapture(
            onResult = onSpeechResult,
            enabled = state.isAwaitingAnswer,
            resetKey = state.questionIndex
        )

        if (state.feedback != AnswerFeedback.NONE) {
            Spacer(Modifier.height(16.dp))
            AnswerFeedbackCard(state = state, question = question)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.voice_quiz_or_tap),
            style = MaterialTheme.typography.titleMedium,
            color = PatientTheme.colors.mutedText
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.forEach { option ->
                AnswerChoice(
                    label = option,
                    isCorrectOption = option == question.correctOption,
                    state = state,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun QuestionProgress(state: VoiceQuizUiState) {
    val total = state.questions.size
    val answered = state.questionIndex + if (state.feedback != AnswerFeedback.NONE) 1 else 0
    Text(
        text = stringResource(R.string.voice_quiz_progress, state.questionIndex + 1, total),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { answered.toFloat() / total },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = PatientTheme.colors.track,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {}
    )
}

@Composable
fun EmojiTile(emoji: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(150.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(PatientTheme.colors.iconContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 84.sp)
    }
}

/** Zooms into one person in a shared family photo so a single face is clear and large. */
@Composable
fun PhotoCropTile(visual: QuizVisual.PhotoCrop, modifier: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(visual.drawableRes)
    val shape = RoundedCornerShape(40.dp)
    val sidePx = (image.width * visual.size).roundToInt().coerceIn(1, minOf(image.width, image.height))
    val leftPx = (image.width * visual.left).roundToInt().coerceIn(0, image.width - sidePx)
    val topPx = (image.height * visual.top).roundToInt().coerceIn(0, image.height - sidePx)

    Canvas(
        modifier = modifier
            .size(220.dp)
            .clip(shape)
            .border(3.dp, MaterialTheme.colorScheme.primary, shape)
    ) {
        drawImage(
            image = image,
            srcOffset = IntOffset(leftPx, topPx),
            srcSize = IntSize(sidePx, sidePx),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
private fun AnswerFeedbackCard(state: VoiceQuizUiState, question: VoiceQuizQuestion) {
    val colors = PatientTheme.colors
    val correct = state.feedback == AnswerFeedback.CORRECT
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (correct) colors.selectedContainer else colors.noteContainer)
            .padding(18.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        state.heardAnswer?.let { heard ->
            Text(
                text = stringResource(R.string.voice_quiz_you_said, heard),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text = if (correct) {
                stringResource(R.string.voice_quiz_feedback_correct)
            } else {
                stringResource(R.string.voice_quiz_feedback_gentle, question.answerLabel)
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AnswerChoice(
    label: String,
    isCorrectOption: Boolean,
    state: VoiceQuizUiState,
    onClick: () -> Unit
) {
    // Styling lives in LargeChoiceButton so the suggestion dialogs match these buttons exactly.
    LargeChoiceButton(
        label = label,
        onClick = onClick,
        enabled = state.isAwaitingAnswer,
        highlighted = state.feedback != AnswerFeedback.NONE && isCorrectOption
    )
}
