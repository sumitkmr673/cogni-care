package com.example.cognicare.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.cognicare.ui.theme.PatientTheme

/**
 * A deliberately quiet link for actions a patient shouldn't stumble into (the caregiver and doctor
 * paths). Visually small, but it keeps a 48dp touch height so the caregiver can still tap it easily.
 */
@Composable
fun SubtleTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = PatientTheme.colors.mutedText)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center
        )
    }
}
