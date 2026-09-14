package com.example.cognicare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cognicare.R
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.Tone

@Composable
fun CaregiverScrollPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun CaregiverPageHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    emphasis: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth()) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        Eyebrow(eyebrow)
        Spacer(Modifier.height(8.dp))
        if (emphasis != null) EmphasisHeadline(lead = title, emphasis = emphasis) else ScreenTitle(title)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CaregiverTheme.colors.mutedText
            )
        }
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CaregiverTheme.colors.border),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun LinkAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CaregiverTheme.colors.link
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    tone: Tone,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(tone.container),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            style = textStyle,
            color = tone.content,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ToneIconBox(
    icon: ImageVector,
    tone: Tone,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(12.dp)).background(tone.container),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun MetricValue(value: String, unit: String?, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(
            text = value,
            modifier = Modifier.alignByBaseline(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (unit != null) {
            Text(
                text = unit,
                modifier = Modifier.alignByBaseline().padding(start = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = CaregiverTheme.colors.mutedText
            )
        }
    }
}

@Composable
fun StatTile(
    icon: ImageVector,
    tone: Tone,
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
        contentPadding = PaddingValues(16.dp)
    ) {
        ToneIconBox(icon, tone)
        Spacer(Modifier.height(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = CaregiverTheme.colors.mutedText)
        Spacer(Modifier.height(2.dp))
        MetricValue(value, unit)
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = CaregiverTheme.colors.mutedText)
    }
}

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tone.container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = tone.content
    )
}

/** The prototype's "Synthetic demonstration encounter" badge. */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    val colors = CaregiverTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.demo.container)
            .border(1.dp, colors.demoBorder, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(colors.demoDot))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.demo_badge),
            style = MaterialTheme.typography.labelMedium,
            color = colors.demo.content
        )
    }
}

@Composable
fun PlannedFeatureCard(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    DashboardCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            ToneIconBox(icon, CaregiverTheme.colors.mint)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = CaregiverTheme.colors.mutedText)
            }
        }
    }
}

@Composable
fun <T> DividedList(items: List<T>, emptyText: String, row: @Composable (T) -> Unit) {
    if (items.isEmpty()) {
        Text(
            text = emptyText,
            modifier = Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = CaregiverTheme.colors.mutedText
        )
        return
    }
    items.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider(color = CaregiverTheme.colors.divider)
        row(item)
    }
}

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
