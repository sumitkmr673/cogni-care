package com.example.cognicare.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.cognicare.data.model.DailyScorePoint

/** Lightweight two-series line chart; the dashboard step replaces it with Vico. */
@Composable
fun TrendLineChart(
    points: List<DailyScorePoint>,
    memoryColor: Color,
    attentionColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val gridLines = 4
        repeat(gridLines) { index ->
            val y = size.height * index / (gridLines - 1)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (points.size < 2) return@Canvas

        val values = points.flatMap { listOf(it.memory, it.attention) }
        val min = values.min() - 4f
        val max = values.max() + 4f
        drawSeries(points.map { it.attention }, min, max, attentionColor)
        drawSeries(points.map { it.memory }, min, max, memoryColor)
    }
}

private fun DrawScope.drawSeries(values: List<Float>, min: Float, max: Float, color: Color) {
    val range = (max - min).coerceAtLeast(1f)
    val inset = 6.dp.toPx()
    val usableWidth = size.width - inset * 2
    val usableHeight = size.height - inset * 2

    fun pointAt(index: Int) = Offset(
        x = inset + usableWidth * index / values.lastIndex,
        y = inset + usableHeight * (1f - (values[index] - min) / range)
    )

    val path = Path()
    values.indices.forEach { index ->
        val point = pointAt(index)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawCircle(color = color, radius = 4.dp.toPx(), center = pointAt(values.lastIndex))
}
