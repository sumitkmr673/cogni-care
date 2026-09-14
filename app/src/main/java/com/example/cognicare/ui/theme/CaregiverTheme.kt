package com.example.cognicare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Tone(val container: Color, val content: Color)

@Immutable
data class CaregiverColors(
    val border: Color,
    val divider: Color,
    val mutedText: Color,
    val link: Color,
    val snapshotCard: Color,
    val mint: Tone,
    val clay: Tone,
    val lavender: Tone,
    val slate: Tone,
    val sand: Tone,
    val demo: Tone,
    val demoBorder: Color,
    val demoDot: Color,
    val safety: Tone,
    val safetyBorder: Color,
    val success: Tone,
    val danger: Tone,
    val statusDot: Color,
    val statusHalo: Color,
    val memorySeries: Color,
    val attentionSeries: Color
)

private val DefaultCaregiverColors = CaregiverColors(
    border = Border,
    divider = Divider,
    mutedText = Ink500,
    link = Clay700,
    snapshotCard = Teal800,
    mint = Tone(Mint100, Teal600),
    clay = Tone(Clay100, Clay500),
    lavender = Tone(Lavender100, Lavender500),
    slate = Tone(Slate100, Slate500),
    sand = Tone(Sand200, Sand800),
    demo = Tone(Amber50, Amber700),
    demoBorder = AmberBorder,
    demoDot = Amber500,
    safety = Tone(SafetyContainer, SafetyText),
    safetyBorder = SafetyBorder,
    success = Tone(Mint100, Teal700),
    danger = Tone(Rose50, Rose600),
    statusDot = Leaf500,
    statusHalo = Leaf100,
    memorySeries = Clay500,
    attentionSeries = Teal600
)

private val LocalCaregiverColors = staticCompositionLocalOf { DefaultCaregiverColors }

private val CaregiverColorScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Mint100,
    onPrimaryContainer = Ink900,
    secondary = Clay500,
    onSecondary = Color.White,
    secondaryContainer = Clay100,
    onSecondaryContainer = Sand800,
    tertiary = Lavender500,
    onTertiary = Color.White,
    background = Sage100,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Sage100,
    onSurfaceVariant = Ink500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Sage100,
    outline = Border,
    outlineVariant = Divider,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose50,
    onErrorContainer = Rose600
)

object CaregiverTheme {
    val colors: CaregiverColors
        @Composable @ReadOnlyComposable
        get() = LocalCaregiverColors.current
}

/** Denser, data-rich theme for the doctor/caregiver dashboard. */
@Composable
fun CaregiverTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCaregiverColors provides DefaultCaregiverColors) {
        MaterialTheme(
            colorScheme = CaregiverColorScheme,
            typography = CaregiverTypography,
            content = content
        )
    }
}
