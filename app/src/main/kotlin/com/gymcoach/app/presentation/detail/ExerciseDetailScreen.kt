package com.gymcoach.app.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.exercise.SubstitutionEngine
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import com.gymcoach.app.ui.theme.*
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
                ex?.let {
                    _isFavorite.value = it.isFavorite
                    loadSubstitutes(it)
                }
            }
        }
    }

    private suspend fun loadSubstitutes(exercise: Exercise) {
        try {
            val results = substitutionEngine.findSubstitutes(
                exerciseId = exercise.id,
                equipmentType = exercise.equipment,
                maxResults = 5
            )
            _substitutes.value = results
        } catch (e: Exception) {
            _substitutes.value = emptyList()
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

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            tint = if (isFavorite) ErrorRed else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        exercise?.let { ex ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // MEDIA FALLBACK
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 16.dp)
                        .background(DarkSurface, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "MEDIA UNAVAILABLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Offline assets not downloaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // HEADER & PRIMARY INFO
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = ex.name.uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.gymcoach.app.presentation.components.PremiumBadge(text = ex.muscleGroup, color = AccentBlue)
                        com.gymcoach.app.presentation.components.PremiumBadge(text = ex.equipment.ifBlank { "Bodyweight" }, color = DarkSurfaceVariant)
                        if (ex.difficulty.isNotBlank()) {
                            com.gymcoach.app.presentation.components.PremiumBadge(text = ex.difficulty, color = TextTertiary)
                        }
                    }
                    if (ex.secondaryMuscles.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Secondary: ${normalizeMuscleName(ex.secondaryMuscles)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // DESCRIPTION
                if (ex.description.isNotBlank()) {
                    ContentSection(title = "OVERVIEW", content = ex.description)
                }

                // INSTRUCTIONS
                if (ex.instructions.isNotBlank()) {
                    ContentSection(title = "INSTRUCTIONS", content = ex.instructions)
                }

                // TIPS
                if (ex.tips.isNotBlank()) {
                    ContentSection(title = "TECHNIQUE TIPS", content = ex.tips, isHighlight = true)
                }

                // COMMON MISTAKES
                if (ex.commonMistakes.isNotBlank()) {
                    ContentSection(title = "COMMON MISTAKES", content = ex.commonMistakes, isError = true)
                }

                // SUBSTITUTIONS
                if (substitutes.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = "SUGGESTED SUBSTITUTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        substitutes.forEach { sub ->
                            PremiumSubstitutionCard(
                                substituteName = sub.substitute.name,
                                reason = sub.reason,
                                onClick = { onExerciseClick(sub.substitute.id) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                com.gymcoach.app.presentation.components.PremiumEmptyState(
                    title = "NOT FOUND",
                    message = "This exercise could not be loaded."
                )
            }
        }
    }
}

@Composable
private fun ContentSection(title: String, content: String, isHighlight: Boolean = false, isError: Boolean = false) {
    val titleColor = when {
        isError -> ErrorRed
        isHighlight -> AccentBlue
        else -> TextTertiary
    }
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun PremiumSubstitutionCard(substituteName: String, reason: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = substituteName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "->",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Standardizes raw strings like 'front_deltoid' to 'Front Deltoid' */
private fun normalizeMuscleName(raw: String): String {
    return raw.split(',').joinToString(", ") { muscle ->
        muscle.trim().split('_').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}
