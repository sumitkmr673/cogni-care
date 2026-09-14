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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.model.GamePerformance
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.DividedList
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.LoadingBlock
import com.example.cognicare.ui.components.MetricValue
import com.example.cognicare.ui.components.PlannedFeatureCard
import com.example.cognicare.ui.components.SafetyBanner
import com.example.cognicare.ui.components.SectionHeader
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.viewmodel.CaregiverDashboardViewModel
import java.util.Locale

/** Cognitive game performance detail: trend chart plus per-game breakdown. */
@Composable
fun PerformanceScreen(
    onOpenReports: () -> Unit,
    onOpenPatients: () -> Unit,
    viewModel: CaregiverDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = state.dashboard

    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.performance_eyebrow),
            title = stringResource(R.string.performance_title),
            subtitle = stringResource(R.string.dashboard_tagline)
        )
        when {
            dashboard != null -> {
                PatientSelectorChip(dashboard.patient, onClick = onOpenPatients, modifier = Modifier.fillMaxWidth())
                SafetyBanner()
                PatientMetricsCard(dashboard)
                PerformanceTrendCard(
                    eyebrow = stringResource(R.string.performance_history_eyebrow),
                    trend = dashboard.trend
                )
                GameBreakdownCard(dashboard.gamePerformance)
                OutlinedButton(
                    onClick = onOpenReports,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CaregiverTheme.colors.border)
                ) {
                    Icon(Icons.Rounded.Assessment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.action_open_reports), style = MaterialTheme.typography.labelLarge)
                }
            }
            state.isLoading -> LoadingBlock()
            else -> PlannedFeatureCard(
                icon = Icons.Rounded.PersonAdd,
                title = stringResource(R.string.no_patients_title),
                body = stringResource(R.string.no_patients_body)
            )
        }
    }
}

@Composable
private fun PatientMetricsCard(dashboard: PatientDashboard) {
    val colors = CaregiverTheme.colors
    val patient = dashboard.patient
    val language = AppLanguage.fromTag(patient.languageTag)?.englishName ?: patient.languageTag
    val outOf100 = stringResource(R.string.unit_out_of_100)

    DashboardCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(
                name = patient.name,
                tone = colors.sand,
                size = 56.dp,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.performance_profile_meta, language),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mutedText
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.performance_indicator_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.mutedText
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Metric(stringResource(R.string.stat_memory_score), dashboard.memoryScore.toString(), outOf100, Modifier.weight(1f))
            Metric(stringResource(R.string.stat_attention_score), dashboard.attentionScore.toString(), outOf100, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Metric(
                label = stringResource(R.string.stat_game_accuracy),
                value = dashboard.averageAccuracyPercent.toString(),
                unit = stringResource(R.string.unit_percent),
                modifier = Modifier.weight(1f)
            )
            Metric(
                label = stringResource(R.string.stat_response_time),
                value = String.format(Locale.getDefault(), "%.1f", dashboard.averageResponseSeconds),
                unit = stringResource(R.string.unit_seconds),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CaregiverTheme.colors.mutedText)
        Spacer(Modifier.height(2.dp))
        MetricValue(value, unit)
    }
}

@Composable
private fun GameBreakdownCard(performance: List<GamePerformance>) {
    DashboardCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            eyebrow = stringResource(R.string.breakdown_eyebrow),
            title = stringResource(R.string.breakdown_title)
        )
        Spacer(Modifier.height(8.dp))
        DividedList(items = performance, emptyText = stringResource(R.string.breakdown_empty)) { item ->
            GamePerformanceRow(item, Modifier.padding(vertical = 12.dp))
        }
    }
}
