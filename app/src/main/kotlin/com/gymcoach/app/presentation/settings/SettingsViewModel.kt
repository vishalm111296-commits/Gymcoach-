package com.gymcoach.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val hapticFeedback: Boolean = true,
    val autoStartRestTimer: Boolean = true,
    val darkTheme: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true),
            autoStartRestTimer = prefs.getBoolean(KEY_REST_TIMER, true),
            darkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun toggleHapticFeedback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _uiState.update { it.copy(hapticFeedback = enabled) }
    }

    fun toggleAutoStartRestTimer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REST_TIMER, enabled).apply()
        _uiState.update { it.copy(autoStartRestTimer = enabled) }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _uiState.update { it.copy(darkTheme = enabled) }
    }

    companion object {
        private const val PREFS_NAME = "gymcoach_settings"
        private const val KEY_HAPTIC = "haptic_feedback"
        private const val KEY_REST_TIMER = "auto_start_rest_timer"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}
