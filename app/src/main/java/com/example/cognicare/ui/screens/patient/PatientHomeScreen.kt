package com.example.cognicare.ui.screens.patient

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.core.time.formatFullDate
import com.example.cognicare.core.time.formatTime
import com.example.cognicare.data.model.Reminder
import com.example.cognicare.ui.components.BackTextButton
import com.example.cognicare.ui.components.EmphasisHeadline
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientHelpDialog
import com.example.cognicare.ui.components.PatientIdDialog
import com.example.cognicare.ui.components.PatientLanguageDialog
import com.example.cognicare.ui.components.PatientMenuBottomSheet
import com.example.cognicare.ui.components.PatientProfileDialog
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.greetingLead
import com.example.cognicare.ui.components.icon
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.PatientHomeViewModel

/** Daily Activities & Reminders: calm elderly launcher. */
@Composable
fun PatientHomeScreen(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onOpenGames: () -> Unit,
    onSwitchUser: () -> Unit,
    viewModel: PatientHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showMenuBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showPatientIdDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    PatientScreen(
        onMenuClick = { showMenuBottomSheet = true }
    ) {
        Eyebrow(formatFullDate(System.currentTimeMillis()))
        Spacer(Modifier.height(10.dp))
        EmphasisHeadline(lead = greetingLead(), emphasis = "${state.firstName}.")
        Spacer(Modifier.height(8.dp))
        LeadText(stringResource(R.string.patient_home_lead))
        Spacer(Modifier.height(24.dp))

        PlayGameCard(onClick = onOpenGames)
        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.patient_home_next_reminder).uppercase(),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.isRemindersUnavailable -> {
                EmptyReminderCard(stringResource(R.string.patient_home_reminders_unavailable))
            }
            state.nextReminder != null -> {
                NextReminderCard(reminder = state.nextReminder!!)
            }
            else -> {
                EmptyReminderCard(stringResource(R.string.patient_home_no_reminders))
            }
        }

        if (!state.publicId.isNullOrBlank()) {
            Spacer(Modifier.height(28.dp))
            PatientIdCard(
                publicId = state.publicId.orEmpty(),
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Patient ID", state.publicId)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        context,
                        context.getString(R.string.patient_id_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(R.string.patient_id_share_text, state.publicId)
                        )
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            )
        }

        Spacer(Modifier.height(24.dp))
        BackTextButton(
            onClick = onSwitchUser,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.patient_switch_user, state.firstName),
            icon = Icons.AutoMirrored.Rounded.Logout
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.patient_home_help_text),
            style = MaterialTheme.typography.bodySmall,
            color = PatientTheme.colors.mutedText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showMenuBottomSheet) {
        PatientMenuBottomSheet(
            onDismiss = { showMenuBottomSheet = false },
            onOpenProfile = { showProfileDialog = true },
            onOpenPatientId = { showPatientIdDialog = true },
            onOpenLanguage = { showLanguageDialog = true },
            onOpenHelp = { showHelpDialog = true },
            onSwitchUser = onSwitchUser,
            onSignOut = onSwitchUser
        )
    }

    if (showProfileDialog) {
        PatientProfileDialog(
            name = state.fullName.ifBlank { state.firstName },
            publicId = state.publicId,
            dateOfBirth = state.dateOfBirth,
            gender = state.gender,
            languageName = currentLanguage.nativeName,
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showHelpDialog) {
        PatientHelpDialog(
            publicId = state.publicId,
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showPatientIdDialog && !state.publicId.isNullOrBlank()) {
        PatientIdDialog(
            publicId = state.publicId.orEmpty(),
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Patient ID", state.publicId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    context.getString(R.string.patient_id_copied),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onShare = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(R.string.patient_id_share_text, state.publicId)
                    )
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            },
            onDismiss = { showPatientIdDialog = false }
        )
    }

    if (showLanguageDialog) {
        PatientLanguageDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun EmptyReminderCard(message: String) {
    val colors = PatientTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, colors.cardBorder)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.mutedText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        shadowElevation = 4.dp
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
private fun NextReminderCard(reminder: Reminder) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(reminder.kind.icon, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTime(reminder.scheduledTime),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.mutedText
                )
            }
        }
    }
}

@Composable
private fun PatientIdCard(
    publicId: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.selectedContainer,
        border = BorderStroke(1.5.dp, primary.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.patient_home_id_title).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = publicId,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.patient_home_id_lead),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.mutedText
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = PatientTheme.dimens.minTouchTarget),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = primary
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
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = PatientTheme.dimens.minTouchTarget),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = primary
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
}
