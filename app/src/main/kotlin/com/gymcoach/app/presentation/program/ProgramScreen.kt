package com.gymcoach.app.presentation.program

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymcoach.app.core.program.VolumeCalculator
import com.gymcoach.app.presentation.program.components.VolumeBar
import com.gymcoach.app.ui.theme.AccentBlue
import com.gymcoach.app.ui.theme.DarkBackground
import com.gymcoach.app.ui.theme.DarkSurface
import com.gymcoach.app.ui.theme.TextSecondary
import com.gymcoach.app.ui.theme.WarmWhite
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramScreen(
    onBackClick: () -> Unit,
    onStartWorkout: () -> Unit,
    viewModel: ProgramViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val exercises by viewModel.exercises.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Program") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            }
            state.program == null -> {
                EmptyProgramState(onBackClick = onBackClick)
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProgramHeader(program = state.program!!)
                    Spacer(Modifier.height(16.dp))

                    if (state.days.isNotEmpty()) {
                        VolumeOverviewCard(
                            days = state.days,
                            volumeCalculator = state.volumeCalculator
                        )
                        Spacer(Modifier.height(16.dp))

                        ProgramDaysList(
                            days = state.days,
                            exercises = state.exerciseMap
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    StartWorkoutButton(onClick = { onStartWorkout() })
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyProgramState(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "NO PROGRAM YET",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBlue,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Complete onboarding to generate your first V-taper program",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("BACK TO HOME", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun ProgramHeader(program: com.gymcoach.app.data.local.entity.ProgramEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = program.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = WarmWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Card(
                    modifier = Modifier.width(48.dp).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentBlue)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = WarmWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ProgramInfoChip(label = "Split", value = program.splitType.replace("_", " ").capitalize())
                ProgramInfoChip(label = "Weeks", value = "${program.durationWeeks}")
                ProgramInfoChip(label = "Days/Week", value = "${program.daysPerWeek}")
                ProgramInfoChip(label = "Difficulty", value = program.difficulty)
            }
            if (program.goal.isNotEmpty()) {
                Text(
                    text = "Goal: ${program.goal}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ProgramInfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun VolumeOverviewCard(
    days: List<ProgramDayUiModel>,
    volumeCalculator: com.gymcoach.app.core.program.VolumeCalculator?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "WEEKLY VOLUME OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${days.filter { !it.isRestDay }.sumOf { it.exercises.sumOf { it.sets } }} sets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days.filter { !it.isRestDay }) { day ->
                    VolumeBar(
                        label = day.name,
                        value = day.exercises.sumOf { it.sets }.toFloat(),
                        maxValue = 20f,
                        color = AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramDaysList(
    days: List<ProgramDayUiModel>,
    exercises: Map<Long, com.gymcoach.app.domain.model.Exercise>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        days.forEach { day ->
            if (day.isRestDay) {
                RestDayCard(day = day)
            } else {
                TrainingDayCard(day = day, exercises = exercises)
            }
        }
    }
}

@Composable
private fun RestDayCard(day: ProgramDayUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DarkSurface.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛌",
                        fontSize = 20.sp
                    )
                }
                Column {
                    Text(
                        text = "Day ${day.dayNumber} — ${day.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite
                    )
                    Text(
                        text = "Rest & Recovery",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingDayCard(
    day: ProgramDayUiModel,
    exercises: Map<Long, com.gymcoach.app.domain.model.Exercise>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Day ${day.dayNumber} — ${day.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite
                    )
                    if (day.targetMuscles.isNotEmpty()) {
                        Text(
                            text = day.targetMuscles.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = "${day.exercises.size} exercises · ${day.exercises.sumOf { it.sets }} sets",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                day.exercises.forEachIndexed { index, exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        number = index + 1,
                        isLast = index == day.exercises.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ProgramExerciseUiModel,
    number: Int,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelMedium,
            color = AccentBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = WarmWhite
            )
            if (exercise.muscleGroup.isNotEmpty()) {
                Text(
                    text = exercise.muscleGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${exercise.sets} × ${exercise.targetReps}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )
            if (exercise.targetWeightKg > 0) {
                Text(
                    text = "@ ${exercise.targetWeightKg} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "${exercise.restSeconds}s rest",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StartWorkoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "START TODAY'S WORKOUT",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}