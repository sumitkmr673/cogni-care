package com.example.cognicare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cognicare.R
import com.example.cognicare.ui.theme.PatientTheme

/**
 * The one dialog for every proactive prompt — "Did you take your medicine?", "Ready to play a
 * memory game?", and whatever comes later. It knows nothing about medication or games: it shows
 * [question], a speak button, and two or three large answers, and reports what was chosen.
 *
 * The patient can answer by voice ([onSpeech]) or by tapping ([onOptionSelected]); the caller
 * maps both to the same outcome. Tapping outside or pressing back calls [onDismiss], which callers
 * treat as a gentle "not now". Nothing here is styled as urgent or as an error.
 *
 * Once answered, pass [acknowledgement] to show a warm one-line reply in place of the question
 * before the caller closes the dialog.
 */
@Composable
fun SuggestionDialog(
    question: String,
    options: List<String>,
    onOptionSelected: (index: Int) -> Unit,
    onSpeech: (String) -> Unit,
    onDismiss: () -> Unit,
    acknowledgement: String? = null,
    notUnderstood: Boolean = false
) {
    require(options.size in 2..3) { "SuggestionDialog shows two or three answers" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (acknowledgement != null) {
                    Acknowledgement(acknowledgement)
                } else {
                    Text(
                        text = question,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    // The same speak button the talking games use — not a second component.
                    VoiceAnswerCapture(onResult = onSpeech, resetKey = question, buttonSize = 140.dp)

                    if (notUnderstood) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.suggestion_not_understood),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodyLarge,
                            color = PatientTheme.colors.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.voice_quiz_or_tap),
                        style = MaterialTheme.typography.titleMedium,
                        color = PatientTheme.colors.mutedText
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEachIndexed { index, label ->
                            LargeChoiceButton(label = label, onClick = { onOptionSelected(index) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Acknowledgement(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}
