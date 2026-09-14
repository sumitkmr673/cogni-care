package com.example.cognicare.ui.screens.caregiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.ui.components.CaregiverPageHeader
import com.example.cognicare.ui.components.CaregiverScrollPage
import com.example.cognicare.ui.components.DashboardCard
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.components.PlannedFeatureCard
import com.example.cognicare.ui.components.ToneIconBox
import com.example.cognicare.ui.components.labelRes
import com.example.cognicare.ui.theme.CaregiverTheme

@Composable
fun CaregiverSettingsScreen(
    session: AuthSession,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val colors = CaregiverTheme.colors
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    CaregiverScrollPage {
        CaregiverPageHeader(
            eyebrow = stringResource(R.string.settings_eyebrow),
            title = stringResource(R.string.settings_title)
        )

        DashboardCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = session.displayName, tone = colors.mint, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.doctor_name_format, session.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(session.role.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mutedText
                    )
                }
            }
        }

        DashboardCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = { showLanguageDialog = true })
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToneIconBox(Icons.Rounded.Translate, colors.mint)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(currentLanguage.nativeName, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.mutedText)
            }
        }

        PlannedFeatureCard(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.settings_notifications_title),
            body = stringResource(R.string.settings_notifications_body)
        )
        PlannedFeatureCard(
            icon = Icons.Rounded.Lock,
            title = stringResource(R.string.settings_privacy_title),
            body = stringResource(R.string.settings_privacy_body)
        )

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, colors.danger.content),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger.content)
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.action_sign_out), style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                onLanguageSelected(it)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun LanguageDialog(
    current: AppLanguage,
    onDismiss: () -> Unit,
    onSelect: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(
                                selected = language == current,
                                role = Role.RadioButton,
                                onClick = { onSelect(language) }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = language == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(language.nativeName, style = MaterialTheme.typography.bodyLarge)
                            Text(language.englishName, style = MaterialTheme.typography.bodySmall, color = CaregiverTheme.colors.mutedText)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
