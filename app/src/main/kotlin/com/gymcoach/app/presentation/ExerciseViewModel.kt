package com.gymcoach.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow("All")
    val filterDifficulty = MutableStateFlow("All")
    val filterEquipment = MutableStateFlow("All")

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val difficulties = listOf("All", "Beginner", "Intermediate", "Advanced")
    val equipments = listOf("All", "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Resistance Band")

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises = combine(
        searchQuery.debounce(300),
        filterCategory,
        filterDifficulty,
        filterEquipment
    ) { q, cat, diff, equip ->
        FilterState(q, cat, diff, equip)
    }.flatMapLatest { filters ->
        val catFilter = if (filters.category == "All") null else filters.category
        val diffFilter = if (filters.difficulty == "All") null else filters.difficulty
        val equipFilter = if (filters.equipment == "All") null else filters.equipment
        
        repository.getFilteredExercises(catFilter, diffFilter, equipFilter).map { list ->
            if (filters.query.isBlank()) list
            else list.filter { 
                it.name.contains(filters.query, ignoreCase = true) || 
                it.muscleGroup.contains(filters.query, ignoreCase = true) ||
                it.equipment.contains(filters.query, ignoreCase = true) ||
                it.category.contains(filters.query, ignoreCase = true) ||
                it.tags.contains(filters.query, ignoreCase = true)
            }
        }
    }.onEach { _isLoading.value = false }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterState(val query: String, val category: String, val difficulty: String, val equipment: String)

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
}