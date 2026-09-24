package com.gymcoach.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.animation.AnimationRepository
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val animationRepository: AnimationRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow("All")
    val filterDifficulty = MutableStateFlow("All")
    val filterEquipment = MutableStateFlow("All")
    val filterMovementPattern = MutableStateFlow("All")
    val showFavoritesOnly = MutableStateFlow(false)
    val showAnimationOnly = MutableStateFlow(false)
    val showCameraCoachOnly = MutableStateFlow(false)

    val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val difficulties = listOf("All", "Beginner", "Intermediate", "Advanced")
    val equipments = listOf("All", "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Resistance Band")
    val movementPatterns = listOf("All", "Squat", "Hinge", "Push", "Pull", "Lunge", "Carry", "Isolation")

    private data class BaseFilters(
        val category: String,
        val difficulty: String,
        val equipment: String,
        val movementPattern: String
    )

    private data class ToggleFilters(
        val favoritesOnly: Boolean,
        val animationOnly: Boolean,
        val cameraCoachOnly: Boolean
    )

    private val baseFilters = combine(
        filterCategory,
        filterDifficulty,
        filterEquipment,
        filterMovementPattern
    ) { cat, diff, equip, pattern ->
        BaseFilters(cat, diff, equip, pattern)
    }

    private val toggleFilters = combine(
        showFavoritesOnly,
        showAnimationOnly,
        showCameraCoachOnly
    ) { favs, anim, cam ->
        ToggleFilters(favs, anim, cam)
    }

    private val filterGroup = combine(baseFilters, toggleFilters) { base, toggles ->
        SubFilters(
            category = base.category,
            difficulty = base.difficulty,
            equipment = base.equipment,
            movementPattern = base.movementPattern,
            favoritesOnly = toggles.favoritesOnly,
            animationOnly = toggles.animationOnly,
            cameraCoachOnly = toggles.cameraCoachOnly
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val exercises = combine(
        searchQuery.debounce(300),
        filterGroup
    ) { q, sub ->
        FilterState(q, sub.category, sub.difficulty, sub.equipment, sub.movementPattern, sub.favoritesOnly, sub.animationOnly, sub.cameraCoachOnly)
    }.flatMapLatest { filters ->
        val baseFlow = if (filters.query.isNotBlank()) {
            repository.searchExercises(filters.query)
        } else {
            val catFilter = if (filters.category == "All") null else filters.category
            val diffFilter = if (filters.difficulty == "All") null else filters.difficulty
            val equipFilter = if (filters.equipment == "All") null else filters.equipment
            repository.getFilteredExercises(catFilter, diffFilter, equipFilter)
        }
        
        baseFlow.map { list ->
            list.filter { exercise ->
                val matchesCategory = filters.category == "All" || exercise.muscleGroup.equals(filters.category, ignoreCase = true)
                val matchesDifficulty = filters.difficulty == "All" || exercise.difficulty.equals(filters.difficulty, ignoreCase = true)
                val matchesEquipment = filters.equipment == "All" || exercise.equipment.contains(filters.equipment, ignoreCase = true)
                val matchesPattern = filters.movementPattern == "All" || exercise.movementPattern.equals(filters.movementPattern, ignoreCase = true)
                val matchesFav = !filters.favoritesOnly || exercise.isFavorite
                val matchesAnim = !filters.animationOnly || (_animatedExerciseNames.value.contains(exercise.name.trim().lowercase()) || !exercise.animationUrl.isNullOrBlank())
                val matchesCamera = !filters.cameraCoachOnly || (com.gymcoach.app.core.ml.ExerciseType.fromExerciseName(exercise.name) != null)
                matchesCategory && matchesDifficulty && matchesEquipment && matchesPattern && matchesFav && matchesAnim && matchesCamera
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class SubFilters(
        val category: String,
        val difficulty: String,
        val equipment: String,
        val movementPattern: String,
        val favoritesOnly: Boolean,
        val animationOnly: Boolean,
        val cameraCoachOnly: Boolean
    )

    private data class FilterState(
        val query: String,
        val category: String,
        val difficulty: String,
        val equipment: String,
        val movementPattern: String,
        val favoritesOnly: Boolean,
        val animationOnly: Boolean,
        val cameraCoachOnly: Boolean
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        filterCategory.value = category
    }

    fun onDifficultySelected(difficulty: String) {
        filterDifficulty.value = difficulty
    }

    fun onEquipmentSelected(equipment: String) {
        filterEquipment.value = equipment
    }

    fun onMovementPatternSelected(pattern: String) {
        filterMovementPattern.value = pattern
    }

    fun toggleFavoritesOnly() {
        showFavoritesOnly.value = !showFavoritesOnly.value
    }

    fun toggleAnimationOnly() {
        showAnimationOnly.value = !showAnimationOnly.value
    }

    fun toggleCameraCoachOnly() {
        showCameraCoachOnly.value = !showCameraCoachOnly.value
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise.copy(isFavorite = !exercise.isFavorite))
        }
    }


    private val _animatedExerciseNames = MutableStateFlow<Set<String>>(emptySet())
    val animatedExerciseNames: StateFlow<Set<String>> = _animatedExerciseNames.asStateFlow()

    init {
        viewModelScope.launch {
            val names = animationRepository.getAllAnimations().map { it.exerciseName.trim().lowercase() }.toSet()
            _animatedExerciseNames.value = names
        }
    }

    suspend fun hasAnimation(name: String): Boolean {
        return animationRepository.hasAnimation(name)
    }


    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.addExercise(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun createCustomExercise(
        name: String,
        muscleGroup: String,
        equipment: String,
        difficulty: String = "Intermediate",
        notes: String = "",
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.createCustomExercise(
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                difficulty = difficulty,
                notes = notes
            )
            onCreated(id)
        }
    }

    fun deleteCustomExercise(id: Long, onDeleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deleteCustomExercise(id)
            onDeleted(success)
        }
    }
}
