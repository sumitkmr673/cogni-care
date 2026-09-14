package com.example.cognicare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.core.time.DayPart

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = textAlign
    )
}

/** Headline with a teal tail, like the prototype's `<h1>…<em>…</em></h1>`. */
@Composable
fun EmphasisHeadline(
    lead: String,
    emphasis: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall
) {
    val emphasisColor = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            append(lead)
            append(' ')
            withStyle(SpanStyle(color = emphasisColor)) { append(emphasis) }
        },
        modifier = modifier.semantics { heading() },
        style = style,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun StatusDot(color: Color, halo: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(16.dp).clip(CircleShape).background(halo),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun greetingLead(): String = stringResource(
    when (DayPart.at()) {
        DayPart.MORNING -> R.string.greeting_morning
        DayPart.AFTERNOON -> R.string.greeting_afternoon
        DayPart.EVENING -> R.string.greeting_evening
    }
)

fun firstNameOf(fullName: String): String = fullName.trim().substringBefore(' ')

fun initialsOf(name: String): String =
    name.replace(Regex("\\(.*?\\)"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.firstOrNull()?.isLetter() == true }
        .take(2)
        .joinToString("") { it.first().uppercase() }
