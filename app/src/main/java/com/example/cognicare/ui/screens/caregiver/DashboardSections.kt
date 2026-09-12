package com.example.cognicare.ui.screens.caregiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.time.formatDayMonth
import com.example.cognicare.core.time.formatWeekdayDateTime
import com.example.cognicare.data.model.DailyScorePoint
import com.example.cognicare.data.model.GamePerformance
import com.example.cognicare.data.model.GameSession
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.LegendDot
import com.example.cognicare.ui.components.SectionHeader
import com.example.cognicare.ui.components.StatusPill
import com.example.cognicare.ui.components.ToneIconBox
import com.example.cognicare.ui.components.TrendLineChart
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.components.labelRes
import com.example.cognicare.ui.components.titleRes
import com.example.cognicare.ui.components.tone
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.Tone
import kotlin.math.roundToInt

@Composable
fun PatientSelectorChip(patient: PatientProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CaregiverTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(name = patient.name, tone = colors.sand)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.label_patient), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                Text(patient.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_change_patient),
                tint = colors.mutedText
            )
        }
    }
}

@Composable
fun PerformanceTrendCard(
    eyebrow: String,
    trend: List<DailyScorePoint>,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = CaregiverTheme.colors
    DashboardCard(modifier.fillMaxWidth()) {
        SectionHeader(eyebrow = eyebrow, title = stringResource(R.string.trend_title), trailing = action)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(colors.memorySeries, stringResource(R.string.domain_memory))
            Spacer(Modifier.width(16.dp))
            LegendDot(colors.attentionSeries, stringResource(R.string.domain_attention))
        }
        Spacer(Modifier.height(16.dp))
        if (trend.size < 2) {
            Text(stringResource(R.string.trend_not_enough), style = MaterialTheme.typography.bodyMedium, color = colors.mutedText)
            return@DashboardCard
        }

        val first = trend.first()
        val last = trend.last()
        val low = trend.minOf { minOf(it.memory, it.attention) }.roundToInt()
        val high = trend.maxOf { maxOf(it.memory, it.attention) }.roundToInt()
        val chartDescription = stringResource(
            R.string.cd_trend_chart,
            first.memory.roundToInt(),
            last.memory.roundToInt(),
            first.attention.roundToInt(),
            last.attention.roundToInt()
        )
        TrendLineChart(
            points = trend,
            memoryColor = colors.memorySeries,
            attentionColor = colors.attentionSeries,
            gridColor = colors.divider,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = chartDescription }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatDayMonth(first.dayStartMillis), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            Text(
                text = stringResource(R.string.trend_range, low, high),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText,
                textAlign = TextAlign.Center
            )
            Text(formatDayMonth(last.dayStartMillis), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
    }
}

@Composable
fun ReminderRow(reminder: Reminder, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToneIconBox(reminder.kind.icon, reminder.kind.tone)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(reminder.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = formatWeekdayDateTime(reminder.scheduledTime),
                style = MaterialTheme.typography.bodySmall,
                color = CaregiverTheme.colors.mutedText
            )
        }
    }
}

@Composable
fun GameSessionRow(session: GameSession, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToneIconBox(session.gameType.icon, session.gameType.tone)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(session.gameType.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatWeekdayDateTime(session.completedAt ?: session.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = CaregiverTheme.colors.mutedText
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(stringResource(session.syncStatus.labelRes), session.syncStatus.tone)
    }
}

@Composable
fun GamePerformanceRow(performance: GamePerformance, modifier: Modifier = Modifier) {
    val colors = CaregiverTheme.colors
    val gameTone = performance.gameType.tone
    Row(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToneIconBox(performance.gameType.icon, Tone(container = gameTone.content, content = Color.White))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(performance.gameType.titleRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.breakdown_row_meta, performance.completedCount, performance.latestLevel),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
            )
        }
        Spacer(Modifier.width(12.dp))
        LinearProgressIndicator(
            progress = { performance.accuracyPercent / 100f },
            modifier = Modifier.width(72.dp).height(6.dp),
            color = colors.mint.content,
            trackColor = colors.mint.container,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.percent_value, performance.accuracyPercent),
            modifier = Modifier.widthIn(min = 40.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
