package com.gymcoach.app.presentation.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.ExerciseItemCard

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
    val filterMovementPattern by viewModel.filterMovementPattern.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()

    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showCameraPicker by rememberSaveable { mutableStateOf(false) }

    val hasActiveFilter = filterDifficulty != "All" || filterEquipment != "All" || filterMovementPattern != "All" || showFavoritesOnly

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.onSearchQueryChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (textFieldValue.isNotBlank()) {
                        IconButton(onClick = {
                            textFieldValue = ""
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            placeholder = { Text("Search exercises, muscles, equipment...") },
            singleLine = true
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

            IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorites only",
                    tint = if (showFavoritesOnly) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No exercises found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Try adjusting your search query or filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasActiveFilter) {
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = {
                            viewModel.onDifficultySelected("All")
                            viewModel.onEquipmentSelected("All")
                            viewModel.onMovementPatternSelected("All")
                            if (showFavoritesOnly) viewModel.toggleFavoritesOnly()
                        }) {
                            Text("Reset Filters")
                        }
                    }
                }
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
                        movementPattern = exercise.movementPattern,
                        isFavorite = exercise.isFavorite,
                        hasAnimation = exercise.animationUrl != null || exercise.id <= 20,
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Filter Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))
                Text("Movement Pattern", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.movementPatterns.forEach { pattern ->
                        FilterChip(
                            selected = pattern == filterMovementPattern,
                            onClick = { viewModel.onMovementPatternSelected(pattern) },
                            label = { Text(pattern) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Difficulty", style = MaterialTheme.typography.titleMedium)
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
                Text("Equipment", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        viewModel.onDifficultySelected("All")
                        viewModel.onEquipmentSelected("All")
                        viewModel.onMovementPatternSelected("All")
                        if (showFavoritesOnly) viewModel.toggleFavoritesOnly()
                    }) {
                        Text("Reset All")
                    }

                    TextButton(onClick = { showFilterSheet = false }) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
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
                    text = "Real-Time Camera Form Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select an exercise for live computer-vision form tracking:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
