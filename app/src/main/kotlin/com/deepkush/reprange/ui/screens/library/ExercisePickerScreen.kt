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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepkush.reprange.data.repo.DatasetSeeder
import com.deepkush.reprange.ui.component.LocalAppWindowInsets
import com.deepkush.reprange.utils.AppHaptics

/** Library in selection mode - returns the chosen exercise id via savedStateHandle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    mode: String,
    onSelect: (String) -> Unit,
    viewModel: com.deepkush.reprange.viewmodels.LibraryViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == "template") "Add exercise to routine" else "Add exercise to workout") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(
                    LocalAppWindowInsets.current.only(WindowInsetsSides.Bottom),
                ),
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search exercises…") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                categories.forEach { category ->
                    val selected = filters.category == category
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = {
                            AppHaptics.segmentTick(view)
                            viewModel.setCategory(if (selected) null else category)
                        },
                        label = { Text(category.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            val seedState by viewModel.seedState.collectAsStateWithLifecycle()
            if (seedState == DatasetSeeder.SeedState.Loading) {
                Text("Loading dataset…", Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(results, key = { _, e -> e.id }) { index, exercise ->
                    ExerciseRow(exercise) {
                        AppHaptics.confirm(view)
                        onSelect(exercise.id)
                    }
                }
            }
        }
    }
}
