package com.example.cognicare.ui.screens.caregiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.repository.LinkPatientFailure
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.PlannedFeatureCard
import com.example.cognicare.ui.components.SafetyBanner
import com.example.cognicare.ui.components.ToneIconBox
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.viewmodel.CaregiverDashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen() {
    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.alerts_eyebrow),
            title = stringResource(R.string.alerts_title),
            subtitle = stringResource(R.string.alerts_subtitle)
        )
        PlannedFeatureCard(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.alerts_planned_title),
            body = stringResource(R.string.alerts_planned_body)
        )
    }
}

@Composable
fun CareSuggestionsScreen() {
    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.suggestions_eyebrow),
            title = stringResource(R.string.suggestions_title),
            subtitle = stringResource(R.string.suggestions_subtitle)
        )
        SafetyBanner(
            title = stringResource(R.string.suggestions_safety_title),
            body = stringResource(R.string.suggestions_safety_body),
            tag = stringResource(R.string.safety_tag_draft)
        )
        PlannedFeatureCard(
            icon = Icons.Rounded.RateReview,
            title = stringResource(R.string.suggestions_review_title),
            body = stringResource(R.string.suggestions_review_body)
        )
        PlannedFeatureCard(
            icon = Icons.Rounded.Lightbulb,
            title = stringResource(R.string.suggestions_planned_title),
            body = stringResource(R.string.suggestions_planned_body)
        )
    }
}

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.reports_eyebrow),
            title = stringResource(R.string.reports_title),
            subtitle = stringResource(R.string.reports_subtitle),
            onBack = onBack
        )
        SafetyBanner()
        PlannedFeatureCard(
            icon = Icons.Rounded.TableChart,
            title = stringResource(R.string.reports_planned_title),
            body = stringResource(R.string.reports_planned_body)
        )
    }
}

@Composable
fun PatientsScreen(
    onBack: () -> Unit,
    viewModel: CaregiverDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = CaregiverTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val selectedId = state.dashboard?.patient?.id
    val scope = rememberCoroutineScope()

    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.patients_eyebrow),
            title = stringResource(R.string.patients_title),
            subtitle = stringResource(R.string.patients_subtitle),
            onBack = onBack
        )
        state.patients.forEach { patient ->
            val isSelected = patient.id == selectedId
            val language = AppLanguage.fromTag(patient.languageTag)?.englishName ?: patient.languageTag
            Surface(
                selected = isSelected,
                onClick = {
                    scope.launch {
                        viewModel.selectPatient(patient.id).join()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) colors.mint.container else MaterialTheme.colorScheme.surface,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) primary else colors.border)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = patient.name, tone = colors.sand, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(patient.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = stringResource(R.string.patients_meta, language, patient.timeZoneId),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.mutedText
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.cd_selected),
                            tint = primary
                        )
                    }
                }
            }
        }
        Surface(
            onClick = viewModel::onShowLinkPatientDialog,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                ToneIconBox(Icons.Rounded.PersonAdd, colors.mint)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.patients_add_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.patients_add_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
                }
            }
        }
    }

    if (state.isLinkPatientDialogVisible) {
        LinkPatientDialog(
            publicId = state.linkPublicId,
            error = state.linkPatientError,
            isSubmitting = state.isLinkingPatient,
            canSubmit = state.canSubmitLinkPatient,
            onPublicIdChange = viewModel::onLinkPublicIdChange,
            onSubmit = viewModel::onSubmitLinkPatient,
            onDismiss = viewModel::onDismissLinkPatientDialog
        )
    }
}

@Composable
private fun LinkPatientDialog(
    publicId: String,
    error: LinkPatientFailure?,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onPublicIdChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        DashboardCard(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.patients_link_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.patients_link_dialog_lead),
                style = MaterialTheme.typography.bodyMedium,
                color = CaregiverTheme.colors.mutedText
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = publicId,
                onValueChange = onPublicIdChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.patients_link_dialog_field)) },
                singleLine = true,
                enabled = !isSubmitting,
                isError = error != null,
                supportingText = error?.let { failure ->
                    {
                        Text(
                            stringResource(
                                when (failure) {
                                    LinkPatientFailure.NOT_FOUND -> R.string.error_patient_link_not_found
                                    LinkPatientFailure.ALREADY_LINKED -> R.string.error_patient_link_already_linked
                                    LinkPatientFailure.NETWORK_ERROR -> R.string.error_network
                                }
                            )
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.patients_link_dialog_submit), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
