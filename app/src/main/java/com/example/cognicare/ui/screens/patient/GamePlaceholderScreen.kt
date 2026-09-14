package com.example.cognicare.ui.screens.patient

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.data.model.GameType
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.components.titleRes

/** Stand-in for game modules; real games arrive in the patient-flow step. */
@Composable
fun GamePlaceholderScreen(
    gameType: GameType,
    languageLabel: String,
    onBackToGames: () -> Unit
) {
    PatientScreen(languageLabel = languageLabel) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            IconCircle(gameType.icon, size = 96.dp)
            Spacer(Modifier.height(24.dp))
            Eyebrow(stringResource(R.string.game_placeholder_eyebrow))
            Spacer(Modifier.height(8.dp))
            ScreenTitle(stringResource(gameType.titleRes), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            LeadText(stringResource(R.string.game_placeholder_body), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            PatientPrimaryButton(
                text = stringResource(R.string.game_back_to_games),
                onClick = onBackToGames,
                leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                showArrow = false
            )
        }
    }
}
