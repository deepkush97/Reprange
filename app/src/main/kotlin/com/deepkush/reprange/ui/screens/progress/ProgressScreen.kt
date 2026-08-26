package com.deepkush.reprange.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.constants.WeightUnit
import com.deepkush.reprange.constants.PreferenceKeys
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.utils.listItemShape

@Composable
fun ProgressScreen(
    viewModel: com.deepkush.reprange.viewmodels.ProgressViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val weekly by viewModel.weeklyVolume.collectAsStateWithLifecycle()
    val prs by viewModel.personalRecords.collectAsStateWithLifecycle()
    val (unit, _) = com.deepkush.reprange.utils.rememberEnumPreference(PreferenceKeys.WEIGHT_UNIT, WeightUnit.KG)

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(52.dp))
        Text(
            "Progress",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("This week", Formatters.volume(totals.weekVolumeKg, unit), Modifier.weight(1f))
            StatCard("Sets", "${totals.weekSets}", Modifier.weight(1f))
            StatCard("PRs", "${totals.prCount}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Weekly volume",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        WeeklyVolumeChart(weekly.map { it.volumeKg }, unit)

        if (prs.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Personal records",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            prs.take(12).forEachIndexed { index, pr ->
                Card(
                    shape = listItemShape(index, minOf(prs.size, 12)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp)) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pr.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(
                                "Best ${Formatters.weight(pr.bestWeightKg, unit)} × ${pr.bestReps}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "est. 1RM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                Formatters.weight(pr.bestEst1Rm, unit),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = listItemShape(0, 1),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Complete workouts to unlock PRs and volume trends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyVolumeChart(volumes: List<Double>, unit: WeightUnit) {
    var appear by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (appear) 1f else 0f,
        animationSpec = tween(700),
        label = "chartIn",
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { appear = true }

    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVolume = volumes.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    Box(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            if (volumes.isEmpty()) return@Canvas
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (volumes.size - 1)) / volumes.size
            volumes.forEachIndexed { i, v ->
                val h = (v / maxVolume * size.height * progress).toFloat().coerceAtLeast(if (v > 0) 4f else 2f)
                drawRoundRect(
                    color = if (v > 0) barColor else trackColor,
                    topLeft = Offset(i * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 3f),
                )
            }
        }
        Text(
            "last ${volumes.size} weeks · peak ${Formatters.volume(maxVolume, unit)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 4.dp),
        )
    }
}
