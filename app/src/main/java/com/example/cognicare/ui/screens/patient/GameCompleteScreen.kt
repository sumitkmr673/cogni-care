package com.example.cognicare.ui.screens.patient

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.game.LevelResult
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LargeChoiceButton
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle

/**
 * Encouraging, non-clinical feedback after any game. No raw scores are shown
 * to the patient here — those live only in the caregiver dashboard.
 *
 * After a levelled game ([levelResult] set) the main action is to keep playing: "Play level N"
 * when the patient moved up, otherwise "Play again" — never a promise of a level they did not
 * reach. Talking games have no levels and only offer the way back to the games.
 */
@Composable
fun GameCompleteScreen(
    languageLabel: String,
    onBackToGames: () -> Unit,
    levelResult: LevelResult? = null,
    onPlayNext: (() -> Unit)? = null
) {
    PatientScreen(languageLabel = languageLabel) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconCircle(Icons.Rounded.Celebration, size = 96.dp)
            Spacer(Modifier.height(24.dp))
            ScreenTitle(stringResource(R.string.game_complete_title), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            LeadText(stringResource(R.string.game_complete_body), textAlign = TextAlign.Center)
            if (levelResult?.leveledUp == true) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.game_level_up),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(32.dp))

            if (levelResult != null && onPlayNext != null) {
                PatientPrimaryButton(
                    text = if (levelResult.leveledUp) {
                        stringResource(R.string.game_play_level, levelResult.nextLevel)
                    } else {
                        stringResource(R.string.game_play_again)
                    },
                    onClick = onPlayNext
                )
                Spacer(Modifier.height(12.dp))
                LargeChoiceButton(
                    label = stringResource(R.string.game_back_to_games),
                    onClick = onBackToGames
                )
            } else {
                PatientPrimaryButton(
                    text = stringResource(R.string.game_back_to_games),
                    onClick = onBackToGames,
                    showArrow = false
                )
            }
        }
    }
}
