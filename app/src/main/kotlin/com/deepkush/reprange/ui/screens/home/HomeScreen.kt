package com.deepkush.reprange.ui.screens.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.ui.component.SettingsGroup
import com.deepkush.reprange.ui.component.SettingsGroupSpec
import com.deepkush.reprange.ui.component.SettingsItemSpec
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.Formatters
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onStartTemplate: (Long) -> Unit,
    onStartBlank: (Long) -> Unit,
    onEditTemplate: (Long) -> Unit,
    onCreateTemplate: () -> Unit,
    onOpenExercise: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val menuState = com.deepkush.reprange.ui.component.LocalMenuState.current
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.padding(top = 56.dp))
        Text(
            "Reprange",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 8.dp),
        )
        Text(
            "Log it. Beat it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 20.dp),
        )

        // Rapid-start hero: one tap to a blank workout.
        Button(
            onClick = {
                AppHaptics.primaryTap(view)
                viewModel.startBlank { id -> if (id > 0) onStartBlank(id) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = listItemShape(0, 1, radius = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Start empty workout", style = MaterialTheme.typography.titleMedium)
        }

        when (seedState) {
            is DatasetSeeder.SeedState.Loading -> {
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Loading exercise database…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is DatasetSeeder.SeedState.Error -> {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    onClick = { viewModel.retrySeed() },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Couldn't load exercises",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Tap to retry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            else -> {}
        }

        // Plan Ahead: saved routines with one-tap start.
        Text(
            "Routines",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 28.dp, bottom = 8.dp),
        )

        if (templates.isEmpty()) {
            Card(
                shape = listItemShape(0, 1),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("No routines yet", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Build a Push/Pull/Legs split once, then start it in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { AppHaptics.tap(view); onCreateTemplate() }) {
                        Text("Create routine")
                    }
                }
            }
        } else {
            templates.forEachIndexed { index, summary ->
                Card(
                    shape = listItemShape(index, templates.size),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                        .clickable {
                            AppHaptics.primaryTap(view)
                            viewModel.startTemplate(summary.template.id) { id ->
                                if (id != null) onStartTemplate(id)
                            }
                        },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.template.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${summary.exerciseCount} exercise${if (summary.exerciseCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                AppHaptics.longPress(view)
                                menuState.show {
                                    TemplateMenuContent(
                                        templateId = summary.template.id,
                                        title = summary.template.title,
                                        onDismiss = { menuState.dismiss() },
                                        onStart = { id ->
                                            viewModel.startTemplate(id) { started ->
                                                if (started != null) onStartTemplate(started)
                                            }
                                        },
                                        onEdit = onEditTemplate,
                                        onDelete = { viewModel.deleteTemplate(it) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = { AppHaptics.tap(view); onCreateTemplate() },
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        ) {
            Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("New routine")
        }

        // Recent activity for quick progress visibility.
        if (recentSessions.isNotEmpty()) {
            Text(
                "Recent workouts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
            )
            recentSessions.forEachIndexed { index, s ->
                val shape = listItemShape(index, recentSessions.size)
                Card(
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            Icons.Filled.History,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Formatters.relativeDay(s.session.startedAt), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${s.setCount} sets · ${s.exerciseCount} exercises · ${Formatters.volume(s.session.totalVolumeKg, com.deepkush.reprange.constants.WeightUnit.KG)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun TemplateMenuContent(
    templateId: Long,
    title: String,
    onDismiss: () -> Unit,
    onStart: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val view = LocalView.current
    Column(Modifier.padding(bottom = 28.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
        )
        listOf(
            Triple(Icons.Filled.PlayArrow, "Start workout") {
                AppHaptics.primaryTap(view); onDismiss(); onStart(templateId)
            },
            Triple(Icons.Filled.Edit, "Edit routine") {
                AppHaptics.tap(view); onDismiss(); onEdit(templateId)
            },
            Triple(Icons.Filled.Delete, "Delete") {
                AppHaptics.thud(view); onDismiss(); onDelete(templateId)
            },
        ).forEach { (icon, label, action) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = action)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
