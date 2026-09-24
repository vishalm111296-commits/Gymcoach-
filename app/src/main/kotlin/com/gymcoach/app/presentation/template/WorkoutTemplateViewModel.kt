package com.gymcoach.app.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.TemplateExercise
import com.gymcoach.app.domain.model.WorkoutTemplate
import com.gymcoach.app.domain.repository.WorkoutTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutTemplateViewModel @Inject constructor(
    private val repository: WorkoutTemplateRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    val templates: StateFlow<List<WorkoutTemplate>> = repository.getActiveTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedTemplates: StateFlow<List<WorkoutTemplate>> = repository.getArchivedTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissError() {
        _errorMessage.value = null
    }

    fun clearValidationErrors() {
        _validationErrors.value = emptyMap()
    }

    fun validateTemplate(name: String, exercises: List<TemplateExercise>): Boolean {
        val errors = mutableMapOf<String, String>()
        if (name.isBlank()) {
            errors["name"] = "Template name cannot be empty"
        }
        if (exercises.isEmpty()) {
            errors["exercises"] = "Template must have at least one exercise"
        }
        exercises.forEachIndexed { index, ex ->
            if (ex.targetSets <= 0) {
                errors["exercise_$index"] = "Sets must be at least 1"
            }
            if (ex.targetWeightKg < 0) {
                errors["exercise_${index}_weight"] = "Target weight cannot be negative"
            }
            if (ex.restSeconds < 0) {
                errors["exercise_${index}_rest"] = "Rest seconds cannot be negative"
            }
            if (ex.targetRpe != null && (ex.targetRpe <= 0.0 || ex.targetRpe > 10.0)) {
                errors["exercise_${index}_rpe"] = "RPE must be between 1.0 and 10.0"
            }
        }
        _validationErrors.value = errors
        return errors.isEmpty()
    }

    fun reorderExercises(exercises: List<TemplateExercise>, fromIndex: Int, toIndex: Int): List<TemplateExercise> {
        if (fromIndex !in exercises.indices || toIndex !in exercises.indices || fromIndex == toIndex) {
            return exercises
        }
        val mutable = exercises.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.mapIndexed { index, ex ->
            ex.copy(orderIndex = index)
        }
    }

    fun addExerciseToTemplate(exercises: List<TemplateExercise>, newExercise: TemplateExercise): List<TemplateExercise> {
        return exercises + newExercise.copy(orderIndex = exercises.size)
    }

    fun removeExerciseFromTemplate(exercises: List<TemplateExercise>, indexToRemove: Int): List<TemplateExercise> {
        if (indexToRemove !in exercises.indices) return exercises
        val mutable = exercises.toMutableList()
        mutable.removeAt(indexToRemove)
        return mutable.mapIndexed { index, ex ->
            ex.copy(orderIndex = index)
        }
    }

    fun saveTemplate(
        template: WorkoutTemplate,
        exercises: List<TemplateExercise>,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (!validateTemplate(template.name, exercises)) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = repository.saveTemplate(template, exercises)
                onSuccess(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save template"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun duplicateTemplate(templateId: Long, onSuccess: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newId = repository.duplicateTemplate(templateId)
                if (newId > 0) {
                    onSuccess(newId)
                } else {
                    _errorMessage.value = "Failed to duplicate template"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to duplicate template"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun archiveTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                repository.archiveTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to archive template"
            }
        }
    }

    fun unarchiveTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                repository.unarchiveTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to unarchive template"
            }
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete template"
            }
        }
    }

    fun startWorkoutFromTemplate(templateId: Long, onWorkoutStarted: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val workoutId = repository.startWorkoutFromTemplate(templateId)
                onWorkoutStarted(workoutId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to start workout from template"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
