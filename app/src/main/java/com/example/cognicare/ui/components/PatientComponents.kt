package com.example.cognicare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.ui.theme.PatientTheme

const val ONBOARDING_STEP_COUNT = 4

/**
 * Extra room at the bottom of every [PatientScreen] so the last content can scroll clear of the
 * floating voice assistant. Zero outside the patient area, where no assistant is shown.
 */
val LocalFloatingAssistantInset = staticCompositionLocalOf { 0.dp }

/**
 * A full-width answer button: the style shared by the talking games' tap answers and the
 * suggestion dialogs, so every "say it or tap it" choice in the app looks and feels the same.
 */
@Composable
fun LargeChoiceButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = PatientTheme.dimens.primaryAction),
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) colors.selectedContainer else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (highlighted) 2.dp else 1.5.dp, if (highlighted) primary else colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (highlighted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
fun onboardingStepLabels(): List<String> = listOf(
    stringResource(R.string.step_language),
    stringResource(R.string.step_consent),
    stringResource(R.string.step_identify),
    stringResource(R.string.step_sign_in)
)

/** Scrollable page with the kiosk header and footer from the web prototype. */
@Composable
fun PatientScreen(
    modifier: Modifier = Modifier,
    languageLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glow = PatientTheme.colors.glow
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val center = Offset(size.width * 0.9f, size.width * 0.3f)
                    val radius = size.width * 0.8f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(glow, Color.Transparent), center, radius),
                        radius = radius,
                        center = center
                    )
                }
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            KioskHeader(languageLabel)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                content = content
            )
            KioskFooter()
            Spacer(Modifier.height(LocalFloatingAssistantInset.current))
        }
    }
}

@Composable
private fun KioskHeader(languageLabel: String?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandLockup(
                tagline = stringResource(R.string.brand_tagline_patient),
                modifier = Modifier.weight(1f)
            )
            if (languageLabel != null) LanguagePill(languageLabel)
        }
        HorizontalDivider(color = PatientTheme.colors.divider)
    }
}

@Composable
private fun KioskFooter() {
    val colors = PatientTheme.colors
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.divider)
        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            Text(stringResource(R.string.footer_help), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.footer_privacy), style = MaterialTheme.typography.bodySmall, color = colors.mutedText)
        }
    }
}

@Composable
fun LeadText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign
    )
}

@Composable
fun StepProgress(steps: List<String>, currentIndex: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = PatientTheme.colors
    val description = stringResource(R.string.cd_step_progress, currentIndex + 1, steps.size, steps[currentIndex])

    Column(modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        LinearProgressIndicator(
            progress = { (currentIndex + 1f) / steps.size },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = primary,
            trackColor = colors.track,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, label ->
                val reached = index <= currentIndex
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (reached) primary else colors.track),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentIndex) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (reached) Color.White else colors.mutedText
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (reached) primary else colors.mutedText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** The kiosk's action bar, stacked for phones: primary action first, then Go back. */
@Composable
fun FlowActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
    note: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = PatientTheme.colors.divider)
        Spacer(Modifier.height(20.dp))
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodyMedium, color = PatientTheme.colors.mutedText)
            Spacer(Modifier.height(12.dp))
        }
        PatientPrimaryButton(text = primaryLabel, onClick = onPrimary, enabled = primaryEnabled)
        if (onBack != null) {
            Spacer(Modifier.height(8.dp))
            BackTextButton(onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PatientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    showArrow: Boolean = true,
    minHeight: Dp = PatientTheme.dimens.primaryAction
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(28.dp), color = LocalContentColor.current, strokeWidth = 3.dp)
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            if (showArrow) {
                Spacer(Modifier.width(12.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun BackTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.action_go_back),
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = PatientTheme.dimens.minTouchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TextLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = PatientTheme.dimens.minTouchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textDecoration = TextDecoration.Underline
        )
    }
}

/**
 * The kiosk's language/identity option. Pass [selected] for a single-choice row,
 * or leave it null for a plain navigation row with a forward arrow.
 */
@Composable
fun OptionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    selected: Boolean? = null
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val isSelected = selected == true
    val shape = RoundedCornerShape(16.dp)
    val container = if (isSelected) colors.selectedContainer else MaterialTheme.colorScheme.surface
    val border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, if (isSelected) primary else colors.cardBorder)
    val rowModifier = modifier.fillMaxWidth().heightIn(min = 84.dp)

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color.White else colors.iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.mutedText)
                }
            }
            Spacer(Modifier.width(12.dp))
            val trailing = when (selected) {
                null -> Icons.AutoMirrored.Rounded.ArrowForward
                true -> Icons.Rounded.CheckCircle
                false -> Icons.Rounded.RadioButtonUnchecked
            }
            Icon(
                imageVector = trailing,
                contentDescription = null,
                tint = if (selected == false) colors.mutedText else primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }

    if (selected == null) {
        Surface(onClick = onClick, modifier = rowModifier, shape = shape, color = container, border = border, content = content)
    } else {
        Surface(selected = selected, onClick = onClick, modifier = rowModifier, shape = shape, color = container, border = border, content = content)
    }
}

@Composable
fun IconCircle(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun DemoNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PatientTheme.colors.noteContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = PatientTheme.colors.mutedText
    )
}

/**
 * A grid-friendly version of OptionRow, designed for displaying games in a grid.
 */
@Composable
fun GameOptionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    val container = MaterialTheme.colorScheme.surface
    val border = BorderStroke(1.5.dp, colors.cardBorder)
    val cardModifier = modifier.fillMaxWidth()

    Surface(onClick = onClick, modifier = cardModifier, shape = shape, color = container, border = border) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.mutedText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
