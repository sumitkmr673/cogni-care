package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.FlowActions
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.OnboardingUiState

@Composable
fun ConsentScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onAgree: () -> Unit
) {
    var agreed by rememberSaveable { mutableStateOf(state.consentAccepted) }

    PatientScreen(languageLabel = state.selectedLanguage?.nativeName) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 1)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 2, ONBOARDING_STEP_COUNT, stringResource(R.string.consent_step)))
        Spacer(Modifier.height(16.dp))
        IconCircle(Icons.Rounded.Shield)
        Spacer(Modifier.height(16.dp))
        ScreenTitle(stringResource(R.string.consent_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.consent_lead))
        Spacer(Modifier.height(12.dp))
        ConsentItem("01", stringResource(R.string.consent_activity_title), stringResource(R.string.consent_activity_body))
        ConsentItem("02", stringResource(R.string.consent_insights_title), stringResource(R.string.consent_insights_body))
        ConsentItem("03", stringResource(R.string.consent_control_title), stringResource(R.string.consent_control_body))
        Spacer(Modifier.height(20.dp))
        ConsentCheck(checked = agreed, onCheckedChange = { agreed = it })
        FlowActions(
            primaryLabel = stringResource(R.string.consent_agree),
            onPrimary = onAgree,
            primaryEnabled = agreed,
            onBack = onBack
        )
    }
}

@Composable
private fun ConsentItem(number: String, title: String, body: String) {
    val colors = PatientTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = number,
                modifier = Modifier.width(44.dp).clearAndSetSemantics {},
                style = MaterialTheme.typography.labelMedium,
                color = colors.mutedText
            )
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = colors.mutedText)
            }
        }
        HorizontalDivider(color = colors.divider)
    }
}

@Composable
private fun ConsentCheck(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (checked) colors.selectedContainer else MaterialTheme.colorScheme.surface)
            .border(1.5.dp, if (checked) primary else colors.cardBorder, shape)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox)
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.scale(1.3f))
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.consent_agree_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
