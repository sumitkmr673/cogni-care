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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.PlannedFeatureCard
import com.example.cognicare.ui.components.SafetyBanner
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.viewmodel.CaregiverDashboardViewModel
import com.example.cognicare.viewmodel.LinkPatientStatus
import com.example.cognicare.viewmodel.LinkPatientUiState
import com.example.cognicare.viewmodel.LinkPatientViewModel
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
    viewModel: CaregiverDashboardViewModel = hiltViewModel(),
    linkViewModel: LinkPatientViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val linkState by linkViewModel.uiState.collectAsStateWithLifecycle()
    // A newly linked patient should appear in the list right away, not on the next auto-refresh.
    LaunchedEffect(linkState.status) {
        if (linkState.status is LinkPatientStatus.Linked) viewModel.refresh()
    }
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
                        // What another caregiver or the doctor types to link this patient.
                        patient.publicId?.let { publicId ->
                            Text(
                                text = stringResource(R.string.patients_public_id, publicId),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.mutedText
                            )
                        }
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
        LinkPatientCard(
            state = linkState,
            onInputChange = linkViewModel::onInputChange,
            onLink = linkViewModel::link
        )
    }
}

@Composable
private fun LinkPatientCard(
    state: LinkPatientUiState,
    onInputChange: (String) -> Unit,
    onLink: () -> Unit
) {
    val colors = CaregiverTheme.colors
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.link_patient_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.link_patient_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.mutedText
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.label_patient_id)) },
            placeholder = { Text("PT-AB12CD34") },
            singleLine = true,
            isError = state.isError,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLink() }),
            shape = RoundedCornerShape(12.dp)
        )
        linkStatusMessage(state.status)?.let { message ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onLink,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (state.status == LinkPatientStatus.Linking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.link_patient_submit))
            }
        }
        Spacer(Modifier.height(10.dp))
        // Linking someone into your care still needs their (or their guardian's) consent.
        Text(
            text = stringResource(R.string.patients_add_body),
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText
        )
    }
}

@Composable
private fun linkStatusMessage(status: LinkPatientStatus): String? = when (status) {
    LinkPatientStatus.Idle, LinkPatientStatus.Linking -> null
    is LinkPatientStatus.Linked -> stringResource(R.string.link_patient_success, status.patientName)
    LinkPatientStatus.InvalidId -> stringResource(R.string.link_error_invalid_id)
    LinkPatientStatus.NotFound -> stringResource(R.string.link_error_not_found)
    LinkPatientStatus.AlreadyLinked -> stringResource(R.string.link_error_already_linked)
    LinkPatientStatus.NetworkError -> stringResource(R.string.error_network)
}
