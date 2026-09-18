package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.repository.MAX_PATIENT_NAME_ATTEMPTS
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.ONBOARDING_STEP_COUNT
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.SubtleTextLink
import com.example.cognicare.ui.components.VoiceAnswerCapture
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientLoginUiState
import com.example.cognicare.viewmodel.PatientLoginViewModel

/** Everyday patient sign-in: say your name with the big speak button, or type it. */
@Composable
fun PatientLoginScreen(
    languageLabel: String?,
    onBack: () -> Unit,
    onSetUpDeviceAgain: () -> Unit,
    viewModel: PatientLoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PatientScreen(languageLabel = languageLabel) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 3)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 4, ONBOARDING_STEP_COUNT, stringResource(R.string.step_sign_in)))
        Spacer(Modifier.height(8.dp))

        if (state.isLockedOut) {
            LockedOutContent(onSetUpDeviceAgain = onSetUpDeviceAgain)
        } else {
            NameEntryContent(
                state = state,
                onSpeechResult = viewModel::onSpeechResult,
                onTypedNameChange = viewModel::onTypedNameChange,
                onSubmitTypedName = viewModel::submitTypedName,
                onSetUpDeviceAgain = onSetUpDeviceAgain
            )
        }

        Spacer(Modifier.height(12.dp))
        BackTextButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ColumnScope.NameEntryContent(
    state: PatientLoginUiState,
    onSpeechResult: (String) -> Unit,
    onTypedNameChange: (String) -> Unit,
    onSubmitTypedName: () -> Unit,
    onSetUpDeviceAgain: () -> Unit
) {
    ScreenTitle(stringResource(R.string.patient_login_title))
    Spacer(Modifier.height(8.dp))
    LeadText(stringResource(R.string.patient_login_lead))
    Spacer(Modifier.height(24.dp))

    VoiceAnswerCapture(onResult = onSpeechResult, enabled = !state.isVerifying)
    state.heardName?.let { heard ->
        Spacer(Modifier.height(8.dp))
        LeadText(
            text = stringResource(R.string.speech_heard, heard),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        val error = state.error
        when {
            state.isVerifying -> CircularProgressIndicator(Modifier.size(36.dp))
            error != null -> Text(
                text = stringResource(error.toMessageRes()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = PatientTheme.colors.accentText,
                textAlign = TextAlign.Center
            )
        }
    }

    Text(
        text = stringResource(R.string.patient_login_type_label),
        style = MaterialTheme.typography.titleMedium,
        color = PatientTheme.colors.mutedText
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.typedName,
        onValueChange = onTypedNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.patient_login_name_field)) },
        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmitTypedName() }),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(16.dp))
    PatientPrimaryButton(
        text = stringResource(R.string.action_continue),
        onClick = onSubmitTypedName,
        enabled = state.canSubmitTypedName,
        isLoading = state.isVerifying
    )

    Spacer(Modifier.height(24.dp))
    DemoNote(stringResource(R.string.patient_login_demo_note, DemoData.PATIENT_NAME))
    Spacer(Modifier.height(12.dp))
    LeadText(stringResource(R.string.patient_login_help))
    Spacer(Modifier.height(8.dp))
    SubtleTextLink(
        text = stringResource(R.string.patient_login_setup_again),
        onClick = onSetUpDeviceAgain,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

/**
 * Shown after [MAX_PATIENT_NAME_ATTEMPTS] unrecognised names. There is no alerts channel to the
 * caregiver yet (no backend endpoint and no push notifications), so the lock is recorded on the
 * device and the patient is asked to fetch their caregiver or doctor in person.
 */
@Composable
private fun ColumnScope.LockedOutContent(onSetUpDeviceAgain: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    IconCircle(Icons.Rounded.VolunteerActivism, size = 88.dp)
    Spacer(Modifier.height(20.dp))
    ScreenTitle(stringResource(R.string.patient_login_locked_title))
    Spacer(Modifier.height(8.dp))
    LeadText(stringResource(R.string.patient_login_locked_body))
    Spacer(Modifier.height(24.dp))
    DemoNote(stringResource(R.string.patient_login_locked_caregiver_note, MAX_PATIENT_NAME_ATTEMPTS))
    Spacer(Modifier.height(12.dp))
    SubtleTextLink(
        text = stringResource(R.string.patient_login_setup_again),
        onClick = onSetUpDeviceAgain,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}
