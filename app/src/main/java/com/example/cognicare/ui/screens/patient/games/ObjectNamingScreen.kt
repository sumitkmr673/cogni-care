package com.example.cognicare.ui.screens.patient.games

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.viewmodel.ObjectNamingViewModel

@Composable
fun ObjectNamingScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ObjectNamingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VoiceQuizScreen(
        eyebrow = stringResource(R.string.game_object_naming),
        title = stringResource(R.string.object_naming_title),
        state = state,
        onSpeechResult = viewModel::onSpeechResult,
        onOptionSelected = viewModel::onOptionSelected,
        onBack = onBack,
        onComplete = onComplete
    )
}
