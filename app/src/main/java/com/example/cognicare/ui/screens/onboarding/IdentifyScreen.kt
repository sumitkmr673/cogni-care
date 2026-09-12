package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.FlowActions
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.OptionRow
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.viewmodel.OnboardingUiState

/** Role selection, presented as the kiosk's "Identify" step. */
@Composable
fun IdentifyScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onContinue: (AppArea) -> Unit
) {
    var choice by rememberSaveable { mutableStateOf<AppArea?>(null) }

    PatientScreen(languageLabel = state.selectedLanguage?.nativeName) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 2)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 3, ONBOARDING_STEP_COUNT, stringResource(R.string.identify_step)))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.identify_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.identify_lead))
        Spacer(Modifier.height(24.dp))
        OptionRow(
            title = stringResource(R.string.identify_patient_title),
            subtitle = stringResource(R.string.identify_patient_body),
            icon = Icons.Rounded.Person,
            selected = choice == AppArea.PATIENT,
            onClick = { choice = AppArea.PATIENT }
        )
        Spacer(Modifier.height(10.dp))
        OptionRow(
            title = stringResource(R.string.identify_caregiver_title),
            subtitle = stringResource(R.string.identify_caregiver_body),
            icon = Icons.Rounded.MedicalServices,
            selected = choice == AppArea.CAREGIVER,
            onClick = { choice = AppArea.CAREGIVER }
        )
        Spacer(Modifier.height(16.dp))
        DemoNote(stringResource(R.string.identify_demo_note))
        FlowActions(
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = { choice?.let(onContinue) },
            primaryEnabled = choice != null,
            onBack = onBack
        )
    }
}
