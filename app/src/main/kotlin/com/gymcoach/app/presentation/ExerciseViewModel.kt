package com.gymcoach.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.Exercise
import com.gymcoach.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")

    val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            val queryFlow = MutableStateFlow(query)
            repository.getAllExercises().combine(queryFlow) { list, q ->
                if (q.isBlank()) list
                else list.filter { it.name.contains(q, ignoreCase = true) }
            }
        }
        .combine(selectedCategory) { list, category ->
            if (category == "All") list
            else list.filter { it.muscleGroup.equals(category, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        selectedCategory.value = category
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