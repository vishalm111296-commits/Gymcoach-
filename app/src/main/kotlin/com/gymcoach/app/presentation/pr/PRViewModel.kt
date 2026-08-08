package com.gymcoach.app.presentation.pr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.domain.model.WorkoutWithDetails
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class PRViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _allWorkouts = MutableStateFlow<List<WorkoutWithDetails>>(emptyList())

    private val _prCategories = MutableStateFlow(PRCategories())
    val prCategories: StateFlow<PRCategories> = _prCategories.asStateFlow()

    private val _recentPrs = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val recentPrs: StateFlow<List<PersonalRecord>> = _recentPrs.asStateFlow()

    private val _exerciseSpecificPrs = MutableStateFlow<Map<Long, List<PersonalRecord>>>(emptyMap())
    val exerciseSpecificPrs: StateFlow<Map<Long, List<PersonalRecord>>> = _exerciseSpecificPrs.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            workoutRepository.getAllWorkoutsWithDetails().collect { workouts ->
                _allWorkouts.value = workouts
                calculatePRs()
            }
        }
    }

    private fun calculatePRs() {
        val workouts = _allWorkouts.value
        if (workouts.isEmpty()) {
            _prCategories.value = PRCategories()
            _recentPrs.value = emptyList()
            _exerciseSpecificPrs.value = emptyMap()
            return
        }

        val allPrs = mutableListOf<PersonalRecord>()
        val exercisePrsMap = mutableMapOf<Long, MutableList<PersonalRecord>>()

        var highestWeight = PersonalRecord(type = PRType.HIGHEST_WEIGHT, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var highestVolume = PersonalRecord(type = PRType.HIGHEST_VOLUME, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var highestReps = PersonalRecord(type = PRType.HIGHEST_REPS, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var longestWorkout = PersonalRecord(type = PRType.LONGEST_WORKOUT, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var fastestWorkout = PersonalRecord(type = PRType.FASTEST_WORKOUT, value = Double.MAX_VALUE, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var mostExercises = PersonalRecord(type = PRType.MOST_EXERCISES, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var mostSets = PersonalRecord(type = PRType.MOST_SETS, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A")
        var mostCalories = PersonalRecord(type = PRType.MOST_CALORIES, value = 0.0, date = Instant.EPOCH, workoutId = 0, exerciseName = "N/A") // Assuming calorie tracking is available

        workouts.sortedByDescending { it.workout.date }.forEach { workoutWithDetails ->
            val workout = workoutWithDetails.workout
            val exercises = workoutWithDetails.exercises

            // Overall workout stats
            if (workout.duration > longestWorkout.value) {
                longestWorkout = PersonalRecord(PRType.LONGEST_WORKOUT, workout.duration.toDouble(), workout.date, workout.id)
                allPrs.add(longestWorkout)
            }
            if (workout.duration < fastestWorkout.value && workout.duration > 0) {
                fastestWorkout = PersonalRecord(PRType.FASTEST_WORKOUT, workout.duration.toDouble(), workout.date, workout.id)
                allPrs.add(fastestWorkout)
            }
            val numExercises = exercises.size.toDouble()
            if (numExercises > mostExercises.value) {
                mostExercises = PersonalRecord(PRType.MOST_EXERCISES, numExercises, workout.date, workout.id)
                allPrs.add(mostExercises)
            }
            val numSets = exercises.sumOf { it.sets.size }.toDouble()
            if (numSets > mostSets.value) {
                mostSets = PersonalRecord(PRType.MOST_SETS, numSets, workout.date, workout.id)
                allPrs.add(mostSets)
            }
            // Calories are not currently in Workout model, assuming 0 for now.
            // if (workout.caloriesBurned > mostCalories.value) { ... }


            exercises.forEach { workoutExerciseWithSets ->
                val exerciseId = workoutExerciseWithSets.exercise.id
                val exerciseName = workoutExerciseWithSets.exercise.name

                workoutExerciseWithSets.sets.forEach { workoutSet ->
                    val totalWeight = workoutSet.weight * workoutSet.reps
                    if (totalWeight > highestVolume.value) {
                        highestVolume = PersonalRecord(PRType.HIGHEST_VOLUME, totalWeight, workout.date, workout.id, exerciseName)
                        allPrs.add(highestVolume)
                    }
                    if (workoutSet.weight > highestWeight.value) {
                        highestWeight = PersonalRecord(PRType.HIGHEST_WEIGHT, workoutSet.weight, workout.date, workout.id, exerciseName)
                        allPrs.add(highestWeight)
                    }
                    if (workoutSet.reps > highestReps.value) {
                        highestReps = PersonalRecord(PRType.HIGHEST_REPS, workoutSet.reps.toDouble(), workout.date, workout.id, exerciseName)
                        allPrs.add(highestReps)
                    }

                    // Exercise-specific PRs
                    if (!exercisePrsMap.containsKey(exerciseId)) {
                        exercisePrsMap[exerciseId] = mutableListOf()
                    }
                    val currentExercisePrs = exercisePrsMap[exerciseId]!!

                    // Highest Weight per exercise
                    val existingHighestWeight = currentExercisePrs.find { it.type == PRType.HIGHEST_WEIGHT && it.exerciseName == exerciseName }
                    if (existingHighestWeight == null || workoutSet.weight > existingHighestWeight.value) {
                        existingHighestWeight?.let { currentExercisePrs.remove(it) }
                        val pr = PersonalRecord(PRType.HIGHEST_WEIGHT, workoutSet.weight, workout.date, workout.id, exerciseName)
                        currentExercisePrs.add(pr)
                        allPrs.add(pr)
                    }

                    // Highest Volume per exercise
                    val existingHighestVolume = currentExercisePrs.find { it.type == PRType.HIGHEST_VOLUME && it.exerciseName == exerciseName }
                    if (existingHighestVolume == null || totalWeight > existingHighestVolume.value) {
                        existingHighestVolume?.let { currentExercisePrs.remove(it) }
                        val pr = PersonalRecord(PRType.HIGHEST_VOLUME, totalWeight, workout.date, workout.id, exerciseName)
                        currentExercisePrs.add(pr)
                        allPrs.add(pr)
                    }

                    // Highest Reps per exercise
                    val existingHighestReps = currentExercisePrs.find { it.type == PRType.HIGHEST_REPS && it.exerciseName == exerciseName }
                    if (existingHighestReps == null || workoutSet.reps > existingHighestReps.value) {
                        existingHighestReps?.let { currentExercisePrs.remove(it) }
                        val pr = PersonalRecord(PRType.HIGHEST_REPS, workoutSet.reps.toDouble(), workout.date, workout.id, exerciseName)
                        currentExercisePrs.add(pr)
                        allPrs.add(pr)
                    }
                }
            }
        }

        _prCategories.value = PRCategories(
            highestWeight = highestWeight,
            highestVolume = highestVolume,
            highestReps = highestReps,
            longestWorkout = longestWorkout,
            fastestWorkout = fastestWorkout.copy(value = if (fastestWorkout.value == Double.MAX_VALUE) 0.0 else fastestWorkout.value),
            mostExercises = mostExercises,
            mostSets = mostSets,
            mostCalories = mostCalories
        )

        // Filter and sort recent PRs (last 10 unique PRs)
        _recentPrs.value = allPrs
            .sortedByDescending { it.date }
            .distinctBy { Pair(it.type, it.exerciseName) } // Keep only the latest for each type/exercise combo
            .take(10)
            .sortedByDescending { it.date }

        _exerciseSpecificPrs.value = exercisePrsMap.mapValues { (_, prs) ->
            prs.sortedByDescending { it.date }
        }
    }

    fun onExerciseSelected(exerciseId: Long?) {
        _selectedExerciseId.value = exerciseId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    // Helper data classes
    data class PersonalRecord(
        val type: PRType,
        val value: Double,
        val date: Instant,
        val workoutId: Long,
        val exerciseName: String? = null // For exercise-specific PRs
    )

    enum class PRType {
        HIGHEST_WEIGHT,
        HIGHEST_VOLUME,
        HIGHEST_REPS,
        LONGEST_WORKOUT,
        FASTEST_WORKOUT,
        MOST_EXERCISES,
        MOST_SETS,
        MOST_CALORIES
    }

    data class PRCategories(
        val highestWeight: PersonalRecord = PersonalRecord(PRType.HIGHEST_WEIGHT, 0.0, Instant.EPOCH, 0),
        val highestVolume: PersonalRecord = PersonalRecord(PRType.HIGHEST_VOLUME, 0.0, Instant.EPOCH, 0),
        val highestReps: PersonalRecord = PersonalRecord(PRType.HIGHEST_REPS, 0.0, Instant.EPOCH, 0),
        val longestWorkout: PersonalRecord = PersonalRecord(PRType.LONGEST_WORKOUT, 0.0, Instant.EPOCH, 0),
        val fastestWorkout: PersonalRecord = PersonalRecord(PRType.FASTEST_WORKOUT, Double.MAX_VALUE, Instant.EPOCH, 0),
        val mostExercises: PersonalRecord = PersonalRecord(PRType.MOST_EXERCISES, 0.0, Instant.EPOCH, 0),
        val mostSets: PersonalRecord = PersonalRecord(PRType.MOST_SETS, 0.0, Instant.EPOCH, 0),
        val mostCalories: PersonalRecord = PersonalRecord(PRType.MOST_CALORIES, 0.0, Instant.EPOCH, 0)
    )
}