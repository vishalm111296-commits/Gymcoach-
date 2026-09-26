package com.gymcoach.app.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.presentation.onboarding.components.EquipmentChecklist
import com.gymcoach.app.presentation.onboarding.components.GoalSelectionCard
import com.gymcoach.app.ui.theme.GymCoachBorders
import com.gymcoach.app.ui.theme.GymCoachColors
import com.gymcoach.app.ui.theme.GymCoachShapes
import com.gymcoach.app.ui.theme.GymCoachSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val GOALS = listOf(
    "V-Taper Hypertrophy" to "Wide shoulders, tight waist. Lats and side delts lead.",
    "Muscle Gain" to "Maximize overall size with balanced volume.",
    "Strength" to "Heavy compounds, low reps, long rest.",
    "Fat Loss" to "Dense circuits that keep muscle while leaning out.",
    "General Fitness" to "Feel great, move well, train consistently."
)

private val EXPERIENCES = listOf(
    "Beginner" to "Under a year of training, or starting fresh.",
    "Intermediate" to "One to three years of consistent training.",
    "Advanced" to "Three-plus years. You know your numbers."
)

private val SEXES = listOf(
    "Male" to "Male",
    "Female" to "Female",
    "Other" to "Other"
)

private val PREFERRED_SCHEDULES = listOf(
    "Morning" to "Morning (6am-12pm)",
    "Afternoon" to "Afternoon (12pm-6pm)",
    "Evening" to "Evening (6pm-10pm)"
)

private val LIMITATIONS = listOf(
    "None" to "None",
    "Injury" to "Injury/recurring niggles",
    "Mobility" to "Mobility limitations",
    "Time" to "Time constraints",
    "Other" to "Other"
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val order = OnboardingStep.entries
    val stepIndex = order.indexOf(state.step)
    val contentSteps = remember {
        listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.GOAL,
            OnboardingStep.EXPERIENCE,
            OnboardingStep.PERSONAL_INFO,
            OnboardingStep.SCHEDULE,
            OnboardingStep.EQUIPMENT,
            OnboardingStep.REVIEW
        )
    }

    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    Scaffold(containerColor = GymCoachColors.PureDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GymCoachColors.PureDark)
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = {
                    (stepIndex.coerceAtMost(order.indexOf(OnboardingStep.REVIEW))) /
                        order.indexOf(OnboardingStep.REVIEW).toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = GymCoachColors.Primary,
                trackColor = GymCoachColors.SurfaceCardElevated
            )

            // Step Indicator with animated dot width morphing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STEP ${stepIndex + 1} OF ${contentSteps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymCoachColors.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                OnboardingPageIndicator(
                    steps = contentSteps,
                    currentStep = state.step
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val targetIndex = order.indexOf(targetState)
                        val initialIndex = order.indexOf(initialState)
                        if (targetIndex >= initialIndex) {
                            (slideInHorizontally { width -> (width * 0.35f).toInt() } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally { width -> (-width * 0.35f).toInt() } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally { width -> (-width * 0.35f).toInt() } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally { width -> (width * 0.35f).toInt() } + fadeOut(tween(180)))
                        }
                    },
                    label = "onboarding_step_pager"
                ) { currentStep ->
                    Column {
                        when (currentStep) {
                            OnboardingStep.WELCOME -> WelcomeStep()
                            OnboardingStep.GOAL -> GoalStep(state, viewModel::selectGoal)
                            OnboardingStep.EXPERIENCE -> ExperienceStep(state, viewModel::selectExperience)
                            OnboardingStep.PERSONAL_INFO -> PersonalInfoStep(
                                state,
                                viewModel::setAge,
                                viewModel::setHeight,
                                viewModel::setWeight,
                                viewModel::setSex,
                                viewModel::setPreferredSchedule,
                                viewModel::setLimitationsPreferences
                            )
                            OnboardingStep.SCHEDULE -> ScheduleStep(
                                state,
                                viewModel::setDaysPerWeek,
                                viewModel::setSessionMinutes
                            )
                            OnboardingStep.EQUIPMENT -> EquipmentStep(state, viewModel::toggleEquipment)
                            OnboardingStep.REVIEW -> ReviewStep(state)
                            OnboardingStep.COMPLETE -> Unit
                        }
                    }
                }
                state.error?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = GymCoachColors.Danger)
                }
            }

            BottomBar(
                state = state,
                onBack = viewModel::back,
                onNext = {
                    if (state.isLastContentStep) viewModel.completeOnboarding(onComplete)
                    else viewModel.next()
                }
            )
        }
    }
}

@Composable
private fun StepHeader(title: String, description: String) {
    Column {
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = GymCoachColors.TextSecondary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageIndicator(
    steps: List<OnboardingStep>,
    currentStep: OnboardingStep,
    modifier: Modifier = Modifier
) {
    val currentIndex = steps.indexOf(currentStep).coerceAtLeast(0)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, _ ->
            val isSelected = index == currentIndex
            val isPassed = index < currentIndex

            val targetWidth = if (isSelected) 26.dp else 7.dp
            val animatedWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "page_indicator_width_$index"
            )

            val targetColor = when {
                isSelected -> GymCoachColors.Primary
                isPassed -> GymCoachColors.PrimaryLight.copy(alpha = 0.5f)
                else -> GymCoachColors.SurfaceCardElevated
            }
            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(durationMillis = 300),
                label = "page_indicator_color_$index"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(animatedWidth)
                    .clip(RoundedCornerShape(3.dp))
                    .background(animatedColor)
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "BUILD YOUR\nV-TAPER",
            style = MaterialTheme.typography.displaySmall,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Answer six quick questions and your first program is built around your goal, schedule, and equipment.",
            style = MaterialTheme.typography.bodyLarge,
            color = GymCoachColors.TextSecondary
        )
        Spacer(Modifier.height(24.dp))

        val features = remember {
            listOf(
                Triple(
                    Icons.Default.FitnessCenter,
                    "Equipment-Adaptive Programs",
                    "Programs matched precisely to the equipment you have available."
                ),
                Triple(
                    Icons.Default.Bolt,
                    "Evidence-Based Volume",
                    "Weekly sets tuned to scientific hypertrophy and strength ranges."
                ),
                Triple(
                    Icons.Default.Bedtime,
                    "Biometric Recovery Pacing",
                    "Readiness & fatigue tracking keeps workouts productive without burnout."
                ),
                Triple(
                    Icons.Default.LocalFireDepartment,
                    "Instant First Session",
                    "Your customized training routine is generated and ready in minutes."
                )
            )
        }

        features.forEachIndexed { index, (icon, title, desc) ->
            AnimatedFeatureItem(
                icon = icon,
                title = title,
                description = desc,
                delayIndex = index
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AnimatedFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    delayIndex: Int
) {
    val animOffset = remember { Animatable(80f) }
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(delayIndex * 90L + 80L)
        launch {
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = animOffset.value
                alpha = animAlpha.value
            },
        colors = CardDefaults.cardColors(containerColor = GymCoachColors.SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GymCoachColors.SurfaceCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Feature icon",
                    tint = GymCoachColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GymCoachColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GymCoachColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GoalStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    StepHeader("WHAT IS THE GOAL?", "Pick your primary target. Everything else tunes around it.")
    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)) {
        GOALS.forEach { (title, description) ->
            GoalSelectionCard(
                goal = title,
                description = description,
                isSelected = state.goal == title,
                onClick = { onSelect(title) }
            )
        }
    }
}

@Composable
private fun ExperienceStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    StepHeader("TRAINING EXPERIENCE", "Sets, reps, and progression adapt to where you are.")
    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.md)) {
        EXPERIENCES.forEach { (title, description) ->
            GoalSelectionCard(
                goal = title,
                description = description,
                isSelected = state.experience == title,
                onClick = { onSelect(title) }
            )
        }
    }
}

@Composable
private fun PersonalInfoStep(
    state: OnboardingUiState,
    onAge: (Float) -> Unit,
    onHeight: (Float) -> Unit,
    onWeight: (Float) -> Unit,
    onSex: (String) -> Unit,
    onPreferredSchedule: (String) -> Unit,
    onLimitationsPreferences: (String) -> Unit
) {
    StepHeader("THE BASICS", "Used to calibrate pacing and starting loads.")

    Text(
        text = "AGE - ${state.age.toInt()} YEARS",
        style = MaterialTheme.typography.labelMedium,
        color = GymCoachColors.TextSecondary,
        letterSpacing = 1.sp
    )
    Slider(
        value = state.age,
        onValueChange = onAge,
        valueRange = 14f..80f,
        steps = 65,
        colors = SliderDefaults.colors(
            thumbColor = GymCoachColors.Primary,
            activeTrackColor = GymCoachColors.Primary,
            inactiveTrackColor = GymCoachColors.SurfaceCardElevated
        )
    )

    Spacer(Modifier.height(16.dp))
    NumberField(label = "HEIGHT (CM)", initialValue = state.heightCm.toInt().toString()) { text ->
        text.toFloatOrNull()?.let(onHeight)
    }
    Spacer(Modifier.height(16.dp))
    NumberField(label = "WEIGHT (KG)", initialValue = state.weightKg.toInt().toString()) { text ->
        text.toFloatOrNull()?.let(onWeight)
    }

    Spacer(Modifier.height(16.dp))
    SexSelection(
        selectedSex = state.sex ?: "Male",
        onSelect = onSex
    )

    Spacer(Modifier.height(16.dp))
    PreferredScheduleChipGroup(
        selectedSchedule = state.preferredSchedule ?: "Morning",
        onSelect = onPreferredSchedule
    )

    Spacer(Modifier.height(16.dp))
    LimitationsChipGroup(
        selectedLimitation = state.limitationsPreferences ?: "None",
        onSelect = onLimitationsPreferences
    )
}

@Composable
private fun NumberField(
    label: String,
    initialValue: String,
    onChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = GymCoachColors.TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    text = input
                    onChange(input)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GymCoachColors.Primary,
                unfocusedBorderColor = GymCoachColors.SurfaceCardElevated,
                focusedTextColor = GymCoachColors.TextPrimary,
                unfocusedTextColor = GymCoachColors.TextPrimary,
                cursorColor = GymCoachColors.Primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SexSelection(
    selectedSex: String,
    onSelect: (String) -> Unit
) {
    var sex by remember { mutableStateOf(selectedSex) }
    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
        SEXES.forEach { (label, _) ->
            FilterChip(
                selected = sex == label,
                onClick = { sex = label; onSelect(sex) },
                label = { Text(label, color = if (sex == label) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GymCoachColors.SurfaceCardElevated,
                    selectedContainerColor = GymCoachColors.Primary,
                    labelColor = GymCoachColors.TextSecondary,
                    selectedLabelColor = GymCoachColors.TextPrimary
                )
            )
        }
    }
}

@Composable
private fun PreferredScheduleChipGroup(
    selectedSchedule: String,
    onSelect: (String) -> Unit
) {
    var schedule by remember { mutableStateOf(selectedSchedule) }
    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
        PREFERRED_SCHEDULES.forEach { (label, _) ->
            FilterChip(
                selected = schedule == label,
                onClick = { schedule = label; onSelect(schedule) },
                label = { Text(label, color = if (schedule == label) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GymCoachColors.SurfaceCardElevated,
                    selectedContainerColor = GymCoachColors.Primary,
                    labelColor = GymCoachColors.TextSecondary,
                    selectedLabelColor = GymCoachColors.TextPrimary
                )
            )
        }
    }
}

@Composable
private fun LimitationsChipGroup(
    selectedLimitation: String,
    onSelect: (String) -> Unit
) {
    var limitation by remember { mutableStateOf(selectedLimitation) }
    Column(verticalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
        LIMITATIONS.forEach { (label, _) ->
            FilterChip(
                selected = limitation == label,
                onClick = { limitation = label; onSelect(limitation) },
                label = { Text(label, color = if (limitation == label) GymCoachColors.TextPrimary else GymCoachColors.TextSecondary) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GymCoachColors.SurfaceCardElevated,
                    selectedContainerColor = GymCoachColors.Primary,
                    labelColor = GymCoachColors.TextSecondary,
                    selectedLabelColor = GymCoachColors.TextPrimary
                )
            )
        }
    }
}

@Composable
private fun ScheduleStep(
    state: OnboardingUiState,
    onDays: (Int) -> Unit,
    onMinutes: (Int) -> Unit
) {
    StepHeader("YOUR WEEK", "How often can you realistically train?")

    Text(
        text = "DAYS PER WEEK",
        style = MaterialTheme.typography.labelMedium,
        color = GymCoachColors.TextSecondary,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.sm)) {
        (2..6).forEach { days ->
            FilterChip(
                selected = state.daysPerWeek == days,
                onClick = { onDays(days) },
                label = { Text("$days") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GymCoachColors.SurfaceCardElevated,
                    selectedContainerColor = GymCoachColors.Primary,
                    labelColor = GymCoachColors.TextSecondary,
                    selectedLabelColor = GymCoachColors.TextPrimary
                )
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = "SESSION LENGTH - ${state.sessionMinutes} MIN",
        style = MaterialTheme.typography.labelMedium,
        color = GymCoachColors.TextSecondary,
        letterSpacing = 1.sp
    )
    Slider(
        value = state.sessionMinutes.toFloat(),
        onValueChange = { onMinutes(it.toInt()) },
        valueRange = 30f..90f,
        steps = 3,
        colors = SliderDefaults.colors(
            thumbColor = GymCoachColors.Primary,
            activeTrackColor = GymCoachColors.Primary,
            inactiveTrackColor = GymCoachColors.SurfaceCardElevated
        )
    )
}

@Composable
private fun EquipmentStep(state: OnboardingUiState, onToggle: (String) -> Unit) {
    StepHeader("YOUR EQUIPMENT", "Every exercise in your program will fit what you own.")
    EquipmentChecklist(
        availableEquipment = state.selectedEquipment,
        onToggle = onToggle
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = "No equipment yet? Bodyweight movements are always included.",
        style = MaterialTheme.typography.bodySmall,
        color = GymCoachColors.TextMuted
    )
}

@Composable
private fun ReviewStep(state: OnboardingUiState) {
    StepHeader("REVIEW", "Confirm and generate your first program.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewRow("Goal", state.goal ?: "-")
        ReviewRow("Experience", state.experience ?: "-")
        ReviewRow("Sex", state.sex ?: "-")
        ReviewRow("Age", "${state.age.toInt()} years")
        ReviewRow("Height", "${state.heightCm.toInt()} cm")
        ReviewRow("Weight", "${state.weightKg.toInt()} kg")
        ReviewRow("Schedule", state.preferredSchedule ?: "-")
        ReviewRow(
            "Limitations/Preferences",
            state.limitationsPreferences ?: "None"
        )
        ReviewRow("Equipment", if (state.selectedEquipment.isEmpty()) "Bodyweight only" else state.selectedEquipment.joinToString(", "))
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = GymCoachColors.TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = GymCoachColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BottomBar(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(GymCoachSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.step != OnboardingStep.WELCOME) {
            TextButton(onClick = onBack, enabled = !state.isGenerating) {
                Text(text = "BACK", color = GymCoachColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onNext,
            enabled = state.isStepValid && !state.isGenerating,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GymCoachColors.Primary,
                contentColor = GymCoachColors.TextPrimary,
                disabledContainerColor = GymCoachColors.SurfaceCardElevated
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = GymCoachColors.TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (state.isLastContentStep) "GENERATE PROGRAM" else "CONTINUE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}