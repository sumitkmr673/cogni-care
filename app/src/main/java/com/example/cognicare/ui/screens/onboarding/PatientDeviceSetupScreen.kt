package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.StepProgress
import com.example.cognicare.ui.components.TextLinkButton
import com.example.cognicare.ui.components.onboardingStepLabels
import com.example.cognicare.viewmodel.PatientDeviceSetupViewModel

/**
 * One-time setup, typically done by a caregiver on the patient's behalf: a real sign-in with the
 * patient's own email and password. After that the patient signs in by saying or typing their name.
 */
@Composable
fun PatientDeviceSetupScreen(
    languageLabel: String?,
    onBack: () -> Unit,
    viewModel: PatientDeviceSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val error = state.error

    PatientScreen(languageLabel = languageLabel) {
        StepProgress(steps = onboardingStepLabels(), currentIndex = 3)
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.step_eyebrow, 4, ONBOARDING_STEP_COUNT, stringResource(R.string.step_sign_in)))
        Spacer(Modifier.height(8.dp))
        ScreenTitle(stringResource(R.string.device_setup_title))
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.device_setup_lead))
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_email)) },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_password)) },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                        )
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge
        )

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(error.toMessageRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))
        PatientPrimaryButton(
            text = stringResource(R.string.device_setup_submit),
            onClick = viewModel::submit,
            enabled = state.canSubmit,
            isLoading = state.isSubmitting,
            showArrow = false
        )
        Spacer(Modifier.height(16.dp))
        DemoNote(stringResource(R.string.device_setup_demo_body, DemoData.PATIENT_EMAIL, DemoData.PATIENT_PASSWORD))
        Spacer(Modifier.height(8.dp))
        TextLinkButton(text = stringResource(R.string.action_use_demo), onClick = viewModel::useDemoCredentials)
        Spacer(Modifier.height(12.dp))
        BackTextButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
