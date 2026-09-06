package com.gymcoach.app.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.exercise.SubstitutionEngine
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val substitutionEngine: SubstitutionEngine
) : ViewModel() {
    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise.asStateFlow()

    private val _substitutes = MutableStateFlow<List<SubstitutionEngine.SubstitutionResult>>(emptyList())
    val substitutes: StateFlow<List<SubstitutionEngine.SubstitutionResult>> = _substitutes.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun loadExercise(id: Long) {
        viewModelScope.launch {
            repository.getExerciseById(id).collect { ex ->
                _exercise.value = ex
                if (ex != null) {
                    _isFavorite.value = ex.isFavorite
                    _substitutes.value = runCatching {
                        substitutionEngine.findSubstitutes(
                            exerciseId = ex.id,
                            equipmentType = ex.equipment,
                            maxResults = 5
                        )
                    }.getOrDefault(emptyList())
                } else {
                    _substitutes.value = emptyList()
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val ex = _exercise.value ?: return@launch
            val updated = ex.copy(isFavorite = !ex.isFavorite)
            repository.updateExercise(updated)
            _isFavorite.value = updated.isFavorite
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBackClick: () -> Unit,
    onExerciseClick: (Long) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val substitutes by viewModel.substitutes.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    LaunchedEffect(exerciseId) { viewModel.loadExercise(exerciseId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        val ex = exercise
        if (ex == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Exercise not found.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            MediaHero(exercise = ex)

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(ex.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    ex.description.ifBlank { "A resistance exercise selected for your current training goal." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                InfoChips(ex)

                Spacer(Modifier.height(20.dp))
                TrainingTargetCard(ex)

                if (ex.setupInstructions.isNotBlank() || ex.executionInstructions.isNotBlank() || ex.instructions.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    InstructionCard(ex)
                }

                if (ex.breathingInstructions.isNotBlank() || ex.tempoGuidance.isNotBlank() || ex.tips.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    TechniqueCard(ex)
                }

                if (substitutes.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SubstitutionSection(substitutes, onExerciseClick)
                }

                if (ex.commonMistakes.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    ContentCard(
                        title = "Common mistakes",
                        icon = Icons.Filled.Info,
                        body = ex.commonMistakes
                    )
                }

                if (ex.safetyNotes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    ContentCard(
                        title = "Safety notes",
                        icon = Icons.Filled.CheckCircle,
                        body = ex.safetyNotes,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MediaHero(exercise: Exercise) {
    val media = exercise.videoUrl ?: exercise.animationUrl ?: exercise.imageUrl
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(230.dp),
            contentAlignment = Alignment.Center
        ) {
            if (media.isNullOrBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Filled.FitnessCenter, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text("Technique guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Follow the setup, execution and breathing cues below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Media available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Playback is available from the exercise session when the media source is supported.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChips(ex: Exercise) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoChip(Icons.Filled.FitnessCenter, ex.muscleGroup.ifBlank { "Target muscle" })
        InfoChip(Icons.Filled.Build, ex.equipment.ifBlank { "Bodyweight" })
        InfoChip(Icons.Filled.LocalFireDepartment, ex.difficulty.ifBlank { "General" })
        if (ex.recommendedRepRange.isNotBlank()) InfoChip(Icons.Filled.TrendingUp, ex.recommendedRepRange)
        if (ex.recommendedRestTime.isNotBlank()) InfoChip(Icons.Filled.Info, ex.recommendedRestTime)
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TrainingTargetCard(ex: Exercise) {
    val targets = buildList {
        if (ex.vtaperLat > 0) add("Lats")
        if (ex.vtaperLateralDelt > 0) add("Lateral delts")
        if (ex.vtaperUpperChest > 0) add("Upper chest")
        if (ex.vtaperRearDelt > 0) add("Rear delts")
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Why this exercise is here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            if (targets.isNotEmpty()) {
                Text(
                    "Primary physique targets: ${targets.joinToString(", ") }.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                "These target labels describe the muscles the program prioritizes; they are planning heuristics, not a clinical or scientific body-shape score.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstructionCard(ex: Exercise) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("How to do it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LabeledText("Setup", ex.setupInstructions)
            LabeledText("Execution", ex.executionInstructions.ifBlank { ex.instructions })
        }
    }
}

@Composable
private fun TechniqueCard(ex: Exercise) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Technique cues", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LabeledText("Breathing", ex.breathingInstructions)
            LabeledText("Tempo", ex.tempoGuidance)
            LabeledText("Tips", ex.tips)
        }
    }
}

@Composable
private fun LabeledText(label: String, body: String) {
    if (body.isBlank()) return
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(3.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ContentCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    body: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SubstitutionSection(
    substitutes: List<SubstitutionEngine.SubstitutionResult>,
    onExerciseClick: (Long) -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Good alternatives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Use these when the required setup is unavailable.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            substitutes.forEach { result ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onExerciseClick(result.substitute.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(result.substitute.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${result.substitute.muscleGroup} · ${result.substitute.equipment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${result.preservationScore}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
