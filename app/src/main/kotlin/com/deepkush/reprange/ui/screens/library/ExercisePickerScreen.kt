package com.deepkush.reprange.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.data.db.ExerciseEntity
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics

/**
 * Resolves the muscle bucket/category used to prefilter the picker for a given
 * row exercise. Returns null when there is nothing to scope by, leaving the
 * picker unscoped (template/session default).
 */
fun bucketForExercise(exercise: ExerciseEntity?): String? =
    exercise?.category?.takeIf { it.isNotBlank() }


@Composable
fun ExercisePickerScreen(
    mode: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: com.deepkush.reprange.viewmodels.LibraryViewModel = hiltViewModel(),
    initialCategory: String? = null,
) {
    val view = LocalView.current
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val seedState by viewModel.seedState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) {
            viewModel.setCategory(initialCategory)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalAppWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalAppWindowInsets.current.only(WindowInsetsSides.Top)))
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable {
                        AppHaptics.tap(view)
                        onBack()
                    }
                    .padding(8.dp),
            )
            Text(
                if (mode == "template") "Add to routine" else "Add to workout",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        OutlinedTextField(
            value = filters.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text("Search exercises…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
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
        }
        if (seedState == DatasetSeeder.SeedState.Loading) {
            Text("Loading dataset…", Modifier.padding(horizontal = 16.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(results, key = { _, e -> e.id }) { _, exercise ->
                ExerciseRow(exercise) {
                    AppHaptics.confirm(view)
                    onSelect(exercise.id)
                }
            }
        }
    }
}
