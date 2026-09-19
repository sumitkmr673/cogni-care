package com.example.cognicare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import com.example.cognicare.viewmodel.MAX_BIRTH_YEAR
import com.example.cognicare.viewmodel.MIN_BIRTH_YEAR
import com.example.cognicare.viewmodel.maxDaysInMonth
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
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
import androidx.compose.ui.unit.sp
import com.example.cognicare.R
import com.example.cognicare.ui.theme.PatientTheme

const val ONBOARDING_STEP_COUNT = 4

/**
 * Inset-aware room at the bottom of every [PatientScreen] so the last content can scroll clear of the
 * floating voice assistant.
 */
val LocalFloatingAssistantInset = staticCompositionLocalOf { 110.dp }

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

/** Native patient screen shell with safe drawing and inset-aware bottom clearance. */
@Composable
fun PatientScreen(
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onMenuClick: (() -> Unit)? = null,
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
            if (showHeader) {
                PatientTopBar(onMenuClick = onMenuClick)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                content = content
            )
            Spacer(Modifier.height(LocalFloatingAssistantInset.current))
        }
    }
}

@Composable
fun PatientTopBar(
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
            if (onMenuClick != null) {
                Surface(
                    onClick = onMenuClick,
                    shape = RoundedCornerShape(12.dp),
                    color = PatientTheme.colors.selectedContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.menu_button_label),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.menu_button_label),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = PatientTheme.colors.divider)
    }
}

@Composable
fun PatientLanguageDialog(
    currentLanguage: com.example.cognicare.core.locale.AppLanguage,
    onLanguageSelected: (com.example.cognicare.core.locale.AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.simple_reg_lang_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.language_dialog_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = PatientTheme.colors.mutedText
                )
                Spacer(Modifier.height(4.dp))
                com.example.cognicare.core.locale.AppLanguage.entries.forEach { language ->
                    OptionRow(
                        title = language.nativeName,
                        subtitle = if (language.isTranslated) {
                            language.englishName.takeIf { it != language.nativeName }
                        } else {
                            stringResource(R.string.language_coming_soon)
                        },
                        selected = if (language.isTranslated) language == currentLanguage else false,
                        enabled = language.isTranslated,
                        onClick = {
                            onLanguageSelected(language)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.background
    )
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
    selected: Boolean? = null,
    enabled: Boolean = true
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val isSelected = selected == true
    val shape = RoundedCornerShape(16.dp)
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        isSelected -> colors.selectedContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = BorderStroke(
        if (isSelected) 2.dp else 1.5.dp,
        if (!enabled) colors.cardBorder.copy(alpha = 0.4f) else if (isSelected) primary else colors.cardBorder
    )
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
                    Icon(icon, contentDescription = null, tint = if (enabled) primary else colors.mutedText, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else colors.mutedText
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.mutedText
                    )
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
                tint = if (!enabled || selected == false) colors.mutedText else primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }

    if (selected == null) {
        Surface(onClick = onClick, enabled = enabled, modifier = rowModifier, shape = shape, color = container, border = border, content = content)
    } else {
        Surface(selected = selected, enabled = enabled, onClick = onClick, modifier = rowModifier, shape = shape, color = container, border = border, content = content)
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
    val cardModifier = modifier.fillMaxWidth().heightIn(min = 110.dp)

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

/**
 * Clean, distraction-free top bar for patient onboarding.
 * Displays an optional back button and "Step X of Y".
 */
@Composable
fun SimpleOnboardingTopBar(
    currentStep: Int? = null,
    totalSteps: Int = 5,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(PatientTheme.dimens.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_go_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(Modifier.size(PatientTheme.dimens.minTouchTarget))
        }

        Spacer(Modifier.weight(1f))

        if (currentStep != null) {
            Text(
                text = stringResource(R.string.step_progress_text, currentStep, totalSteps),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = PatientTheme.colors.mutedText
            )
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(PatientTheme.dimens.minTouchTarget))
    }
}

/**
 * 3-Column Native Compose Date Picker (Day, Month, Year).
 * Features Up/Down arrows (▲/▼), dynamic day clamping, and touch-drag support.
 */
@Composable
fun WheelDatePicker(
    day: Int,
    month: Int,
    year: Int,
    onDateChange: (day: Int, month: Int, year: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val maxDays = maxDaysInMonth(month, year)
        val clampedDay = day.coerceIn(1, maxDays)

        // Day Column
        WheelColumn(
            label = stringResource(R.string.simple_reg_dob_day),
            value = String.format(Locale.getDefault(), "%02d", clampedDay),
            onIncrement = {
                val next = if (clampedDay >= maxDays) 1 else clampedDay + 1
                onDateChange(next, month, year)
            },
            onDecrement = {
                val prev = if (clampedDay <= 1) maxDays else clampedDay - 1
                onDateChange(prev, month, year)
            },
            modifier = Modifier.weight(1f)
        )

        // Month Column
        val clampedMonth = month.coerceIn(1, 12)
        val monthShort = Month.of(clampedMonth).getDisplayName(TextStyle.SHORT, Locale.getDefault())
        WheelColumn(
            label = stringResource(R.string.simple_reg_dob_month),
            value = "$monthShort (${String.format(Locale.getDefault(), "%02d", clampedMonth)})",
            onIncrement = {
                val nextMonth = if (clampedMonth >= 12) 1 else clampedMonth + 1
                val newMax = maxDaysInMonth(nextMonth, year)
                val newDay = minOf(clampedDay, newMax)
                onDateChange(newDay, nextMonth, year)
            },
            onDecrement = {
                val prevMonth = if (clampedMonth <= 1) 12 else clampedMonth - 1
                val newMax = maxDaysInMonth(prevMonth, year)
                val newDay = minOf(clampedDay, newMax)
                onDateChange(newDay, prevMonth, year)
            },
            modifier = Modifier.weight(1.35f)
        )

        // Year Column
        val clampedYear = year.coerceIn(MIN_BIRTH_YEAR, MAX_BIRTH_YEAR)
        WheelColumn(
            label = stringResource(R.string.simple_reg_dob_year),
            value = clampedYear.toString(),
            onIncrement = {
                val nextYear = if (clampedYear >= MAX_BIRTH_YEAR) MIN_BIRTH_YEAR else clampedYear + 1
                val newMax = maxDaysInMonth(clampedMonth, nextYear)
                val newDay = minOf(clampedDay, newMax)
                onDateChange(newDay, clampedMonth, nextYear)
            },
            onDecrement = {
                val prevYear = if (clampedYear <= MIN_BIRTH_YEAR) MAX_BIRTH_YEAR else clampedYear - 1
                val newMax = maxDaysInMonth(clampedMonth, prevYear)
                val newDay = minOf(clampedDay, newMax)
                onDateChange(newDay, clampedMonth, prevYear)
            },
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
fun WheelColumn(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.mutedText
        )
        Spacer(Modifier.height(8.dp))

        // Up arrow button
        Surface(
            onClick = onIncrement,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.iconContainer,
            contentColor = primary,
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Increase $label",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Center highlighted selected box with drag support
        var accumulatedDrag by remember { mutableFloatStateOf(0f) }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        accumulatedDrag += delta
                        if (accumulatedDrag <= -25f) {
                            onIncrement()
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag >= 25f) {
                            onDecrement()
                            accumulatedDrag = 0f
                        }
                    }
                ),
            shape = RoundedCornerShape(14.dp),
            color = colors.selectedContainer,
            border = BorderStroke(2.dp, primary)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Down arrow button
        Surface(
            onClick = onDecrement,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.iconContainer,
            contentColor = primary,
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Decrease $label",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Elderly-friendly bottom sheet menu replacing bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMenuBottomSheet(
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPatientId: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenHelp: () -> Unit,
    onSwitchUser: () -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.menu_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))

            // My Profile
            MenuActionRow(
                title = stringResource(R.string.menu_my_profile),
                icon = Icons.Rounded.Person,
                onClick = {
                    onDismiss()
                    onOpenProfile()
                }
            )

            // My Patient ID
            MenuActionRow(
                title = stringResource(R.string.menu_my_id),
                icon = Icons.Rounded.Badge,
                onClick = {
                    onDismiss()
                    onOpenPatientId()
                }
            )

            // Language
            MenuActionRow(
                title = stringResource(R.string.menu_language),
                icon = Icons.Rounded.Language,
                onClick = {
                    onDismiss()
                    onOpenLanguage()
                }
            )

            // Help
            MenuActionRow(
                title = stringResource(R.string.menu_help),
                icon = Icons.Rounded.HelpOutline,
                onClick = {
                    onDismiss()
                    onOpenHelp()
                }
            )

            HorizontalDivider(color = PatientTheme.colors.divider, modifier = Modifier.padding(vertical = 4.dp))

            // Switch User
            MenuActionRow(
                title = stringResource(R.string.menu_switch_user),
                icon = Icons.AutoMirrored.Rounded.Logout,
                onClick = {
                    onDismiss()
                    onSwitchUser()
                }
            )

            // Sign Out
            MenuActionRow(
                title = stringResource(R.string.menu_sign_out),
                icon = Icons.Rounded.ExitToApp,
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                isDestructive = true
            )
        }
    }
}

@Composable
fun MenuActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val colors = PatientTheme.colors
    val primary = MaterialTheme.colorScheme.primary
    val iconColor = if (isDestructive) MaterialTheme.colorScheme.error else primary
    val textColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDestructive) MaterialTheme.colorScheme.errorContainer else colors.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PatientProfileDialog(
    name: String,
    publicId: String?,
    dateOfBirth: String? = null,
    gender: String? = null,
    languageName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.profile_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileInfoRow(
                    label = stringResource(R.string.profile_field_name),
                    value = name.ifBlank { stringResource(R.string.unspecified) }
                )
                if (!dateOfBirth.isNullOrBlank()) {
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_field_dob),
                        value = dateOfBirth
                    )
                }
                if (!gender.isNullOrBlank()) {
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_field_gender),
                        value = gender
                    )
                }
                ProfileInfoRow(
                    label = stringResource(R.string.profile_field_lang),
                    value = languageName
                )
                if (!publicId.isNullOrBlank()) {
                    ProfileInfoRow(
                        label = stringResource(R.string.simple_reg_id_label),
                        value = publicId
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.background
    )
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PatientTheme.colors.mutedText
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PatientHelpDialog(
    publicId: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.help_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.help_dialog_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!publicId.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PatientTheme.colors.selectedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.simple_reg_id_label).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = publicId,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.background
    )
}

@Composable
fun PatientIdDialog(
    publicId: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = PatientTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.patient_home_id_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.patient_home_id_lead),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.selectedContainer,
                    border = BorderStroke(1.5.dp, primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = publicId,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onCopy()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.patient_id_copy))
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onShare()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.patient_id_share))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close), style = MaterialTheme.typography.labelLarge)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.background
    )
}
