package com.example.cognicare.ui.screens.patient.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.viewmodel.OrientationViewModel

@Composable
fun OrientationScreen(
    languageLabel: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: OrientationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VoiceQuizScreen(
        languageLabel = languageLabel,
        eyebrow = stringResource(R.string.game_orientation),
        title = stringResource(R.string.orientation_title),
        state = state,
        onSpeechResult = viewModel::onSpeechResult,
        onOptionSelected = viewModel::onOptionSelected,
        onBack = onBack,
        onComplete = onComplete
    )
}
