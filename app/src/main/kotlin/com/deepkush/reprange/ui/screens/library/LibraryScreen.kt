package com.deepkush.reprange.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.data.repo.Difficulty
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics
import com.deepkush.reprange.viewmodels.LibraryFilters

private fun difficultyLabel(entity: ExerciseEntity): String =
    Difficulty.fromName(entity.difficulty).label

@Composable
fun LibraryScreen(
    onOpenExercise: (String) -> Unit,
    viewModel: com.deepkush.reprange.viewmodels.LibraryViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(
        filters.query, filters.category, filters.difficulty, filters.equipment,
    ) {
        listState.scrollToItem(0)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
        Text(
            "Library",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
        )

        OutlinedTextField(
            value = filters.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search 1,300+ exercises…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (filters.query.isNotEmpty()) {
                    IconButton(onClick = { AppHaptics.tap(view); viewModel.setQuery("") }) {
                        Icon(Icons.Filled.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        // Filter chips: body part + quick difficulty.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            categories.forEach { category ->
                val selected = filters.category == category
                FilterChip(
                    selected = selected,
                    onClick = {
                        AppHaptics.segmentTick(view)
                        viewModel.setCategory(if (selected) null else category)
                    },
                    label = { Text(category.replaceFirstChar { it.uppercase() }) },
                )
            }
            Spacer(Modifier.width(4.dp))
            Difficulty.entries.forEach { d ->
                val selected = filters.difficulty == d.name
                FilterChip(
                    selected = selected,
                    onClick = {
                        AppHaptics.segmentTick(view)
                        viewModel.setDifficulty(if (selected) null else d.name)
                    },
                    label = { Text(d.label) },
                )
            }
        }

        when (seedState) {
            DatasetSeeder.SeedState.Loading -> FullScreenLoading()
            is DatasetSeeder.SeedState.Error -> SeedError((seedState as DatasetSeeder.SeedState.Error).message) {
                viewModel.retrySeed()
            }

            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(results, key = { _, e -> e.id }) { index, exercise ->
                    ExerciseRow(exercise) {
                        AppHaptics.contextClick(view)
                        onOpenExercise(exercise.id)
                    }
                    if (index == results.lastIndex) Spacer(Modifier.height(140.dp))
                }
            }
        }
    }
}

@Composable
internal fun ExerciseRow(exercise: ExerciseEntity, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = exercise.imageUrl,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    "${exercise.target.replaceFirstChar { it.uppercase() }} · ${exercise.equipment.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                difficultyLabel(exercise),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Fetching the exercise dataset…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SeedError(message: String, onRetryHint: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text("Failed to load dataset", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.TextButton(onClick = onRetryHint) { Text("Retry") }
    }
}
