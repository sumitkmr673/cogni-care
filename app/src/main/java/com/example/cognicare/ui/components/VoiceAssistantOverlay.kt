package com.example.cognicare.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.viewmodel.AssistantNavigation
import com.example.cognicare.viewmodel.VoiceAssistantViewModel

/**
 * Lower-middle of the screen: reachable with either thumb on any phone size, without the stretch
 * to a corner, and low enough that page headings and the main card stay visible above it.
 */
private val ExpandedAssistantAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.62f)

/** Room [PatientScreen] leaves at the bottom so its last content can scroll above the button. */
private val ExpandedAssistantInset = 190.dp

/** Same idea during a game: larger boards (up to 18 cards) can scroll clear of the small robot. */
private val CollapsedAssistantInset = 96.dp

/**
 * Mounted once around the patient NavHost, so it sits above the home screen, the games hub, the
 * games and the completion screen alike.
 *
 * Two states:
 * - **Expanded** (everywhere except a running game): the robot speak button floats in the
 *   lower-middle of the screen.
 * - **Collapsed** (while [isInGame]): it shrinks to a small robot button at the bottom right. Game
 *   boards own the centre of the screen — the Memory Match grid, the Pattern Recall pads and the
 *   talking games' own microphone all live there — so a centred button would sit on the very
 *   thing being tapped. Tapping the small robot opens the assistant as a panel; nothing listens
 *   until the patient asks, so it never competes with a game's own microphone.
 *
 * Suggestion dialogs that fire during a game wait until the game ends rather than interrupt it.
 */
@Composable
fun VoiceAssistantOverlay(
    isInGame: Boolean,
    onOpenGames: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: VoiceAssistantViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnOpenGames by rememberUpdatedState(onOpenGames)
    val latestOnGoHome by rememberUpdatedState(onGoHome)
    var panelOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.navigation.collect { target ->
            when (target) {
                AssistantNavigation.Games -> latestOnOpenGames()
                AssistantNavigation.Home -> latestOnGoHome()
            }
        }
    }
    // Entering or leaving a game resets the in-game panel.
    LaunchedEffect(isInGame) { panelOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalFloatingAssistantInset provides if (isInGame) CollapsedAssistantInset else ExpandedAssistantInset
        ) {
            content()
        }

        if (!isInGame) {
            ExpandedAssistant(
                messageRes = state.assistantMessageRes,
                onSpeech = viewModel::onAssistantSpeech,
                modifier = Modifier.align(ExpandedAssistantAlignment)
            )
        } else {
            // Bottom-right, not mid-right: the Memory Match grid and Pattern Recall pads span the
            // full width, so halfway down the right edge sits on top of cards. Below the board
            // is the one place every game leaves empty.
            CollapsedAssistant(
                onClick = { panelOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(end = 16.dp, bottom = 20.dp)
            )
            if (panelOpen) {
                AssistantPanel(
                    messageRes = state.assistantMessageRes,
                    onSpeech = viewModel::onAssistantSpeech,
                    onClose = {
                        panelOpen = false
                        viewModel.clearAssistantMessage()
                    }
                )
            }
        }

        // Held back during a game; it appears as soon as the patient leaves the game.
        val suggestion = state.suggestion
        if (suggestion != null && !isInGame) {
            val options = suggestion.options.map { stringResource(it.labelRes) }
            SuggestionDialog(
                question = stringResource(suggestion.questionRes),
                options = options,
                onOptionSelected = { index -> viewModel.onSuggestionOption(suggestion.options[index]) },
                onSpeech = viewModel::onSuggestionSpeech,
                onDismiss = viewModel::dismissSuggestion,
                acknowledgement = state.acknowledgementRes?.let { stringResource(it) },
                notUnderstood = state.suggestionNotUnderstood
            )
        }
    }
}

@Composable
private fun ExpandedAssistant(
    messageRes: Int?,
    onSpeech: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val capture = rememberVoiceCapture(onResult = onSpeech)
    val controller = capture.controller
    val status = speechStatusText(controller)
    val bubble = messageRes?.let { stringResource(it) } ?: status

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = bubble != null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            AssistantBubble(bubble.orEmpty())
        }
        SpeakButton(
            isListening = controller.isListening,
            onClick = capture.onSpeakClick,
            size = 124.dp,
            idleIcon = Icons.Rounded.SmartToy
        )
    }
}

@Composable
private fun CollapsedAssistant(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.assistant_open)
    Surface(
        onClick = onClick,
        // 64dp: still the project's minimum touch target, even when minimised.
        modifier = modifier
            .size(64.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.SmartToy, contentDescription = null, modifier = Modifier.size(34.dp))
        }
    }
}

/** The assistant opened from the collapsed robot during a game, over a soft scrim. */
@Composable
private fun BoxScope.AssistantPanel(
    messageRes: Int?,
    onSpeech: (String) -> Unit,
    onClose: () -> Unit
) {
    val capture = rememberVoiceCapture(onResult = onSpeech)
    val controller = capture.controller
    val status = messageRes?.let { stringResource(it) } ?: speechStatusText(controller)

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    )
    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
            .widthIn(max = 420.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpeakButton(
                isListening = controller.isListening,
                onClick = capture.onSpeakClick,
                size = 140.dp,
                idleIcon = Icons.Rounded.SmartToy
            )
            if (status != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.assistant_back_to_game), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 320.dp)
            .padding(bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
