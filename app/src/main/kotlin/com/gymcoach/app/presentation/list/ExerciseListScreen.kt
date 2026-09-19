package com.gymcoach.app.presentation.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.ml.ExerciseType
import com.gymcoach.app.presentation.ExerciseViewModel
import com.gymcoach.app.presentation.components.CreateCustomExerciseBottomSheet
import com.gymcoach.app.presentation.components.ExerciseItemCard
import com.gymcoach.app.ui.GymCoachBottomNav
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.TextPrimary
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary

private fun ExerciseType.displayLabel(): String =
    name.lowercase()
        .split('_')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

/**
 * Animated filter chip with color blending, spring press scale,
 * and a sliding/expanding selection indicator dot.
 */
@Composable
fun AnimatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AccentBlue else DarkSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) AccentBlue else GymCoachColors.BorderSubtle,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chipBorder"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.03f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(GymCoachShapes.pill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onClick = onClick
            )
            .semantics {
                this.selected = selected
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = GymCoachShapes.pill,
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected) 14.dp else 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Size / indicator slide when selected
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            if (leadingIcon != null && !selected) {
                leadingIcon()
                Spacer(Modifier.width(4.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onExerciseClick: (Long) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCameraClick: (ExerciseType) -> Unit = {},
    onNavigateBottomBar: (String) -> Unit = {}
) {
    val exercises by viewModel.exercises.collectAsState()
    val animatedNames by viewModel.animatedExerciseNames.collectAsState()
    val filterDifficulty by viewModel.filterDifficulty.collectAsState()
    val filterEquipment by viewModel.filterEquipment.collectAsState()
    val filterMovementPattern by viewModel.filterMovementPattern.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()

    var textFieldValue by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showCameraPicker by rememberSaveable { mutableStateOf(false) }
    var showCreateCustomExerciseSheet by rememberSaveable { mutableStateOf(false) }

    val hasActiveFilter = filterDifficulty != "All" || filterEquipment != "All" ||
            filterMovementPattern != "All" || showFavoritesOnly

    val favTint by animateColorAsState(
        targetValue = if (showFavoritesOnly) Color(0xFFF43F5E) else TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "favTint"
    )
    val favScale by animateFloatAsState(
        targetValue = if (showFavoritesOnly) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "favScale"
    )

    val filterBtnBg by animateColorAsState(
        targetValue = if (hasActiveFilter) AccentBlue.copy(alpha = 0.18f) else DarkSurface,
        animationSpec = tween(200),
        label = "filterBtnBg"
    )
    val filterBtnTint by animateColorAsState(
        targetValue = if (hasActiveFilter) AccentBlue else TextSecondary,
        animationSpec = tween(200),
        label = "filterBtnTint"
    )
    val filterDotScale by animateFloatAsState(
        targetValue = if (hasActiveFilter) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "filterDotScale"
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Exercise Library",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "${exercises.size} movements available",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Favorites Toggle with spring bounce
                    IconButton(onClick = { viewModel.toggleFavoritesOnly() }) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle Favorites",
                            tint = favTint,
                            modifier = Modifier.graphicsLayer {
                                scaleX = favScale
                                scaleY = favScale
                            }
                        )
                    }
                    // Camera / Form Tracking
                    IconButton(onClick = { showCameraPicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Form Tracking",
                            tint = TextSecondary
                        )
                    }
                    // Create Custom Exercise
                    IconButton(onClick = { showCreateCustomExerciseSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Custom Exercise",
                            tint = AccentBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            GymCoachBottomNav(
                currentRoute = "exercise_list",
                onNavigate = onNavigateBottomBar
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Filter Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.onSearchQueryChange(newValue)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search exercise, muscle, equipment...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (textFieldValue.isNotBlank()) {
                            IconButton(onClick = {
                                textFieldValue = ""
                                viewModel.onSearchQueryChange("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = GymCoachShapes.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = GymCoachColors.BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Dedicated Filter Trigger Button with Animated Dot
                Box {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .clip(GymCoachShapes.md)
                            .background(filterBtnBg)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = filterBtnTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (filterDotScale > 0.05f) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer {
                                    scaleX = filterDotScale
                                    scaleY = filterDotScale
                                }
                                .clip(CircleShape)
                                .background(AccentBlue)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }

            // Category Chips Row (Horizontal Scroll with AnimatedFilterChip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.width(8.dp))
                viewModel.categories.forEachIndexed { index, category ->
                    val isSelected = tabIndex == index
                    AnimatedFilterChip(
                        selected = isSelected,
                        onClick = {
                            tabIndex = index
                            viewModel.onCategorySelected(category)
                        },
                        label = category
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            // Main List or Empty State with AnimatedContent
            AnimatedContent(
                targetState = exercises.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "catalogContentAnim"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No exercises found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Try adjusting your search keywords or resetting filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            if (hasActiveFilter) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.onDifficultySelected("All")
                                        viewModel.onEquipmentSelected("All")
                                        viewModel.onMovementPatternSelected("All")
                                        if (showFavoritesOnly) viewModel.toggleFavoritesOnly()
                                        textFieldValue = ""
                                        viewModel.onSearchQueryChange("")
                                    },
                                    shape = GymCoachShapes.pill
                                ) {
                                    Text("Reset All Filters")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(exercises, key = { it.id }) { exercise ->
                            val hasRealAnimation = animatedNames.contains(exercise.name.trim().lowercase()) ||
                                    !exercise.animationUrl.isNullOrBlank()
                            ExerciseItemCard(
                                modifier = Modifier.animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                                name = exercise.name,
                                muscleGroup = exercise.muscleGroup,
                                difficulty = exercise.difficulty,
                                equipment = exercise.equipment,
                                movementPattern = exercise.movementPattern,
                                isFavorite = exercise.isFavorite,
                                hasAnimation = hasRealAnimation,
                                isCustom = exercise.isCustom,
                                onFavoriteToggle = { viewModel.toggleFavorite(exercise) },
                                onClick = { onExerciseClick(exercise.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet with AnimatedFilterChips
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Filter Exercises",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Spacer(Modifier.height(18.dp))
                Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.difficulties.forEach { diff ->
                        AnimatedFilterChip(
                            selected = diff == filterDifficulty,
                            onClick = { viewModel.onDifficultySelected(diff) },
                            label = diff
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Movement Pattern", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.movementPatterns.forEach { pattern ->
                        AnimatedFilterChip(
                            selected = pattern == filterMovementPattern,
                            onClick = { viewModel.onMovementPatternSelected(pattern) },
                            label = pattern
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Equipment", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.equipments.forEach { eq ->
                        AnimatedFilterChip(
                            selected = eq == filterEquipment,
                            onClick = { viewModel.onEquipmentSelected(eq) },
                            label = eq
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
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
                        Text("Reset All", color = TextSecondary)
                    }

                    Button(
                        onClick = { showFilterSheet = false },
                        shape = GymCoachShapes.pill
                    ) {
                        Text("Apply Filters")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Camera Form Tracking Picker
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Live Camera Form Analysis",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Select an exercise for real-time computer-vision rep counting and joint angle verification:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                ExerciseType.entries.forEach { type ->
                    Button(
                        onClick = {
                            showCameraPicker = false
                            onCameraClick(type)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = GymCoachShapes.md
                    ) {
                        Text(type.displayLabel())
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Create Custom Exercise
    if (showCreateCustomExerciseSheet) {
        CreateCustomExerciseBottomSheet(
            onDismiss = { showCreateCustomExerciseSheet = false },
            onSave = { name, muscleGroup, equipment, difficulty, notes ->
                viewModel.createCustomExercise(
                    name = name,
                    muscleGroup = muscleGroup,
                    equipment = equipment,
                    difficulty = difficulty,
                    notes = notes
                )
                showCreateCustomExerciseSheet = false
            }
        )
    }
}
