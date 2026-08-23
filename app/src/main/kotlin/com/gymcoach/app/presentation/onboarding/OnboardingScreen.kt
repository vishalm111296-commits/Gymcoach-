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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcoach.app.presentation.onboarding.components.EquipmentChecklist
import com.gymcoach.app.presentation.onboarding.components.GoalSelectionCard
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.AccentBlueDim
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurfaceVariant
import com.gymcoach.app.ui.theme.ErrorRed
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.TextTertiary
import com.gymcoach.app.ui.theme.WarmWhite

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

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val order = OnboardingStep.entries
    val stepIndex = order.indexOf(state.step)

    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    Scaffold(containerColor = DarkBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = (stepIndex.coerceAtMost(order.indexOf(OnboardingStep.REVIEW))) /
                    order.indexOf(OnboardingStep.REVIEW).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = AccentBlue,
                trackColor = DarkSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                when (state.step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.GOAL -> GoalStep(state, viewModel::selectGoal)
                    OnboardingStep.EXPERIENCE -> ExperienceStep(state, viewModel::selectExperience)
                    OnboardingStep.PERSONAL_INFO -> PersonalInfoStep(
                        state,
                        viewModel::setAge,
                        viewModel::setHeight,
                        viewModel::setWeight
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
                state.error?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = ErrorRed)
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
            color = WarmWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "BUILD YOUR\nV-TAPER",
            style = MaterialTheme.typography.displaySmall,
            color = WarmWhite,
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Answer six quick questions and your first program is built around your goal, schedule, and equipment.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Spacer(Modifier.height(28.dp))
        listOf(
            "Programs matched to the equipment you have",
            "Weekly volume tuned to evidence-based ranges",
            "Your first session ready in minutes"
        ).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun GoalStep(state: OnboardingUiState, onSelect: (String) -> Unit) {
    StepHeader("WHAT IS THE GOAL?", "Pick your primary target. Everything else tunes around it.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    onWeight: (Float) -> Unit
) {
    StepHeader("THE BASICS", "Used to calibrate pacing and starting loads.")

    Text(
        text = "AGE - ${state.age.toInt()} YEARS",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        letterSpacing = 1.sp
    )
    Slider(
        value = state.age,
        onValueChange = onAge,
        valueRange = 14f..80f,
        steps = 65,
        colors = SliderDefaults.colors(
            thumbColor = AccentBlue,
            activeTrackColor = AccentBlue,
            inactiveTrackColor = DarkSurfaceVariant
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
            color = TextSecondary,
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
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = WarmWhite,
                unfocusedTextColor = WarmWhite,
                cursorColor = AccentBlue
            ),
            modifier = Modifier.fillMaxWidth()
        )
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
        color = TextSecondary,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (2..6).forEach { days ->
            FilterChip(
                selected = state.daysPerWeek == days,
                onClick = { onDays(days) },
                label = { Text("$days") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurfaceVariant,
                    selectedContainerColor = AccentBlueDim,
                    labelColor = TextSecondary,
                    selectedLabelColor = WarmWhite
                )
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = "SESSION LENGTH - ${state.sessionMinutes} MIN",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        letterSpacing = 1.sp
    )
    Slider(
        value = state.sessionMinutes.toFloat(),
        onValueChange = { onMinutes(it.toInt()) },
        valueRange = 30f..90f,
        steps = 3,
        colors = SliderDefaults.colors(
            thumbColor = AccentBlue,
            activeTrackColor = AccentBlue,
            inactiveTrackColor = DarkSurfaceVariant
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
        color = TextTertiary
    )
}

@Composable
private fun ReviewStep(state: OnboardingUiState) {
    StepHeader("REVIEW", "Confirm and generate your first program.")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewRow("Goal", state.goal ?: "-")
        ReviewRow("Experience", state.experience ?: "-")
        ReviewRow("Age", "${state.age.toInt()} years")
        ReviewRow("Height", "${state.heightCm.toInt()} cm")
        ReviewRow("Weight", "${state.weightKg.toInt()} kg")
        ReviewRow("Schedule", "${state.daysPerWeek} days / ${state.sessionMinutes} min")
        ReviewRow(
            "Equipment",
            if (state.selectedEquipment.isEmpty()) "Bodyweight only" else state.selectedEquipment.joinToString(", ")
        )
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmWhite,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.step != OnboardingStep.WELCOME) {
            TextButton(onClick = onBack, enabled = !state.isGenerating) {
                Text(text = "BACK", color = TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onNext,
            enabled = state.isStepValid && !state.isGenerating,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                contentColor = WarmWhite,
                disabledContainerColor = DarkSurfaceVariant
            ),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            if (state.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = WarmWhite,
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
