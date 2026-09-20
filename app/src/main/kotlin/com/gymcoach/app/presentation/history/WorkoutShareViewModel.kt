package com.gymcoach.app.presentation.history

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcoach.app.core.share.WorkoutShareCardBuilder
import com.gymcoach.app.core.share.WorkoutShareCardRenderer
import com.gymcoach.app.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShareState {
    data object Idle : ShareState()
    data object Rendering : ShareState()
    data class Success(val uri: Uri) : ShareState()
    data class Error(val msg: String) : ShareState()
}

@HiltViewModel
class WorkoutShareViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val shareCardBuilder: WorkoutShareCardBuilder,
    private val shareCardRenderer: WorkoutShareCardRenderer
) : ViewModel() {

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState.asStateFlow()

    fun generateStoryCard(workoutId: Long, context: Context) {
        viewModelScope.launch {
            _shareState.value = ShareState.Rendering
            try {
                val workout = workoutRepository.getWorkoutWithDetails(workoutId).firstOrNull()
                if (workout == null) {
                    _shareState.value = ShareState.Error("Workout not found")
                    return@launch
                }
                
                val cardData = shareCardBuilder.buildShareData(workout)
                val bitmap = shareCardRenderer.renderToBitmap(cardData)
                val uri = shareCardRenderer.saveShareImage(context, bitmap, workoutId)
                
                if (uri != null) {
                    _shareState.value = ShareState.Success(uri)
                } else {
                    _shareState.value = ShareState.Error("Failed to save image")
                }
            } catch (e: Exception) {
                _shareState.value = ShareState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    fun resetState() {
        _shareState.value = ShareState.Idle
    }
}
