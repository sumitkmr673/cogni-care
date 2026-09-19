package com.example.cognicare.ui.screens.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.core.time.isSameDay
import com.example.cognicare.data.model.GameSession
import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.DemoBadge
import com.example.cognicare.ui.components.DividedList
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.LinkAction
import com.example.cognicare.ui.components.LoadingBlock
import com.example.cognicare.ui.components.PlannedFeatureCard
import com.example.cognicare.ui.components.SafetyBanner
import com.example.cognicare.ui.components.SectionHeader
import com.example.cognicare.ui.components.StatTile
import com.example.cognicare.ui.components.StatusDot
import com.example.cognicare.ui.components.firstNameOf
import com.example.cognicare.ui.components.greetingLead
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.Sand200
import com.example.cognicare.viewmodel.CaregiverDashboardViewModel

@Composable
fun CaregiverOverviewScreen(
    onOpenPerformance: () -> Unit,
    onOpenPatients: () -> Unit,
    viewModel: CaregiverDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboard = state.dashboard

    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.overview_eyebrow),
            title = greetingLead(),
            emphasis = firstNameOf(state.caregiverName),
            subtitle = stringResource(R.string.dashboard_tagline)
        )
        DemoBadge()
        when {
            dashboard != null -> {
                PatientSelectorChip(dashboard.patient, onClick = onOpenPatients, modifier = Modifier.fillMaxWidth())
                PatientSnapshotCard(dashboard, onOpenPerformance)
                OverviewStats(dashboard)
                PerformanceTrendCard(
                    eyebrow = stringResource(R.string.overview_trend_eyebrow),
                    trend = dashboard.trend,
                    action = { LinkAction(stringResource(R.string.action_details), onOpenPerformance) }
                )
                ManageRemindersCard(patientId = dashboard.patient.id)
                RecentSessionsCard(dashboard.recentSessions, onSeeAll = onOpenPerformance)
                SafetyBanner()
            }
            state.isLoading -> LoadingBlock()
            // A new caregiver has no patient yet, and the patient chip above (normally the way to
            // the Patients screen) only exists once there is one — so this card must lead there.
            else -> NoPatientsCard(onLinkPatient = onOpenPatients)
        }
    }
}

@Composable
private fun NoPatientsCard(onLinkPatient: () -> Unit) {
    DashboardCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.no_patients_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_patients_body),
            style = MaterialTheme.typography.bodyMedium,
            color = CaregiverTheme.colors.mutedText
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onLinkPatient,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(R.string.link_patient_title))
        }
    }
}

@Composable
private fun PatientSnapshotCard(dashboard: PatientDashboard, onOpenPerformance: () -> Unit) {
    val colors = CaregiverTheme.colors
    val patient = dashboard.patient
    val language = AppLanguage.fromTag(patient.languageTag)?.englishName ?: patient.languageTag
    val syncedToday = dashboard.lastSyncedAt?.let { isSameDay(it, System.currentTimeMillis()) } == true
    val ring = Color.White.copy(alpha = 0.07f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.snapshotCard)
            .drawBehind {
                val center = Offset(size.width * 0.92f, size.height * 0.22f)
                drawCircle(ring, radius = size.height * 0.9f, center = center, style = Stroke(width = 26.dp.toPx()))
                drawCircle(ring, radius = size.height * 0.5f, center = center)
            }
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(stringResource(R.string.snapshot_eyebrow), color = Color.White.copy(alpha = 0.75f))
                    Spacer(Modifier.height(8.dp))
                    Text(patient.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.snapshot_meta, language, patient.timeZoneId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                InitialsAvatar(
                    name = patient.name,
                    tone = colors.sand,
                    modifier = Modifier.rotate(-6f),
                    size = 64.dp,
                    shape = RoundedCornerShape(18.dp),
                    textStyle = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(
                    color = if (syncedToday) colors.statusDot else colors.demoDot,
                    halo = Color.White.copy(alpha = 0.18f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (syncedToday) R.string.sync_synced_today else R.string.sync_waiting),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                LinkAction(stringResource(R.string.action_view_performance), onOpenPerformance, color = Sand200)
            }
        }
    }
}

@Composable
private fun OverviewStats(dashboard: PatientDashboard) {
    val colors = CaregiverTheme.colors
    val outOf100 = stringResource(R.string.unit_out_of_100)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                icon = Icons.Rounded.SportsEsports,
                tone = colors.mint,
                label = stringResource(R.string.stat_games_completed),
                value = dashboard.gamesCompletedToday.toString(),
                unit = stringResource(R.string.unit_today),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatTile(
                icon = Icons.Rounded.Insights,
                tone = colors.slate,
                label = stringResource(R.string.stat_average_accuracy),
                value = dashboard.averageAccuracyPercent.toString(),
                unit = stringResource(R.string.unit_percent),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                icon = Icons.Rounded.Psychology,
                tone = colors.clay,
                label = stringResource(R.string.stat_memory_score),
                value = dashboard.memoryScore.toString(),
                unit = outOf100,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatTile(
                icon = Icons.Rounded.CenterFocusStrong,
                tone = colors.lavender,
                label = stringResource(R.string.stat_attention_score),
                value = dashboard.attentionScore.toString(),
                unit = outOf100,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<GameSession>, onSeeAll: () -> Unit) {
    DashboardCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            eyebrow = stringResource(R.string.recent_eyebrow),
            title = stringResource(R.string.recent_title),
            trailing = { LinkAction(stringResource(R.string.action_see_all), onSeeAll) }
        )
        Spacer(Modifier.height(8.dp))
        DividedList(items = sessions, emptyText = stringResource(R.string.recent_empty)) { session ->
            GameSessionRow(session, Modifier.padding(vertical = 12.dp))
        }
    }
}
