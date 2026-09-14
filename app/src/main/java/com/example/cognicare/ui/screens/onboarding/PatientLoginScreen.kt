package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientLoginUiState
import com.example.cognicare.viewmodel.PatientLoginViewModel

@Composable
fun PatientLoginScreen(
    languageLabel: String?,
    onBack: () -> Unit,
    viewModel: PatientLoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PatientScreen(languageLabel = languageLabel) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 3)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 4, ONBOARDING_STEP_COUNT, stringResource(R.string.step_sign_in)))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.patient_login_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.patient_login_lead, PatientLoginUiState.PIN_LENGTH))
        Spacer(Modifier.height(28.dp))
        PinDots(
            entered = state.pin.length,
            total = PatientLoginUiState.PIN_LENGTH,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isVerifying -> CircularProgressIndicator(Modifier.size(36.dp))
                state.showError -> Text(
                    text = stringResource(R.string.patient_login_error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = PatientTheme.colors.accentText,
                    textAlign = TextAlign.Center
                )
            }
        }
        PinKeypad(
            enabled = !state.isVerifying,
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(28.dp))
        DemoNote(stringResource(R.string.patient_login_demo_note, DemoData.PATIENT_PIN))
        Spacer(Modifier.height(12.dp))
        LeadText(stringResource(R.string.patient_login_help))
        Spacer(Modifier.height(12.dp))
        BackTextButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PinDots(entered: Int, total: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.cd_pin_progress, entered, total)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (index < entered) primary else Color.Transparent)
                    .border(3.dp, primary, CircleShape)
            )
        }
    }
}

@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keySize = PatientTheme.dimens.keypadKey
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit -> DigitKey(digit, enabled, onDigit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.size(keySize))
            DigitKey(0, enabled, onDigit)
            val deleteDescription = stringResource(R.string.cd_delete_digit)
            KeypadKey(
                enabled = enabled,
                onClick = onBackspace,
                modifier = Modifier.semantics { contentDescription = deleteDescription }
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, enabled: Boolean, onDigit: (Int) -> Unit) {
    KeypadKey(enabled = enabled, onClick = { onDigit(digit) }) {
        Text(text = digit.toString(), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun KeypadKey(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(PatientTheme.dimens.keypadKey),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(2.dp, PatientTheme.colors.cardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
