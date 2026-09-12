package com.example.cognicare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Serif headings over sans body copy, as in the web prototype (Georgia headings, Arial body).
private val Heading = FontFamily.Serif
private val Body = FontFamily.SansSerif

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val CaregiverTypography = Typography(
    displaySmall = style(Heading, FontWeight.Bold, 30, 36, -0.6),
    headlineSmall = style(Heading, FontWeight.Bold, 22, 28, -0.2),
    titleLarge = style(Heading, FontWeight.Bold, 18, 24),
    titleMedium = style(Body, FontWeight.SemiBold, 16, 22),
    titleSmall = style(Body, FontWeight.SemiBold, 14, 20),
    bodyLarge = style(Body, FontWeight.Normal, 16, 24),
    bodyMedium = style(Body, FontWeight.Normal, 14, 21),
    bodySmall = style(Body, FontWeight.Normal, 12, 17),
    labelLarge = style(Body, FontWeight.Bold, 14, 20),
    labelMedium = style(Body, FontWeight.SemiBold, 12, 16),
    labelSmall = style(Body, FontWeight.Bold, 11, 16, 1.3)
)

// Patient sizes start at 16sp and still scale with the system font size.
val PatientTypography = Typography(
    displaySmall = style(Heading, FontWeight.Bold, 34, 40, -0.8),
    headlineMedium = style(Heading, FontWeight.Bold, 30, 36, -0.4),
    headlineSmall = style(Heading, FontWeight.Bold, 24, 30),
    titleLarge = style(Body, FontWeight.Bold, 21, 28),
    titleMedium = style(Body, FontWeight.SemiBold, 19, 26),
    bodyLarge = style(Body, FontWeight.Normal, 19, 28),
    bodyMedium = style(Body, FontWeight.Normal, 17, 24),
    bodySmall = style(Body, FontWeight.Normal, 16, 22),
    labelLarge = style(Body, FontWeight.Bold, 19, 24),
    labelMedium = style(Body, FontWeight.SemiBold, 16, 22),
    labelSmall = style(Body, FontWeight.Bold, 14, 20, 1.2)
)
