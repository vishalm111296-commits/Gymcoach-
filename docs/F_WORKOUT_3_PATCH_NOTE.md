    fun addExerciseToWorkout(exercise: Exercise) {
        viewModelScope.launch {
            // F-WORKOUT-3 fix: compute nextOrder inside the coroutine from the live
            // state at the time of DB write, not from a snapshot captured in the caller.
            // Two rapid addExercise calls could both read the same maxOrderIndex before
            // either DB insert completed, producing duplicate orderIndex values.
            val workout = _currentWorkout.value ?: return@launch
            val nextOrder = (workout.exercises.maxOfOrNull { it.workoutExercise.orderIndex } ?: -1) + 1
            workoutRepository.addExerciseToWorkout(workout.workout.id, exercise.id, nextOrder)
            _showExercisePicker.value = false
            // Load previous performance for newly added exercise
            val lastSets = workoutRepository.getLastSetsForExercise(exercise.id)
            if (lastSets.isNotEmpty()) {
                _previousPerformance.value = _previousPerformance.value + (exercise.id to lastSets)
            }
            val lastPerf = workoutRepository.getLastPerformanceForExercise(exercise.id)
            if (lastPerf != null) {
                _lastPerformanceSummary.value = _lastPerformanceSummary.value + (exercise.id to lastPerf)
            }
            // Calculate progression for new exercise
            val refreshed = _currentWorkout.value
            if (refreshed != null) {
                calculateProgressionRecommendations(refreshed.exercises)
            }
        }
    }
