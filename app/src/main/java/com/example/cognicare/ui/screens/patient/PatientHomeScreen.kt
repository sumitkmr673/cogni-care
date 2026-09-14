package com.example.cognicare.ui.screens.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.time.formatFullDate
import com.example.cognicare.core.time.formatTime
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.DemoNote
import com.example.cognicare.ui.components.EmphasisHeadline
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.greetingLead
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientHomeViewModel

/** Daily Activities & Reminders: the patient's home screen. */
@Composable
fun PatientHomeScreen(
    languageLabel: String,
    onOpenGames: () -> Unit,
    onSwitchUser: () -> Unit,
    viewModel: PatientHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary

    PatientScreen(languageLabel = languageLabel) {
        Eyebrow(formatFullDate(System.currentTimeMillis()))
        Spacer(Modifier.height(10.dp))
        EmphasisHeadline(lead = greetingLead(), emphasis = "${state.firstName}.")
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.patient_home_lead))
        Spacer(Modifier.height(24.dp))
        PlayGameCard(onClick = onOpenGames)
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.patient_home_today),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (state.todayReminders.isNotEmpty()) {
            val total = state.todayReminders.size
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.patient_home_progress, state.completedCount, total),
                style = MaterialTheme.typography.labelMedium,
                color = primary
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.completedCount.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = primary,
                trackColor = PatientTheme.colors.track,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.todayReminders.isEmpty() -> DemoNote(stringResource(R.string.patient_home_empty))
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.todayReminders.forEach { reminder ->
                    ReminderCard(reminder = reminder, onToggle = { viewModel.toggleReminder(reminder) })
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        BackTextButton(
            onClick = onSwitchUser,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.patient_switch_user, state.firstName),
            icon = Icons.AutoMirrored.Rounded.Logout
        )
    }
}

@Composable
private fun PlayGameCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.patient_home_play_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.patient_home_play_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun ReminderCard(reminder: Reminder, onToggle: () -> Unit) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (reminder.completed) colors.selectedContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (reminder.completed) 2.dp else 1.5.dp,
            color = if (reminder.completed) primary else colors.cardBorder
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (reminder.completed) Color.White else colors.iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(reminder.kind.icon, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatTime(reminder.scheduledTime), style = MaterialTheme.typography.bodyMedium, color = colors.mutedText)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (reminder.completed) {
                OutlinedButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PatientTheme.dimens.minTouchTarget),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, primary),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = primary)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.patient_reminder_done), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                PatientPrimaryButton(
                    text = stringResource(R.string.patient_reminder_mark_done),
                    onClick = onToggle,
                    leadingIcon = Icons.Rounded.Check,
                    showArrow = false,
                    minHeight = PatientTheme.dimens.minTouchTarget
                )
            }
        }
    }
}
