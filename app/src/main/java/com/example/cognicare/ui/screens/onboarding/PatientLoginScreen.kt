package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.cognicare.ui.components.BrandLockup
import com.example.cognicare.ui.components.IconCircle
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.TextLinkButton
import com.example.cognicare.ui.components.VoiceAnswerCapture
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientLoginUiState
import com.example.cognicare.viewmodel.PatientLoginViewModel

/** Native everyday patient sign-in: say your name or type it. */
@Composable
fun PatientLoginScreen(
    onBack: () -> Unit,
    onSetUpDeviceAgain: () -> Unit,
    viewModel: PatientLoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            BrandLockup(tagline = stringResource(R.string.brand_tagline_patient))
            Spacer(Modifier.height(28.dp))

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

            Spacer(Modifier.height(24.dp))
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        val error = state.error
        when {
            state.isVerifying -> CircularProgressIndicator(Modifier.size(36.dp))
            error != null -> Text(
                text = stringResource(error.toMessageRes()),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
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
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleLarge
    )
    Spacer(Modifier.height(20.dp))
    PatientPrimaryButton(
        text = stringResource(R.string.action_continue),
        onClick = onSubmitTypedName,
        enabled = state.canSubmitTypedName,
        isLoading = state.isVerifying
    )

    Spacer(Modifier.height(28.dp))
    TextLinkButton(
        text = stringResource(R.string.patient_login_setup_again),
        onClick = onSetUpDeviceAgain,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ColumnScope.LockedOutContent(onSetUpDeviceAgain: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    IconCircle(Icons.Rounded.VolunteerActivism, size = 88.dp)
    Spacer(Modifier.height(24.dp))
    ScreenTitle(stringResource(R.string.patient_login_locked_title))
    Spacer(Modifier.height(10.dp))
    LeadText(stringResource(R.string.patient_login_locked_body))
    Spacer(Modifier.height(32.dp))
    PatientPrimaryButton(
        text = stringResource(R.string.patient_login_setup_again),
        onClick = onSetUpDeviceAgain,
        showArrow = true
    )
}
