package com.gymcoach.app.presentation.list

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.ExerciseItemCard
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.WarmWhite

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
    val filterFavorites by viewModel.filterFavorites.collectAsState()
    val filterEquipment by viewModel.filterEquipment.collectAsState()

    // We can infer search is active if the query is not blank.
    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showCameraPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Spacer(Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.onSearchQueryChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary
                )
            },
            placeholder = {
                Text(
                    "Search exercises...",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = WarmWhite,
                unfocusedTextColor = WarmWhite,
                cursorColor = AccentBlue
            ),
            trailingIcon = {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = if (filterDifficulty != "All" || filterEquipment != "All" || filterFavorites) AccentBlue else TextSecondary
                    )
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // Tabs and Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                modifier = Modifier.weight(1f),
                edgePadding = 0.dp,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = AccentBlue
                    )
                },
                divider = {}
            ) {
                viewModel.categories.forEachIndexed { index, category ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            viewModel.onCategorySelected(category)
                        },
                        text = {
                            Text(
                                text = category,
                                color = if (tabIndex == index) WarmWhite else TextSecondary,
                                fontWeight = if (tabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showCameraPicker = true }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Form Analysis", tint = TextSecondary)
                }
                IconButton(onClick = onHistoryClick) {
                    Icon(Icons.Filled.History, contentDescription = "Workout History", tint = TextSecondary)
                }
                IconButton(onClick = onProgressClick) {
                    Icon(Icons.Filled.Insights, contentDescription = "Progress", tint = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // List or Empty State
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (textFieldValue.isNotBlank()) "NO RESULTS" else "NO EXERCISES",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentBlue,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (textFieldValue.isNotBlank()) "No exercises match your search or filters." else "No exercises found in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseItemCard(
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        difficulty = exercise.difficulty,
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
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = WarmWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "DIFFICULTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.difficulties.forEach { diff ->
                        FilterChip(
                            selected = diff == filterDifficulty,
                            onClick = { viewModel.onDifficultySelected(diff) },
                            label = { Text(diff) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkBackground,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = WarmWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = diff == filterDifficulty,
                                borderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "EQUIPMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.equipments.forEach { eq ->
                        FilterChip(
                            selected = eq == filterEquipment,
                            onClick = { viewModel.onEquipmentSelected(eq) },
                            label = { Text(eq) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkBackground,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = WarmWhite
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = eq == filterEquipment,
                                borderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        viewModel.onDifficultySelected("All")
                        viewModel.onEquipmentSelected("All")
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onFavoritesToggled(false)
                        viewModel.onCategorySelected("All")
                        tabIndex = 0
                    }) {
                        Text("CLEAR FILTERS", color = AccentBlue, fontWeight = FontWeight.Bold)
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
            sheetState = pickerSheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "SELECT EXERCISE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            showCameraPicker = false
                            onCameraClick(type)
                        },
                        label = { Text(type.displayLabel(), color = WarmWhite) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkBackground
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
