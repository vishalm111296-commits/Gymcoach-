package com.gymcoach.app.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.PremiumExerciseCard
import com.gymcoach.app.presentation.components.PremiumEmptyState
import com.gymcoach.app.ui.theme.*

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

    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "EXERCISE LIBRARY",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.onSearchQueryChange(newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.Tune, "Filter", tint = if (filterDifficulty != "All" || filterEquipment != "All") AccentBlue else TextSecondary)
                        }
                    },
                    placeholder = { Text("Search exercises...", color = TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                ScrollableTabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = AccentBlue,
                    edgePadding = 16.dp,
                    divider = {},
                    indicator = {}
                ) {
                    viewModel.categories.forEachIndexed { index, category ->
                        val selected = tabIndex == index
                        Tab(
                            selected = selected,
                            onClick = {
                                tabIndex = index
                                viewModel.onCategorySelected(category)
                            },
                            text = {
                                Text(
                                    text = category.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) AccentBlue else TextTertiary
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (textFieldValue.isNotEmpty()) {
                    PremiumEmptyState("NO RESULTS", "No exercises match your search criteria.")
                } else {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    PremiumExerciseCard(
                        name = exercise.name,
                        primaryMuscle = exercise.muscleGroup,
                        equipment = exercise.equipment,
                        difficulty = exercise.difficulty,
                        onClick = { onExerciseClick(exercise.id) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = DarkCard
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("FILTERS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = TextPrimary)
                Spacer(Modifier.height(16.dp))

                Text("Difficulty", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { diff ->
                        FilterChip(
                            selected = filterDifficulty == diff,
                            onClick = { viewModel.onDifficultySelected(if (filterDifficulty == diff) "All" else diff) },
                            label = { Text(diff) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Text("Equipment", style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                    listOf("barbell", "dumbbell", "machine", "bodyweight").forEach { eq ->
                        FilterChip(
                            selected = filterEquipment == eq,
                            onClick = { viewModel.onEquipmentSelected(if (filterEquipment == eq) "All" else eq) },
                            label = { Text(eq.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.onDifficultySelected("All")
                        viewModel.onEquipmentSelected("All")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary)
                ) {
                    Text("Clear Filters")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
