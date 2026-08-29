package com.gymcoach.app.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.ExerciseItemCard

/** Human-readable label, e.g. BENT_OVER_ROW -> "Bent Over Row". */
private fun ExerciseType.displayLabel(): String =
    name.lowercase()
        .split('_')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseViewModel = hiltViewModel(),
    onExerciseClick: (Long) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCameraClick: (ExerciseType) -> Unit = {}
) {
    val exercises by viewModel.exercises.collectAsState()
    val filterDifficulty by viewModel.filterDifficulty.collectAsState()
    val filterEquipment by viewModel.filterEquipment.collectAsState()
    val filterFavorites by viewModel.filterFavorites.collectAsState()

    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showCameraPicker by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.onSearchQueryChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            placeholder = { Text("Search exercises...") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter")
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                modifier = Modifier.weight(1f),
                edgePadding = 0.dp
            ) {
                viewModel.categories.forEachIndexed { index, category ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            viewModel.onCategorySelected(category)
                        },
                        text = { Text(category) }
                    )
                }
            }

            IconButton(onClick = { showCameraPicker = true }) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Form Analysis")
            }

            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Filled.History, contentDescription = "Workout History")
            }

            IconButton(onClick = onProgressClick) {
                Icon(Icons.Filled.Insights, contentDescription = "Progress")
            }
        }

        if (exercises.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No exercises found.",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Try adjusting your filters or search query.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseItemCard(
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        difficulty = exercise.difficulty,
                        equipment = exercise.equipment,
                        onClick = { onExerciseClick(exercise.id) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Difficulty", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.difficulties.forEach { diff ->
                        FilterChip(
                            selected = diff == filterDifficulty,
                            onClick = { viewModel.onDifficultySelected(diff) },
                            label = { Text(diff) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Equipment", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // Equipment options can be long, so use a horizontal scroll or wrap. 
                // Since this is just a sheet, a ScrollableTabRow style or wrapping layout is best.
                // We'll use a simple horizontal scroll for equipment.
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.equipments.forEach { eq ->
                        FilterChip(
                            selected = eq == filterEquipment,
                            onClick = { viewModel.onEquipmentSelected(eq) },
                            label = { Text(eq) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Favorites", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FilterChip(
                    selected = filterFavorites,
                    onClick = { viewModel.onFavoritesToggled(!filterFavorites) },
                    label = { Text("Show only Favorites") }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        viewModel.onDifficultySelected("All")
                        viewModel.onEquipmentSelected("All")
                        viewModel.onCategorySelected("All")
                        viewModel.onFavoritesToggled(false)
                    }) {
                        Text("Clear Filters")
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showCameraPicker) {
        val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCameraPicker = false },
            sheetState = pickerSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = "Which exercise are you doing?",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            showCameraPicker = false
                            onCameraClick(type)
                        },
                        label = { Text(type.displayLabel()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
