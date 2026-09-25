package com.deepkush.reprange.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.viewmodels.PrWithExercise

/** Pure helpers for the per-exercise 1RM-over-time chart (kept UI-free for unit tests). */
object OneRmChart {
    /** Floor fraction so the weakest point stays visible on the chart. */
    const val MIN_FRACTION = 0.15f

    /** Default selection: the top personal record (callers sort best-first). */
    fun defaultSelection(prs: List<PrWithExercise>): String? = prs.firstOrNull()?.exerciseId

    /** Normalizes values to [MIN_FRACTION, 1f] for chart rendering. */
    fun normalized(values: List<Double>): List<Float> {
        if (values.isEmpty()) return emptyList()
        val max = values.max()
        val min = values.min()
        if (max <= 0.0) return values.map { MIN_FRACTION }
        if (max == min) return values.map { 1f }
        val span = max - min
        return values.map { (MIN_FRACTION + (it - min) / span * (1f - MIN_FRACTION)).toFloat() }
    }
}

@Composable
fun ExerciseOneRmSection(
    prs: List<PrWithExercise>,
    selectedId: String?,
    series: List<Pair<Long, Double>>,
    unit: WeightUnit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            "1RM over time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .testTag("one_rm_exercise_row")
                .horizontalScroll(rememberScrollState()),
        ) {
            prs.forEach { pr ->
                FilterChip(
                    selected = pr.exerciseId == selectedId,
                    onClick = { onSelect(pr.exerciseId) },
                    label = { Text(pr.name, maxLines = 1) },
                    modifier = Modifier.testTag("one_rm_chip_${pr.exerciseId}"),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (series.isEmpty()) {
            Text(
                "Log sets for this exercise to see strength progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .testTag("one_rm_empty")
                    .padding(start = 8.dp),
            )
        } else {
            val best = series.maxOf { it.second }
            val latest = series.last().second
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "best ${Formatters.weight(best, unit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "latest ${Formatters.weight(latest, unit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(6.dp))
            OneRmLineChart(series)
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    Formatters.relativeDay(series.first().first),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    Formatters.relativeDay(series.last().first),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun OneRmLineChart(series: List<Pair<Long, Double>>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val dotColor = MaterialTheme.colorScheme.primary
    val fractions = remember(series) { OneRmChart.normalized(series.map { it.second }) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .testTag("one_rm_chart"),
    ) {
        if (fractions.isEmpty()) return@Canvas
        if (fractions.size == 1) {
            drawCircle(
                color = dotColor,
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2f, size.height * (1f - fractions[0])),
            )
            return@Canvas
        }
        val stepX = size.width / (fractions.size - 1)
        val points = fractions.mapIndexed { i, f ->
            Offset(i * stepX, size.height * (1f - f))
        }
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fill = Path().apply {
            moveTo(points[0].x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(fill, fillColor)
        drawPath(
            line,
            lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        points.forEach { drawCircle(dotColor, 3.dp.toPx(), it) }
    }
}
