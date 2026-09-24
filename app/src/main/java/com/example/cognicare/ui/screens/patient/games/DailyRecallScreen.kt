package com.example.cognicare.ui.screens.patient.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.TextLinkButton
import com.example.cognicare.ui.components.VoiceAnswerCapture
import com.example.cognicare.viewmodel.DailyRecallViewModel

/** Open "tell me about your day" reflection. Speaking is optional and nothing is marked right or wrong. */
@Composable
fun DailyRecallScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: DailyRecallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PatientScreen(showHeader = false) {
        BackTextButton(onClick = onBack)
        Spacer(Modifier.height(8.dp))
        Eyebrow(stringResource(R.string.game_daily_recall))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.daily_recall_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.daily_recall_lead))
        Spacer(Modifier.height(24.dp))
        EmojiTile(emoji = "☀️", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))

        VoiceAnswerCapture(onResult = viewModel::onSpeechResult, resetKey = state.transcript.size)

        if (state.transcript.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.daily_recall_you_said),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.transcript.forEach { line ->
                    DemoNote(stringResource(R.string.speech_heard, line))
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        if (state.transcript.isNotEmpty()) {
            PatientPrimaryButton(
                text = stringResource(R.string.daily_recall_done),
                onClick = { viewModel.recordCompletion(); onComplete() },
                showArrow = false
            )
        } else {
            TextLinkButton(
                text = stringResource(R.string.daily_recall_skip),
                onClick = { viewModel.recordCompletion(); onComplete() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
