package com.example.cognicare.ui.screens.patient.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
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
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatternColor
import com.example.cognicare.viewmodel.PatternPhase
import com.example.cognicare.viewmodel.PatternRecallUiState
import com.example.cognicare.viewmodel.PatternRecallViewModel

@Composable
fun PatternRecallScreen(
    languageLabel: String,
    onComplete: () -> Unit,
    viewModel: PatternRecallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) latestOnComplete()
    }

    PatientScreen(languageLabel = languageLabel) {
        Eyebrow(stringResource(R.string.game_pattern_recall))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.pattern_recall_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.pattern_recall_lead))
        Spacer(Modifier.height(16.dp))
        StatusLine(state)
        Spacer(Modifier.height(24.dp))

        val boardEnabled = state.phase == PatternPhase.INPUT
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ColorPad(PatternColor.RED, state, boardEnabled, viewModel::onColorTap, Modifier.weight(1f))
                ColorPad(PatternColor.GREEN, state, boardEnabled, viewModel::onColorTap, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ColorPad(PatternColor.BLUE, state, boardEnabled, viewModel::onColorTap, Modifier.weight(1f))
                ColorPad(PatternColor.YELLOW, state, boardEnabled, viewModel::onColorTap, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusLine(state: PatternRecallUiState) {
    val textRes = when (state.phase) {
        PatternPhase.SHOWING -> R.string.pattern_recall_watch
        PatternPhase.INPUT -> R.string.pattern_recall_your_turn
        PatternPhase.MISTAKE -> R.string.pattern_recall_try_again
        PatternPhase.ROUND_COMPLETE, PatternPhase.COMPLETE -> R.string.pattern_recall_well_done
    }
    val color = if (state.phase == PatternPhase.MISTAKE) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.game_level, state.level, state.maxLevel),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            color = PatientTheme.colors.mutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            // Rounds come from the level now, not a fixed constant.
            text = stringResource(R.string.pattern_recall_round, minOf(state.round, state.winRound), state.winRound)
                + "  ·  " + stringResource(textRes),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.labelLarge,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorPad(
    color: PatternColor,
    state: PatternRecallUiState,
    enabled: Boolean,
    onTap: (PatternColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHighlighted = state.highlightedColor == color
    val baseColor = colorFor(color)
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.06f else 1f,
        animationSpec = tween(120),
        label = "padPulse"
    )
    val label = stringResource(colorLabelRes(color))

    Surface(
        onClick = { onTap(color) },
        enabled = enabled,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = label }
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = if (isHighlighted) baseColor else baseColor.copy(alpha = if (enabled) 1f else 0.55f)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Text(
                text = label,
                modifier = Modifier.padding(bottom = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

private fun colorFor(color: PatternColor): Color = when (color) {
    PatternColor.RED -> Color(0xFFC85F52)
    PatternColor.GREEN -> Color(0xFF4EAE83)
    PatternColor.BLUE -> Color(0xFF4C7EC9)
    PatternColor.YELLOW -> Color(0xFFCF9D3E)
}

private fun colorLabelRes(color: PatternColor): Int = when (color) {
    PatternColor.RED -> R.string.color_red
    PatternColor.GREEN -> R.string.color_green
    PatternColor.BLUE -> R.string.color_blue
    PatternColor.YELLOW -> R.string.color_yellow
}
