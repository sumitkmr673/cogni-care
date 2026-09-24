package com.example.cognicare.ui.screens.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.data.model.GameType
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.GameOptionCard
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.components.titleRes

@Composable
fun GamesHubScreen(
    onBack: () -> Unit,
    onOpenGame: (GameType) -> Unit
) {
    PatientScreen(showHeader = false) {
        BackTextButton(onClick = onBack)
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.games_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.games_lead))
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val chunkedGames = GameType.entries.chunked(2)
            chunkedGames.forEach { rowGames ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowGames.forEach { game ->
                        GameOptionCard(
                            title = stringResource(game.titleRes),
                            icon = game.icon,
                            onClick = { onOpenGame(game) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowGames.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
