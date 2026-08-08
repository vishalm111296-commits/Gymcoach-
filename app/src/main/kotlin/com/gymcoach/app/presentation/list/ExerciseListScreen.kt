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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.ExerciseItemCard
import com.gymcoach.app.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    navController: androidx.navigation.NavHostController,
    viewModel: ExerciseViewModel = hiltViewModel(),
    onExerciseClick: (Long) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    val exercises by viewModel.exercises.collectAsState()
    val filterDifficulty by viewModel.filterDifficulty.collectAsState()
    val filterEquipment by viewModel.filterEquipment.collectAsState()

    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.onSearchQueryChange(newValue)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onFilterClick = { showFilterSheet = true }
        )

        if (showFilterSheet) {
            FilterBottomSheet(
                filterDifficulty = filterDifficulty,
                filterEquipment = filterEquipment,
                difficulties = viewModel.difficulties,
                equipments = viewModel.equipments,
                categories = viewModel.categories,
                selectedCategory = tabIndex,
                onDifficultySelected = { viewModel.onDifficultySelected(it) },
                onEquipmentSelected = { viewModel.onEquipmentSelected(it) },
                onCategorySelected = {
                    tabIndex = it
                    viewModel.onCategorySelected(viewModel.categories[it])
                },
                onClearFilters = {
                    viewModel.onDifficultySelected("All")
                    viewModel.onEquipmentSelected("All")
                    viewModel.onCategorySelected("All")
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            )
        }
    }

    mainContent(
        exercises = exercises,
        categories = viewModel.categories,
        selectedCategory = tabIndex,
        onCategorySelected = {
            tabIndex = it
            viewModel.onCategorySelected(viewModel.categories[it])
        },
        onExerciseClick = onExerciseClick,
        topBarActions = {
            NavigationActions(
                onHistoryClick = onHistoryClick,
                onProgressClick = onProgressClick,
                onCameraClick = onCameraClick,
                navigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
    )
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search exercises",
                modifier = Modifier.size(24.dp)
            )
        },
        placeholder = { Text("Search exercises...") },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = onFilterClick, modifier = Modifier.size(48.dp).height(48.dp)) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Filter exercises",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filterDifficulty: String,
    filterEquipment: String,
    difficulties: List<String>,
    equipments: List<String>,
    categories: List<String>,
    selectedCategory: Int,
    onDifficultySelected: (String) -> Unit,
    onEquipmentSelected: (String) -> Unit,
    onCategorySelected: (Int) -> Unit,
    onClearFilters: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { /* automatically dismissed by sheetState */ },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Difficulty", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FilterRow(
                options = difficulties,
                selected = filterDifficulty,
                onOptionSelected = onDifficultySelected
            )
            Spacer(Modifier.height(16.dp))
            Text("Equipment", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            EquipmentRow(
                options = equipments,
                selected = filterEquipment,
                onOptionSelected = onEquipmentSelected
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClearFilters,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Filters")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilterRow(
    options: List<String>,
    selected: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { diff ->
            FilterChip(
                selected = diff == selected,
                onClick = { onOptionSelected(diff) },
                label = { Text(diff) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun EquipmentRow(
    options: List<String>,
    selected: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { eq ->
            FilterChip(
                selected = eq == selected,
                onClick = { onOptionSelected(eq) },
                label = { Text(eq) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationActions(
    onHistoryClick: () -> Unit,
    onProgressClick: () -> Unit,
    onCameraClick: () -> Unit,
    navigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScrollableTabRow(
            selectedTabIndex = 0,
            modifier = Modifier.weight(1f),
            edgePadding = 0.dp
        ) {
            listOf("All").forEachIndexed { index, category ->
                Tab(
                    selected = index == 0,
                    onClick = {},
                    text = { Text(category) }
                )
            }
        }

        IconButton(
            onClick = onCameraClick,
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = "Form analysis",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = "Workout history",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = onProgressClick,
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(
                Icons.Filled.Insights,
                contentDescription = "Progress",
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = navigateToSettings,
            modifier = Modifier.height(48.dp).width(48.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainContent(
    exercises: List<com.gymcoach.app.domain.model.Exercise>,
    categories: List<String>,
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
    onExerciseClick: (Long) -> Unit,
    topBarActions: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                topBarActions()
            }
            items(exercises, key = { it.id }) { exercise ->
                ExerciseItemCard(
                    name = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    difficulty = exercise.difficulty,
                    equipment = exercise.equipment,
                    onClick = { onExerciseClick(exercise.id) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}