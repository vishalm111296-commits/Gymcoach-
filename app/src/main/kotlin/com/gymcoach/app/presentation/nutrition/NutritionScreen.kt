package com.gymcoach.app.presentation.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    onBackClick: () -> Unit,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition & Fuel", color = GymCoachColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GymCoachColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GymCoachColors.PureDark,
                    titleContentColor = GymCoachColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = GymCoachColors.Primary,
                contentColor = GymCoachColors.TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Log Meal")
            }
        },
        containerColor = GymCoachColors.PureDark
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GymCoachColors.Primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Date Bar
                DateNavigator(
                    selectedDate = uiState.selectedDate,
                    onPreviousDay = { viewModel.loadDay(uiState.selectedDate.minusDays(1)) },
                    onNextDay = { viewModel.loadDay(uiState.selectedDate.plusDays(1)) },
                    onToday = { viewModel.loadDay(LocalDate.now()) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        CalorieOverviewCard(uiState.dailySummary)
                    }

                    item {
                        MacroBreakdownRow(uiState.dailySummary)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Logged Meals (${uiState.todayLogs.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GymCoachColors.TextPrimary
                            )
                        }
                    }

                    if (uiState.todayLogs.isEmpty()) {
                        item {
                            EmptyMealCard(onLogClick = { viewModel.showAddDialog() })
                        }
                    } else {
                        items(uiState.todayLogs, key = { it.id }) { log ->
                            MealLogCard(
                                log = log,
                                onEdit = { viewModel.editLog(log) },
                                onDelete = { viewModel.deleteLog(log) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            AddEditMealDialog(
                initialLog = uiState.editingLog,
                onDismiss = { viewModel.hideDialog() },
                onSave = { mealName, cal, p, c, f, fiber, water, notes ->
                    viewModel.saveLog(mealName, cal, p, c, f, fiber, water, notes)
                }
            )
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.Card)
            .background(GymCoachColors.SurfaceCard)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day", tint = GymCoachColors.TextSecondary)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onToday)
        ) {
            Text(
                text = if (isToday) "Today" else selectedDate.format(formatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary
            )
            if (!isToday) {
                Text(
                    text = "Tap for Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.Primary
                )
            }
        }

        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = GymCoachColors.TextSecondary)
        }
    }
}

@Composable
private fun CalorieOverviewCard(summary: DailyNutritionSummary) {
    val ratio = if (summary.calorieGoal > 0) (summary.totalCalories.toFloat() / summary.calorieGoal).coerceIn(0f, 1f) else 0f
    val remaining = (summary.calorieGoal - summary.totalCalories).coerceAtLeast(0)

    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CALORIES CONSUMED",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${summary.totalCalories} / ${summary.calorieGoal} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GymCoachColors.TextPrimary
                    )
                }

                Surface(
                    shape = GymCoachShapes.pill,
                    color = if (remaining > 0) GymCoachColors.Success.copy(alpha = 0.15f) else GymCoachColors.Warning.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (remaining > 0) "$remaining kcal left" else "Goal Met!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0) GymCoachColors.Success else GymCoachColors.Warning,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(GymCoachShapes.pill),
                color = if (ratio > 1.0f) GymCoachColors.Warning else GymCoachColors.Primary,
                trackColor = GymCoachColors.SurfaceDeep
            )
        }
    }
}

@Composable
private fun MacroBreakdownRow(summary: DailyNutritionSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroCard(
            title = "Protein",
            current = "${summary.totalProtein.toInt()}g",
            goal = "${summary.proteinGoalGrams.toInt()}g",
            ratio = summary.totalProtein / summary.proteinGoalGrams.coerceAtLeast(1f),
            color = GymCoachColors.Primary,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            title = "Carbs",
            current = "${summary.totalCarbs.toInt()}g",
            goal = "${summary.carbsGoalGrams.toInt()}g",
            ratio = summary.totalCarbs / summary.carbsGoalGrams.coerceAtLeast(1f),
            color = GymCoachColors.GoldAccent,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            title = "Fat",
            current = "${summary.totalFat.toInt()}g",
            goal = "${summary.fatGoalGrams.toInt()}g",
            ratio = summary.totalFat / summary.fatGoalGrams.coerceAtLeast(1f),
            color = GymCoachColors.CyanAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: String,
    goal: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(current, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text("of $goal", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(GymCoachShapes.pill),
                color = color,
                trackColor = GymCoachColors.SurfaceDeep
            )
        }
    }
}

@Composable
private fun MealLogCard(
    log: NutritionLogEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = GymCoachColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        log.mealName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${log.calories} kcal • P: ${log.proteinGrams.toInt()}g | C: ${log.carbsGrams.toInt()}g | F: ${log.fatGrams.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
                if (log.notes.isNotBlank()) {
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = GymCoachColors.TextMuted,
                        maxLines = 1
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = GymCoachColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = GymCoachColors.Danger, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyMealCard(onLogClick: () -> Unit) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        border = GymCoachBorders.subtleBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLogClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.LocalDining,
                contentDescription = null,
                tint = GymCoachColors.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "No meals logged for this day",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap to log your breakfast, lunch, dinner, or snacks",
                style = MaterialTheme.typography.bodySmall,
                color = GymCoachColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AddEditMealDialog(
    initialLog: NutritionLogEntity?,
    onDismiss: () -> Unit,
    onSave: (mealName: String, cal: Int, p: Float, c: Float, f: Float, fiber: Float, water: Int, notes: String) -> Unit
) {
    var mealName by remember { mutableStateOf(initialLog?.mealName ?: "Breakfast") }
    var calories by remember { mutableStateOf(if ((initialLog?.calories ?: 0) > 0) initialLog?.calories.toString() else "") }
    var protein by remember { mutableStateOf(if ((initialLog?.proteinGrams ?: 0f) > 0f) initialLog?.proteinGrams.toString() else "") }
    var carbs by remember { mutableStateOf(if ((initialLog?.carbsGrams ?: 0f) > 0f) initialLog?.carbsGrams.toString() else "") }
    var fat by remember { mutableStateOf(if ((initialLog?.fatGrams ?: 0f) > 0f) initialLog?.fatGrams.toString() else "") }
    var notes by remember { mutableStateOf(initialLog?.notes ?: "") }

    val presetMeals = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceCard,
        titleContentColor = GymCoachColors.TextPrimary,
        title = {
            Text(if (initialLog == null) "Log Meal" else "Edit Meal")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Meal Type", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetMeals.take(3).forEach { meal ->
                        FilterChip(
                            selected = mealName == meal,
                            onClick = { mealName = meal },
                            label = { Text(meal, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GymCoachColors.Primary,
                                selectedLabelColor = GymCoachColors.TextPrimary
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetMeals.drop(3).forEach { meal ->
                        FilterChip(
                            selected = mealName == meal,
                            onClick = { mealName = meal },
                            label = { Text(meal, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GymCoachColors.Primary,
                                selectedLabelColor = GymCoachColors.TextPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calories.toIntOrNull() ?: 0
                    val p = protein.toFloatOrNull() ?: 0f
                    val c = carbs.toFloatOrNull() ?: 0f
                    val f = fat.toFloatOrNull() ?: 0f
                    onSave(mealName, cal, p, c, f, 0f, 0, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymCoachColors.Primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GymCoachColors.TextMuted)
            }
        }
    )
}
