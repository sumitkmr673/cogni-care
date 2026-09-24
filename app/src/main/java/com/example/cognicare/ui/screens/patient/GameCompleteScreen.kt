package com.example.cognicare.ui.screens.patient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle

/**
 * Encouraging, non-clinical feedback after any game. No raw scores are shown
 * to the patient here — those live only in the caregiver dashboard.
 */
@Composable
fun GameCompleteScreen(
    onBackToGames: () -> Unit
) {
    PatientScreen(showHeader = false) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            IconCircle(Icons.Rounded.Celebration, size = 96.dp)
            Spacer(Modifier.height(24.dp))
            ScreenTitle(stringResource(R.string.game_complete_title), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            LeadText(stringResource(R.string.game_complete_body), textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            PatientPrimaryButton(
                text = stringResource(R.string.game_back_to_games),
                onClick = onBackToGames,
                showArrow = false
            )
        }
    }
}
