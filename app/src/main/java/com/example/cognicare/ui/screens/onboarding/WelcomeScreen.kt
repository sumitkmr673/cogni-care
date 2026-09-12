package com.example.cognicare.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.ui.components.BrandMark
import com.example.cognicare.ui.components.EmphasisHeadline
import com.example.cognicare.ui.components.Eyebrow
import com.example.cognicare.ui.components.LeadText
import com.example.cognicare.ui.components.PatientPrimaryButton
import com.example.cognicare.ui.components.PatientScreen
import com.example.cognicare.ui.components.TextLinkButton
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.OnboardingUiState

private val RingOuter = Color(0xFFF2FAF6)
private val RingMiddle = Color(0xFFECF7F2)
private val RingInner = Color(0xFFE5F3ED)
private val RingStroke = Color(0xFFA8D3C4)
private val ModeLabelBorder = Color(0xFFCFE7DD)

@Composable
fun WelcomeScreen(
    state: OnboardingUiState,
    onStart: () -> Unit,
    onDemoPatient: () -> Unit,
    onDemoCaregiver: () -> Unit
) {
    PatientScreen(languageLabel = state.selectedLanguage?.nativeName) {
        WelcomeVisual(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(20.dp))
        FeatureNote(
            icon = Icons.Rounded.RecordVoiceOver,
            title = stringResource(R.string.welcome_note_voice),
            subtitle = stringResource(R.string.welcome_note_voice_sub)
        )
        Spacer(Modifier.height(10.dp))
        FeatureNote(
            icon = Icons.Rounded.VerifiedUser,
            title = stringResource(R.string.welcome_note_review),
            subtitle = stringResource(R.string.welcome_note_review_sub)
        )
        Spacer(Modifier.height(28.dp))
        Eyebrow(stringResource(R.string.welcome_eyebrow))
        Spacer(Modifier.height(12.dp))
        ModeLabel(stringResource(R.string.welcome_mode))
        Spacer(Modifier.height(14.dp))
        EmphasisHeadline(
            lead = stringResource(R.string.welcome_title),
            emphasis = stringResource(R.string.welcome_emphasis)
        )
        Spacer(Modifier.height(12.dp))
        LeadText(stringResource(R.string.welcome_body))
        Spacer(Modifier.height(28.dp))
        PatientPrimaryButton(
            text = stringResource(R.string.welcome_start),
            onClick = onStart,
            enabled = !state.isStartingDemo
        )
        Spacer(Modifier.height(12.dp))
        if (state.isStartingDemo) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                LeadText(stringResource(R.string.welcome_demo_loading))
            }
        } else {
            TextLinkButton(text = stringResource(R.string.welcome_demo_patient), onClick = onDemoPatient)
            TextLinkButton(text = stringResource(R.string.welcome_demo_caregiver), onClick = onDemoCaregiver)
        }
    }
}

@Composable
private fun WelcomeVisual(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(210.dp)
            .drawBehind {
                val outer = size.minDimension / 2
                drawCircle(RingOuter, radius = outer)
                drawCircle(RingMiddle, radius = outer - 20.dp.toPx())
                drawCircle(RingInner, radius = outer - 40.dp.toPx())
                drawCircle(RingStroke, radius = outer - 40.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            },
        contentAlignment = Alignment.Center
    ) {
        BrandMark(size = 84.dp, container = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun FeatureNote(icon: ImageVector, title: String, subtitle: String) {
    val colors = PatientTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            }
        }
    }
}

@Composable
private fun ModeLabel(text: String) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .clip(shape)
            .background(PatientTheme.colors.selectedContainer)
            .border(1.dp, ModeLabelBorder, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
