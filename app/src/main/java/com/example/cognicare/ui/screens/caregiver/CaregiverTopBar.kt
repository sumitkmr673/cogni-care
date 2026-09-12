package com.example.cognicare.ui.screens.caregiver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.ui.components.BrandLockup
import com.example.cognicare.ui.components.InitialsAvatar
import com.example.cognicare.ui.theme.CaregiverTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverTopBar(
    session: AuthSession,
    hasUnreadAlerts: Boolean,
    onOpenAlerts: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val colors = CaregiverTheme.colors
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLockup(
                    tagline = stringResource(R.string.brand_tagline_caregiver),
                    modifier = Modifier.weight(1f),
                    markSize = 36.dp
                )
                IconButton(onClick = onOpenAlerts) {
                    BadgedBox(badge = { if (hasUnreadAlerts) Badge(containerColor = colors.link) }) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsNone,
                            contentDescription = stringResource(R.string.cd_open_alerts),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = stringResource(R.string.cd_open_settings),
                            role = Role.Button,
                            onClick = onOpenProfile
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    InitialsAvatar(name = session.displayName, tone = colors.mint, size = 36.dp)
                }
            }
            HorizontalDivider(color = colors.border)
        }
    }
}
