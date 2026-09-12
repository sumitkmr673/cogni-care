package com.example.cognicare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class PatientDimens(
    val minTouchTarget: Dp = 64.dp,
    val primaryAction: Dp = 72.dp,
    val keypadKey: Dp = 80.dp
)

@Immutable
data class PatientColors(
    val mutedText: Color,
    val cardBorder: Color,
    val divider: Color,
    val selectedContainer: Color,
    val iconContainer: Color,
    val noteContainer: Color,
    val track: Color,
    val glow: Color,
    val accentText: Color
)

private val DefaultPatientColors = PatientColors(
    mutedText = PatientInkMuted,
    cardBorder = PatientCardBorder,
    divider = Divider,
    selectedContainer = Mint50,
    iconContainer = Mint100,
    noteContainer = NoteContainer,
    track = Track,
    glow = Glow,
    accentText = PatientAccentText
)

private val LocalPatientColors = staticCompositionLocalOf { DefaultPatientColors }
private val LocalPatientDimens = staticCompositionLocalOf { PatientDimens() }

// Darker teal than the web kiosk so text and icons clear WCAG AAA on white.
private val PatientColorScheme = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Mint100,
    onPrimaryContainer = Ink900,
    secondary = Clay500,
    onSecondary = Color.White,
    background = Sage50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Sage50,
    onSurfaceVariant = PatientInkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Sage50,
    outline = PatientCardBorder,
    outlineVariant = Divider,
    error = Rose600,
    onError = Color.White
)

object PatientTheme {
    val colors: PatientColors
        @Composable @ReadOnlyComposable
        get() = LocalPatientColors.current

    val dimens: PatientDimens
        @Composable @ReadOnlyComposable
        get() = LocalPatientDimens.current
}

/** Large-type, high-contrast theme for the patient flow and shared onboarding. */
@Composable
fun PatientTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPatientColors provides DefaultPatientColors,
        LocalPatientDimens provides PatientDimens()
    ) {
        MaterialTheme(
            colorScheme = PatientColorScheme,
            typography = PatientTypography,
            content = content
        )
    }
}
