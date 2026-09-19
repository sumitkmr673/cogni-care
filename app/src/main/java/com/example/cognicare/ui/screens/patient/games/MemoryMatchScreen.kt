package com.example.cognicare.ui.screens.patient.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.core.game.LevelResult
import com.example.cognicare.viewmodel.MemoryCard
import com.example.cognicare.viewmodel.MemoryMatchViewModel

private const val COLUMNS = 3

@Composable
fun MemoryMatchScreen(
    languageLabel: String,
    onComplete: (LevelResult) -> Unit,
    viewModel: MemoryMatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latestOnComplete by rememberUpdatedState(onComplete)

    LaunchedEffect(state.isSolved) {
        if (state.isSolved) latestOnComplete(state.levelResult)
    }

    PatientScreen(languageLabel = languageLabel) {
        Eyebrow(stringResource(R.string.game_memory_match))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.memory_match_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.memory_match_lead))
        Spacer(Modifier.height(8.dp))
        // The board is dealt once the stored level loads; until then there is nothing to count.
        if (state.isReady) {
            Text(
                text = stringResource(R.string.game_level, state.level, state.maxLevel),
                style = MaterialTheme.typography.labelLarge,
                color = PatientTheme.colors.mutedText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.memory_match_pairs_found, state.matchedPairs, state.totalPairs),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(20.dp))

        // Plain rows, not LazyVerticalGrid: PatientScreen already scrolls vertically, and a lazy
        // grid inside a vertical scroll crashes. Twelve cards don't need lazy layout anyway.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.cards.chunked(COLUMNS).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowCards.forEach { card ->
                        MemoryCardTile(
                            card = card,
                            onClick = { viewModel.onCardClick(card.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(COLUMNS - rowCards.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardTile(card: MemoryCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val revealed = card.isFaceUp || card.isMatched
    // A quick pop-scale on reveal reads as a "flip" without a fragile 3D rotation.
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.86f,
        animationSpec = tween(180),
        label = "cardReveal"
    )
    val faceDescription = if (revealed) card.emoji else stringResource(R.string.cd_face_down_card)

    Surface(
        onClick = onClick,
        enabled = !card.isMatched && !card.isFaceUp,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = faceDescription }
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = when {
            card.isMatched -> colors.selectedContainer
            revealed -> MaterialTheme.colorScheme.surface
            else -> primary
        },
        border = if (revealed) BorderStroke(1.5.dp, colors.cardBorder) else null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (revealed) {
                Text(text = card.emoji, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}
