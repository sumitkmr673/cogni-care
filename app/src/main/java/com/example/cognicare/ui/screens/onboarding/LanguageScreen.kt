package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
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

@Composable
fun LanguageScreen(
    state: OnboardingUiState,
    onSelect: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val selected = state.selectedLanguage
    PatientScreen(languageLabel = selected?.nativeName) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 0)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 1, ONBOARDING_STEP_COUNT, stringResource(R.string.step_language)))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.language_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.language_lead))
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { language ->
                OptionRow(
                    title = language.nativeName,
                    subtitle = language.englishName.takeIf { it != language.nativeName },
                    selected = language == selected,
                    onClick = { onSelect(language) }
                )
            }
        }
        FlowActions(
            primaryLabel = stringResource(R.string.action_continue),
            onPrimary = onContinue,
            primaryEnabled = selected != null,
            onBack = onBack,
            note = selected?.let { stringResource(R.string.language_selected, it.nativeName) }
        )
    }
}
