package com.deepkush.reprange.ui.screens.template

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepkush.reprange.data.db.SetStrategy
import com.deepkush.reprange.ui.component.ActionPromptDialog
import com.deepkush.reprange.ui.component.EnumDialog
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.utils.listItemShape
import com.deepkush.reprange.viewmodels.TemplateBuilderViewModel

@Composable
fun TemplateBuilderScreen(
    templateId: Long?,
    onDone: () -> Unit,
    onPickExercise: () -> Unit,
    viewModel: TemplateBuilderViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    var strategyDialogFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var showUnsavedDialog by rememberSaveable { mutableStateOf(false) }
    var showEmptyError by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            AppHaptics.tap(view)
                            showUnsavedDialog = true
                        }
                        .padding(8.dp),
                )
                Text(
                    if (templateId == null) "New routine" else "Edit routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = { Text("Routine name (e.g. Push Day)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = viewModel::setNotes,
                    placeholder = { Text("Notes") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))
                draft.entries.forEachIndexed { index, entry ->
                    Card(
                        shape = listItemShape(index, draft.entries.size),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                            AsyncImage(
                                model = entry.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Sets stepper.
                                    SmallStepButton("−") { AppHaptics.segmentTick(view); viewModel.changeSetCount(index, -1) }
                                    Text(
                                        "${entry.setCount} sets",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                    SmallStepButton("+") { AppHaptics.segmentTick(view); viewModel.changeSetCount(index, +1) }
                                }
                                Text(
                                    buildString {
                                        append(entry.strategy.label)
                                        if (entry.supersetGroup != null) append(" · SS")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable {
                                            AppHaptics.tap(view)
                                            strategyDialogFor = index
                                        },
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.ArrowUpward,
                                    "Move up",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { AppHaptics.dragTick(view); viewModel.move(index, -1) }
                                        .padding(2.dp),
                                )
                                Icon(
                                    Icons.Filled.ArrowDownward,
                                    "Move down",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { AppHaptics.dragTick(view); viewModel.move(index, +1) }
                                        .padding(2.dp),
                                )
                            }
                            Icon(
                                Icons.Filled.Delete,
                                "Remove",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(20.dp)
                                    .clickable {
                                        AppHaptics.thud(view)
                                        viewModel.removeAt(index)
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }

                if (index0(draft.entries.size)) {
                    EmptyHint()
                }

                TextButton(onClick = { AppHaptics.tap(view); onPickExercise() }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add exercise")
                }
                Spacer(Modifier.height(120.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                AppHaptics.confirm(view)
                viewModel.save { saved ->
                    if (saved != null) onDone() else showEmptyError = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = "Save routine", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    strategyDialogFor?.let { index ->
        EnumDialog(
            title = "Set type",
            selected = draft.entries.getOrNull(index)?.strategy ?: SetStrategy.STANDARD,
            options = SetStrategy.entries.toList(),
            labelOf = { it.label },
            onSelect = { AppHaptics.segmentTick(view); viewModel.setStrategy(index, it) },
            onDismiss = { strategyDialogFor = null },
        )
    }
    if (showUnsavedDialog) {
        ActionPromptDialog(
            title = "Leave editor?",
            message = "Unsaved changes will be lost.",
            confirmLabel = "Leave",
            dismissLabel = "Stay",
            onConfirm = onDone,
            onDismiss = { showUnsavedDialog = false },
        )
    }
    if (showEmptyError) {
        ActionPromptDialog(
            title = "Cannot save",
            message = "Give the routine a name and at least one exercise.",
            confirmLabel = "OK",
            onDismiss = { showEmptyError = false },
        )
    }
}

private fun index0(size: Int): Boolean = size == 0

@Composable
private fun SmallStepButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
    ) {
        Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(10.dp))
        Text(
            "Add exercises to build this routine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
