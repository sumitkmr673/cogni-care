package com.example.cognicare.ui.screens.caregiver

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.reminder.ReminderType
import com.example.cognicare.core.reminder.combineDateAndTime
import com.example.cognicare.core.reminder.pickerDateUtcMillis
import com.example.cognicare.core.time.formatWeekdayDateTime
import com.example.cognicare.data.model.ReminderKind
import com.example.cognicare.repository.ManagedReminder
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.LoadingBlock
import com.example.cognicare.ui.components.SectionHeader
import com.example.cognicare.ui.components.ToneIconBox
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.components.tone
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.viewmodel.CaregiverRemindersViewModel
import com.example.cognicare.viewmodel.ReminderMessage
import java.time.Instant
import java.time.ZoneId

/**
 * The web portal's "Upcoming reminders" panel for the app: every reminder for the selected
 * patient with its status and creator, Pause/Resume, Edit and Delete where this caregiver is
 * allowed (primary caregiver or creator), and a form to add or edit one.
 */
@Composable
fun ManageRemindersCard(
    patientId: String,
    viewModel: CaregiverRemindersViewModel = hiltViewModel()
) {
    LaunchedEffect(patientId) { viewModel.show(patientId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardCard(Modifier.fillMaxWidth()) {
        SectionHeader(
            eyebrow = stringResource(R.string.reminders_eyebrow),
            title = stringResource(R.string.reminders_title),
            trailing = {
                Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = CaregiverTheme.colors.mutedText)
            }
        )
        Spacer(Modifier.height(8.dp))

        when {
            state.isLoading -> LoadingBlock()
            state.loadFailed && state.reminders.isEmpty() -> {
                MutedLine(stringResource(R.string.reminders_load_failed))
                TextButton(onClick = viewModel::reload) { Text(stringResource(R.string.reminders_retry)) }
            }
            state.reminders.isEmpty() -> MutedLine(stringResource(R.string.reminders_empty))
            else -> state.reminders.forEachIndexed { index, reminder ->
                if (index > 0) HorizontalDivider(color = CaregiverTheme.colors.border)
                ManagedReminderRow(
                    reminder = reminder,
                    enabled = !state.isBusy,
                    onToggle = { viewModel.toggleActive(reminder) },
                    onEdit = { viewModel.startEdit(reminder) },
                    onDelete = { viewModel.requestDelete(reminder) }
                )
            }
        }

        state.message?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(message.textRes),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = CaregiverTheme.colors.border)
        Spacer(Modifier.height(16.dp))
        ReminderFormSection(
            isEditing = state.editingId != null,
            title = state.form.title,
            type = state.form.type,
            scheduledAt = state.form.scheduledAt,
            busy = state.isBusy,
            onTitleChange = viewModel::onTitleChange,
            onTypeChange = viewModel::onTypeChange,
            onScheduleChange = viewModel::onScheduleChange,
            onSubmit = viewModel::submit,
            onCancel = viewModel::cancelEdit
        )
    }

    state.pendingDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.reminder_delete_title)) },
            text = { Text(stringResource(R.string.reminder_delete_body, reminder.title)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.reminder_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text(stringResource(R.string.reminder_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManagedReminderRow(
    reminder: ManagedReminder,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CaregiverTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        ToneIconBox(reminder.type.kind.icon, reminder.type.kind.tone)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reminder.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                StatusChip(reminder.isActive)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatWeekdayDateTime(reminder.scheduledAt) +
                    if (reminder.isRecurring) " · " + stringResource(R.string.reminder_repeats) else "",
                style = MaterialTheme.typography.bodySmall,
                color = colors.mutedText
            )
            creatorLabel(reminder)?.let { creator ->
                Row {
                    Text(
                        text = stringResource(R.string.reminder_created_by, creator),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (reminder.isMine) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.reminder_you),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            reminder.description?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            if (reminder.canManage) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction(if (reminder.isActive) R.string.reminder_pause else R.string.reminder_resume, enabled, onToggle)
                    SmallAction(R.string.reminder_edit, enabled, onEdit)
                    SmallAction(R.string.reminder_delete, enabled, onDelete, isDestructive = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ReminderFormSection(
    isEditing: Boolean,
    title: String,
    type: ReminderType,
    scheduledAt: Long?,
    busy: Boolean,
    onTitleChange: (String) -> Unit,
    onTypeChange: (ReminderType) -> Unit,
    onScheduleChange: (Long) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(if (isEditing) R.string.reminder_form_edit else R.string.reminder_form_add),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.reminder_title_label)) },
        placeholder = { Text(stringResource(R.string.reminder_title_placeholder)) },
        singleLine = true,
        enabled = !busy
    )
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.reminder_type_label), style = MaterialTheme.typography.labelLarge, color = CaregiverTheme.colors.mutedText)
    Spacer(Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReminderType.entries.forEach { option ->
            FilterChip(
                selected = option == type,
                onClick = { onTypeChange(option) },
                label = { Text(stringResource(option.labelRes)) },
                enabled = !busy
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    // Date first, then the time: two small dialogs are easier than one crowded one.
    var step by rememberSaveable { mutableStateOf(PickerStep.NONE) }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    OutlinedButton(
        onClick = { step = PickerStep.DATE },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(scheduledAt?.let(::formatWeekdayDateTime) ?: stringResource(R.string.reminder_pick_time))
    }

    val initial = scheduledAt ?: System.currentTimeMillis()
    if (step == PickerStep.DATE) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = pickerDateUtcMillis(initial))
        DatePickerDialog(
            onDismissRequest = { step = PickerStep.NONE },
            confirmButton = {
                TextButton(onClick = {
                    pickedDay = dateState.selectedDateMillis ?: pickerDateUtcMillis(initial)
                    step = PickerStep.TIME
                }) { Text(stringResource(R.string.reminder_next)) }
            },
            dismissButton = {
                TextButton(onClick = { step = PickerStep.NONE }) { Text(stringResource(R.string.reminder_cancel)) }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
    if (step == PickerStep.TIME) {
        val local = remember(initial) { Instant.ofEpochMilli(initial).atZone(ZoneId.systemDefault()) }
        val timeState = rememberTimePickerState(initialHour = local.hour, initialMinute = local.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { step = PickerStep.NONE },
            title = { Text(stringResource(R.string.reminder_time_label)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickedDay ?: pickerDateUtcMillis(initial)
                    onScheduleChange(combineDateAndTime(day, timeState.hour, timeState.minute))
                    step = PickerStep.NONE
                }) { Text(stringResource(R.string.reminder_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { step = PickerStep.NONE }) { Text(stringResource(R.string.reminder_cancel)) }
            }
        )
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onSubmit, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(if (isEditing) R.string.reminder_save_button else R.string.reminder_add_button))
        }
        if (isEditing) {
            TextButton(onClick = onCancel, enabled = !busy) { Text(stringResource(R.string.reminder_cancel)) }
        }
    }
}

private enum class PickerStep { NONE, DATE, TIME }

@Composable
private fun StatusChip(active: Boolean) {
    val tone = if (active) CaregiverTheme.colors.mint else CaregiverTheme.colors.slate
    Text(
        text = stringResource(if (active) R.string.reminder_status_active else R.string.reminder_status_paused),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = tone.content
    )
}

@Composable
private fun SmallAction(@StringRes label: Int, enabled: Boolean, onClick: () -> Unit, isDestructive: Boolean = false) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, if (isDestructive) color.copy(alpha = 0.4f) else CaregiverTheme.colors.border),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(stringResource(label), color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MutedLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = CaregiverTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp))
}

/** "Ananya Sharma (Demo) (CG-DEMO0001)", as the web shows it. */
private fun creatorLabel(reminder: ManagedReminder): String? = when {
    reminder.creatorName != null && reminder.creatorPublicId != null -> "${reminder.creatorName} (${reminder.creatorPublicId})"
    else -> reminder.creatorName ?: reminder.creatorPublicId
}

private val ReminderType.kind: ReminderKind
    get() = when (this) {
        ReminderType.GAME -> ReminderKind.GAME
        ReminderType.MEDICATION -> ReminderKind.MEDICATION
        ReminderType.APPOINTMENT -> ReminderKind.APPOINTMENT
        ReminderType.ACTIVITY -> ReminderKind.ACTIVITY
        ReminderType.OTHER -> ReminderKind.OTHER
    }

private val ReminderType.labelRes: Int
    @StringRes get() = when (this) {
        ReminderType.GAME -> R.string.reminder_type_game
        ReminderType.MEDICATION -> R.string.reminder_type_medication
        ReminderType.APPOINTMENT -> R.string.reminder_type_appointment
        ReminderType.ACTIVITY -> R.string.reminder_type_activity
        ReminderType.OTHER -> R.string.reminder_type_other
    }

private val ReminderMessage.textRes: Int
    @StringRes get() = when (this) {
        ReminderMessage.CREATED -> R.string.reminder_msg_created
        ReminderMessage.UPDATED -> R.string.reminder_msg_updated
        ReminderMessage.DELETED -> R.string.reminder_msg_deleted
        ReminderMessage.PAUSED -> R.string.reminder_msg_paused
        ReminderMessage.RESUMED -> R.string.reminder_msg_resumed
        ReminderMessage.NOT_ALLOWED -> R.string.reminder_msg_not_allowed
        ReminderMessage.INVALID -> R.string.reminder_msg_invalid
        ReminderMessage.NOT_FOUND -> R.string.reminder_msg_not_found
        ReminderMessage.FAILED -> R.string.reminder_msg_failed
        ReminderMessage.NEED_TITLE -> R.string.reminder_msg_need_title
        ReminderMessage.TITLE_TOO_LONG -> R.string.reminder_msg_title_too_long
        ReminderMessage.NEED_TIME -> R.string.reminder_msg_need_time
    }
