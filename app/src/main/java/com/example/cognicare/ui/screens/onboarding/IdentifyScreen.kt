package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.SubtleTextLink
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.viewmodel.OnboardingUiState

/**
 * Role selection, presented as the kiosk's "Identify" step. The patient path is one big button;
 * the caregiver/doctor path is a small link at the bottom so a patient is unlikely to tap it by mistake.
 */
@Composable
fun IdentifyScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onContinue: (AppArea) -> Unit
) {
    PatientScreen(languageLabel = state.selectedLanguage?.nativeName) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 2)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 3, ONBOARDING_STEP_COUNT, stringResource(R.string.identify_step)))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.identify_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.identify_lead))
        Spacer(Modifier.height(24.dp))

        PatientStartCard(onClick = { onContinue(AppArea.PATIENT) })

        Spacer(Modifier.height(16.dp))
        DemoNote(stringResource(R.string.identify_demo_note))
        Spacer(Modifier.height(16.dp))
        BackTextButton(onClick = onBack, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(40.dp))
        SubtleTextLink(
            text = stringResource(R.string.identify_caregiver_link),
            onClick = { onContinue(AppArea.CAREGIVER) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PatientStartCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.identify_patient_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.identify_patient_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
