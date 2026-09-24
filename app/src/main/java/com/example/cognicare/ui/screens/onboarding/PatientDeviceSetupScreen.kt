package com.example.cognicare.ui.screens.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.demo.DemoData
import com.example.cognicare.repository.AuthFailure
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.BrandLockup
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.OptionRow
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.ScreenTitle
import com.example.cognicare.ui.components.SimpleOnboardingTopBar
import com.example.cognicare.ui.components.TextLinkButton
import com.example.cognicare.ui.components.WheelDatePicker
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientDeviceSetupViewModel
import com.example.cognicare.viewmodel.PatientSetupStep

/**
 * Patient setup & device binding screen following the Android Simple Mode philosophy:
 * "One screen = one simple decision."
 *
 * 1. WELCOME
 * 2. NAME
 * 3. BIRTH_DATE (Native 3-column wheel picker)
 * 4. GENDER
 * 5. LANGUAGE
 * 6. CONSENT (Concise 3-bullet summary)
 * 7. SUCCESS (Prominent Patient ID, Copy, Share)
 */
@Composable
fun PatientDeviceSetupScreen(
    onBack: () -> Unit,
    onCaregiverSignIn: (() -> Unit)? = null,
    viewModel: PatientDeviceSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val error = state.error

    BackHandler {
        if (!viewModel.onPrevStep()) {
            onBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (state.isCaregiverSetupMode) {
                // Secondary Caregiver / Demo account sign-in mode
                SimpleOnboardingTopBar(onBack = viewModel::toggleSetupMode)
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
                    keyboardActions = KeyboardActions(onDone = { viewModel.submitCaregiver() }),
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
                    onClick = viewModel::submitCaregiver,
                    enabled = state.canSubmitCaregiver,
                    isLoading = state.isSubmitting,
                    showArrow = false
                )
                Spacer(Modifier.height(16.dp))
                TextLinkButton(
                    text = stringResource(R.string.action_simple_registration),
                    onClick = viewModel::toggleSetupMode
                )
                Spacer(Modifier.height(12.dp))
                DemoNote(stringResource(R.string.device_setup_demo_body, DemoData.PATIENT_EMAIL, DemoData.PATIENT_PASSWORD))
                Spacer(Modifier.height(8.dp))
                TextLinkButton(text = stringResource(R.string.action_use_demo), onClick = viewModel::useDemoCredentials)

            } else {
                when (state.step) {
                    PatientSetupStep.WELCOME -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BrandLockup(
                                tagline = stringResource(R.string.brand_tagline_patient),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box {
                                TextButton(
                                    onClick = viewModel::onShowLoginWithIdDialog,
                                    modifier = Modifier.heightIn(min = PatientTheme.dimens.minTouchTarget)
                                ) {
                                    Text(
                                        text = stringResource(R.string.patient_login_with_id_button),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // A Popup renders in its own layer, so its 230dp-wide bubble never
                                // affects this Row's measured size or pushes the button around.
                                if (state.showLoginCoachmark) {
                                    val density = LocalDensity.current
                                    Popup(
                                        alignment = Alignment.TopEnd,
                                        offset = with(density) {
                                            IntOffset(0, PatientTheme.dimens.minTouchTarget.roundToPx())
                                        },
                                        onDismissRequest = viewModel::onDismissLoginCoachmark
                                    ) {
                                        LoginCoachmark(
                                            text = stringResource(R.string.patient_login_with_id_coachmark),
                                            onTap = viewModel::onShowLoginWithIdDialog,
                                            onDismiss = viewModel::onDismissLoginCoachmark
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(52.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(PatientTheme.colors.selectedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(Modifier.height(24.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_welcome_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_welcome_lead))
                        Spacer(Modifier.height(40.dp))

                        PatientPrimaryButton(
                            text = stringResource(R.string.simple_reg_welcome_action),
                            onClick = viewModel::onStartSetup,
                            showArrow = true
                        )
                        Spacer(Modifier.height(24.dp))

                        TextLinkButton(
                            text = stringResource(R.string.simple_reg_caregiver_switch),
                            onClick = {
                                if (onCaregiverSignIn != null) {
                                    onCaregiverSignIn()
                                } else {
                                    viewModel.toggleSetupMode()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PatientSetupStep.NAME -> {
                        SimpleOnboardingTopBar(
                            currentStep = 1,
                            totalSteps = 5,
                            onBack = { viewModel.onPrevStep() }
                        )
                        Spacer(Modifier.height(16.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_name_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_name_lead))
                        Spacer(Modifier.height(28.dp))

                        OutlinedTextField(
                            value = state.patientName,
                            onValueChange = viewModel::onNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.simple_reg_name_label)) },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { viewModel.onNameContinue() }),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.action_continue),
                            onClick = viewModel::onNameContinue,
                            enabled = state.canProceed,
                            showArrow = true
                        )
                    }

                    PatientSetupStep.BIRTH_DATE -> {
                        SimpleOnboardingTopBar(
                            currentStep = 2,
                            totalSteps = 5,
                            onBack = { viewModel.onPrevStep() }
                        )
                        Spacer(Modifier.height(16.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_dob_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_dob_lead))
                        Spacer(Modifier.height(28.dp))

                        WheelDatePicker(
                            day = state.selectedDay,
                            month = state.selectedMonth,
                            year = state.selectedYear,
                            onDateChange = viewModel::onDateChanged
                        )

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.action_continue),
                            onClick = viewModel::onDobContinue,
                            enabled = state.canProceed,
                            showArrow = true
                        )
                    }

                    PatientSetupStep.GENDER -> {
                        SimpleOnboardingTopBar(
                            currentStep = 3,
                            totalSteps = 5,
                            onBack = { viewModel.onPrevStep() }
                        )
                        Spacer(Modifier.height(16.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_gender_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_gender_lead))
                        Spacer(Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OptionRow(
                                title = stringResource(R.string.simple_reg_gender_woman),
                                selected = state.gender == "Woman",
                                onClick = { viewModel.onGenderSelect("Woman") }
                            )
                            OptionRow(
                                title = stringResource(R.string.simple_reg_gender_man),
                                selected = state.gender == "Man",
                                onClick = { viewModel.onGenderSelect("Man") }
                            )
                            OptionRow(
                                title = stringResource(R.string.simple_reg_gender_other),
                                selected = state.gender == "Prefer not to say",
                                onClick = { viewModel.onGenderSelect("Prefer not to say") }
                            )
                        }

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.action_continue),
                            onClick = viewModel::onGenderContinue,
                            enabled = state.canProceed,
                            showArrow = true
                        )
                    }

                    PatientSetupStep.LANGUAGE -> {
                        SimpleOnboardingTopBar(
                            currentStep = 4,
                            totalSteps = 5,
                            onBack = { viewModel.onPrevStep() }
                        )
                        Spacer(Modifier.height(16.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_lang_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_lang_lead))
                        Spacer(Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppLanguage.entries.forEach { language ->
                                OptionRow(
                                    title = language.nativeName,
                                    subtitle = if (language.isTranslated) {
                                        language.englishName.takeIf { it != language.nativeName }
                                    } else {
                                        stringResource(R.string.language_coming_soon)
                                    },
                                    selected = if (language.isTranslated) language == state.selectedLanguage else false,
                                    enabled = language.isTranslated,
                                    onClick = { viewModel.onLanguageSelect(language) }
                                )
                            }
                        }

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.action_continue),
                            onClick = viewModel::onLanguageContinue,
                            enabled = state.canProceed,
                            showArrow = true
                        )
                    }

                    PatientSetupStep.CONSENT -> {
                        SimpleOnboardingTopBar(
                            currentStep = 5,
                            totalSteps = 5,
                            onBack = { viewModel.onPrevStep() }
                        )
                        Spacer(Modifier.height(16.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_consent_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_consent_lead))
                        Spacer(Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ConsentBulletCard(
                                title = stringResource(R.string.simple_reg_consent_games_title),
                                body = stringResource(R.string.simple_reg_consent_games_desc)
                            )
                            ConsentBulletCard(
                                title = stringResource(R.string.simple_reg_consent_team_title),
                                body = stringResource(R.string.simple_reg_consent_team_desc)
                            )
                            ConsentBulletCard(
                                title = stringResource(R.string.simple_reg_consent_choice_title),
                                body = stringResource(R.string.simple_reg_consent_choice_desc)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Large checkbox row
                        Surface(
                            onClick = { viewModel.onConsentToggle(!state.consentAccepted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (state.consentAccepted) PatientTheme.colors.selectedContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (state.consentAccepted) 2.dp else 1.5.dp,
                                if (state.consentAccepted) MaterialTheme.colorScheme.primary else PatientTheme.colors.cardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.consentAccepted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (state.consentAccepted) MaterialTheme.colorScheme.primary else PatientTheme.colors.mutedText,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.simple_reg_consent_checkbox),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (error != null) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(error.toMessageRes()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.simple_reg_lang_finish),
                            onClick = viewModel::onSubmitRegistration,
                            enabled = state.canProceed,
                            isLoading = state.isSubmitting,
                            showArrow = false
                        )
                    }

                    PatientSetupStep.SUCCESS -> {
                        Spacer(Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(PatientTheme.colors.selectedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))

                        ScreenTitle(stringResource(R.string.simple_reg_success_title))
                        Spacer(Modifier.height(8.dp))
                        LeadText(stringResource(R.string.simple_reg_success_lead))
                        Spacer(Modifier.height(24.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = PatientTheme.colors.selectedContainer,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(Modifier.padding(24.dp)) {
                                Text(
                                    text = stringResource(R.string.simple_reg_id_label).uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = state.patientPublicId.orEmpty(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.simple_reg_id_helper),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Patient ID", state.patientPublicId.orEmpty())
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.patient_id_copied),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = PatientTheme.dimens.minTouchTarget),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.patient_id_copy),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    context.getString(R.string.patient_id_share_text, state.patientPublicId.orEmpty())
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, null))
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = PatientTheme.dimens.minTouchTarget),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.patient_id_share),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(36.dp))
                        PatientPrimaryButton(
                            text = stringResource(R.string.simple_reg_start_button),
                            onClick = viewModel::onCompleteSetup,
                            showArrow = true
                        )
                    }
                }
            }
        }
    }

    if (state.isLoginWithIdDialogVisible) {
        LoginWithIdDialog(
            publicId = state.loginPublicId,
            error = state.loginWithIdError,
            isSubmitting = state.isLoggingInWithId,
            canSubmit = state.canSubmitLoginWithId,
            onPublicIdChange = viewModel::onLoginPublicIdChange,
            onSubmit = viewModel::onSubmitLoginWithId,
            onDismiss = viewModel::onDismissLoginWithIdDialog
        )
    }
}

@Composable
private fun LoginCoachmark(
    text: String,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(230.dp), horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .padding(end = 20.dp)
                .size(14.dp)
                .rotate(45f)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
        )
        Surface(
            onClick = onTap,
            modifier = Modifier.offset(y = (-7).dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_go_back),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginWithIdDialog(
    publicId: String,
    error: AuthFailure?,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onPublicIdChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                ScreenTitle(stringResource(R.string.patient_login_with_id_title))
                Spacer(Modifier.height(8.dp))
                LeadText(stringResource(R.string.patient_login_with_id_lead))
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = publicId,
                    onValueChange = onPublicIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.patient_login_with_id_field)) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    shape = RoundedCornerShape(14.dp),
                    textStyle = MaterialTheme.typography.titleLarge
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(error.toMessageRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))
                PatientPrimaryButton(
                    text = stringResource(R.string.patient_login_with_id_submit),
                    onClick = onSubmit,
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                    showArrow = false
                )
                Spacer(Modifier.height(12.dp))
                TextLinkButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ConsentBulletCard(title: String, body: String) {
    val colors = PatientTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, colors.cardBorder)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
