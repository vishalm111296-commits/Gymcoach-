package com.gymcoach.app.presentation.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.nutrition.ActivityLevel
import com.gymcoach.app.core.nutrition.NutritionGoal
import com.gymcoach.app.core.nutrition.TdeeProfile
import com.gymcoach.app.data.local.entity.NutritionLogEntity
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = GymCoachColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showTdeeSheet() },
                        modifier = Modifier.semantics { contentDescription = "Open TDEE & Macro Calculator" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Calculate,
                            contentDescription = null,
                            tint = GymCoachColors.Primary
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
                shape = CircleShape,
                modifier = Modifier.semantics { contentDescription = "Log new meal" }
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
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
                        CalorieOverviewCard(
                            summary = uiState.dailySummary,
                            onOpenCalculator = { viewModel.showTdeeSheet() }
                        )
                    }

                    if (uiState.dailySummary.tdeeProfile != null) {
                        item {
                            TdeeTargetSummaryCard(
                                profile = uiState.dailySummary.tdeeProfile!!,
                                onOpenCalculator = { viewModel.showTdeeSheet() }
                            )
                        }
                    }

                    item {
                        MacroBreakdownRow(uiState.dailySummary)
                    }

                    item {
                        WaterIntakeCard(
                            totalWaterMl = uiState.dailySummary.totalWaterMl,
                            goalWaterMl = uiState.dailySummary.waterGoalMl,
                            onLogWater = { viewModel.logWater(it) }
                        )
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

        if (uiState.showTdeeCalculatorSheet && uiState.dailySummary.tdeeProfile != null) {
            TdeeCalculatorBottomSheet(
                currentProfile = uiState.dailySummary.tdeeProfile!!,
                selectedGoal = uiState.selectedGoalOverride ?: uiState.dailySummary.tdeeProfile!!.goal,
                selectedActivity = uiState.selectedActivityOverride ?: uiState.dailySummary.tdeeProfile!!.activityLevel,
                onSelectGoal = { viewModel.selectGoalOverride(it) },
                onSelectActivity = { viewModel.selectActivityOverride(it) },
                onDismiss = { viewModel.hideTdeeSheet() }
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
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.semantics { contentDescription = "Previous Day" }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = GymCoachColors.TextSecondary)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(onClick = onToday)
                .semantics { contentDescription = if (isToday) "Today selected" else "Selected date ${selectedDate.format(formatter)}, tap to go to today" }
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

        IconButton(
            onClick = onNextDay,
            modifier = Modifier.semantics { contentDescription = "Next Day" }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GymCoachColors.TextSecondary)
        }
    }
}

@Composable
private fun CalorieOverviewCard(
    summary: DailyNutritionSummary,
    onOpenCalculator: () -> Unit
) {
    val rawRatio = if (summary.calorieGoal > 0) summary.totalCalories.toFloat() / summary.calorieGoal else 0f
    val ratio = rawRatio.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = ratio, animationSpec = tween(600), label = "calorieProgress")
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
                    color = if (remaining > 0) GymCoachColors.Success.copy(alpha = 0.15f) else GymCoachColors.Warning.copy(alpha = 0.15f),
                    modifier = Modifier.clickable(onClick = onOpenCalculator)
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
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(GymCoachShapes.pill),
                color = if (rawRatio > 1.0f) GymCoachColors.Warning else GymCoachColors.Primary,
                trackColor = GymCoachColors.SurfaceDeep
            )
        }
    }
}

@Composable
private fun TdeeTargetSummaryCard(
    profile: TdeeProfile,
    onOpenCalculator: () -> Unit
) {
    Card(
        shape = GymCoachShapes.Card,
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCardElevated),
        border = GymCoachBorders.subtleBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCalculator)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GymCoachColors.GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "METABOLIC TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.GoldAccent
                    )
                }

                Surface(
                    shape = GymCoachShapes.pill,
                    color = GymCoachColors.Primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = profile.goal.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("BMR (Base)", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
                    Text("${profile.bmr} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GymCoachColors.TextPrimary)
                }
                Column {
                    Text("TDEE (Burned)", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
                    Text("${profile.tdee} kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GymCoachColors.TextPrimary)
                }
                Column {
                    Text("Daily Target", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
                    Text(
                        "${profile.targetCalories} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = if (profile.calorieAdjustment >= 0) GymCoachColors.Success else GymCoachColors.CyanAccent
                    )
                }
                Column {
                    Text("Adjustment", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
                    Text(
                        if (profile.calorieAdjustment > 0) "+${profile.calorieAdjustment} kcal" else "${profile.calorieAdjustment} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (profile.calorieAdjustment > 0) GymCoachColors.GoldAccent else if (profile.calorieAdjustment < 0) GymCoachColors.CyanAccent else GymCoachColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Macro Split Proportions Bar
            MacroDistributionBar(profile.macroSplit)
        }
    }
}

@Composable
private fun MacroDistributionBar(split: com.gymcoach.app.core.nutrition.MacroSplit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .weight((split.proteinPercent / 100f).coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(GymCoachColors.Primary)
            )
            Box(
                modifier = Modifier
                    .weight((split.carbsPercent / 100f).coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(GymCoachColors.GoldAccent)
            )
            Box(
                modifier = Modifier
                    .weight((split.fatPercent / 100f).coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(GymCoachColors.CyanAccent)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Protein ${split.proteinPercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.Primary
            )
            Text(
                "Carbs ${split.carbsPercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.GoldAccent
            )
            Text(
                "Fat ${split.fatPercent.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = GymCoachColors.CyanAccent
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
    val animatedProgress by animateFloatAsState(targetValue = ratio.coerceIn(0f, 1f), animationSpec = tween(500), label = "macroProgress")

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
                progress = { animatedProgress },
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
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Edit meal ${log.mealName}" }
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = GymCoachColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Delete meal ${log.mealName}" }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = GymCoachColors.Danger, modifier = Modifier.size(20.dp))
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
private fun WaterIntakeCard(
    totalWaterMl: Int,
    goalWaterMl: Int,
    onLogWater: (Int) -> Unit
) {
    val progress = (totalWaterMl.toFloat() / goalWaterMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(500), label = "waterProgress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GymCoachShapes.md)
            .border(GymCoachBorders.subtle, GymCoachShapes.md),
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = GymCoachShapes.md
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = GymCoachColors.CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "DAILY HYDRATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GymCoachColors.CyanAccent
                )
            }

            Text(
                text = "$totalWaterMl / $goalWaterMl ml",
                style = MaterialTheme.typography.bodyLarge,
                color = GymCoachColors.TextPrimary
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = GymCoachColors.CyanAccent,
                trackColor = GymCoachColors.CyanAccent.copy(alpha = 0.2f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLogWater(250) }
                        .semantics { contentDescription = "Add 250 milliliters water glass" },
                    shape = RoundedCornerShape(16.dp),
                    color = GymCoachColors.CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+250 ml (Glass)",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GymCoachColors.CyanAccent
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLogWater(500) }
                        .semantics { contentDescription = "Add 500 milliliters water bottle" },
                    shape = RoundedCornerShape(16.dp),
                    color = GymCoachColors.CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+500 ml (Bottle)",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = GymCoachColors.CyanAccent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TdeeCalculatorBottomSheet(
    currentProfile: TdeeProfile,
    selectedGoal: NutritionGoal,
    selectedActivity: ActivityLevel,
    onSelectGoal: (NutritionGoal) -> Unit,
    onSelectActivity: (ActivityLevel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GymCoachColors.SurfaceCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GymCoachColors.TextMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = GymCoachColors.Primary
                    )
                    Text(
                        "TDEE & Macro Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = GymCoachColors.TextMuted)
                }
            }

            // Scientific formula explanation
            Surface(
                shape = GymCoachShapes.Card,
                color = GymCoachColors.SurfaceCardElevated,
                border = GymCoachBorders.subtleBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Formula: ${currentProfile.formulaUsed.name.replace("_", " ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GymCoachColors.GoldAccent
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Calculated from your biological parameters, training volume, and selected dietary focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GymCoachColors.TextSecondary
                    )
                }
            }

            // Goal Selection
            Text(
                "Nutrition & Body Goal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextMuted
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NutritionGoal.values().forEach { goal ->
                    val isSelected = goal == selectedGoal
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGoal(goal) },
                        shape = GymCoachShapes.Card,
                        color = if (isSelected) GymCoachColors.Primary.copy(alpha = 0.15f) else GymCoachColors.SurfaceCardElevated,
                        border = if (isSelected) BorderStroke(1.dp, GymCoachColors.Primary) else GymCoachBorders.subtleBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                goal.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GymCoachColors.Primary else GymCoachColors.TextPrimary
                            )
                            Text(
                                "${goal.proteinPerKg}g protein/kg",
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Activity Level Selection
            Text(
                "Activity Level",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GymCoachColors.TextMuted
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityLevel.values().forEach { activity ->
                    val isSelected = activity == selectedActivity
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectActivity(activity) },
                        shape = GymCoachShapes.Card,
                        color = if (isSelected) GymCoachColors.CyanAccent.copy(alpha = 0.15f) else GymCoachColors.SurfaceCardElevated,
                        border = if (isSelected) BorderStroke(1.dp, GymCoachColors.CyanAccent) else GymCoachBorders.subtleBorder()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${activity.displayName} (x${activity.multiplier})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GymCoachColors.CyanAccent else GymCoachColors.TextPrimary
                            )
                            Text(
                                activity.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = GymCoachColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
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
                MealTypeSelector(selectedMeal = mealName, onSelectMeal = { mealName = it })

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                MacroInputFields(
                    protein = protein,
                    onProteinChange = { protein = it },
                    carbs = carbs,
                    onCarbsChange = { carbs = it },
                    fat = fat,
                    onFatChange = { fat = it }
                )

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

@Composable
private fun MealTypeSelector(
    selectedMeal: String,
    onSelectMeal: (String) -> Unit
) {
    val presetMeals = listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout")
    Text("Meal Type", style = MaterialTheme.typography.labelSmall, color = GymCoachColors.TextMuted)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        presetMeals.take(3).forEach { meal ->
            FilterChip(
                selected = selectedMeal == meal,
                onClick = { onSelectMeal(meal) },
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
                selected = selectedMeal == meal,
                onClick = { onSelectMeal(meal) },
                label = { Text(meal, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GymCoachColors.Primary,
                    selectedLabelColor = GymCoachColors.TextPrimary
                )
            )
        }
    }
}

@Composable
private fun MacroInputFields(
    protein: String,
    onProteinChange: (String) -> Unit,
    carbs: String,
    onCarbsChange: (String) -> Unit,
    fat: String,
    onFatChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = protein,
            onValueChange = onProteinChange,
            label = { Text("Protein (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = carbs,
            onValueChange = onCarbsChange,
            label = { Text("Carbs (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = fat,
            onValueChange = onFatChange,
            label = { Text("Fat (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}
