package com.gymcoach.app.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSettingsState())
    val state: StateFlow<ProfileSettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _state.value = ProfileSettingsState(
            profileSyncEnabled = prefs.getBoolean(KEY_PROFILE_SYNC, true),
            autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true),
            syncFrequency = prefs.getString(KEY_SYNC_FREQUENCY, "daily") ?: "daily",
            dataUsageLimit = prefs.getLong(KEY_DATA_USAGE_LIMIT, 100 * 1024 * 1024), // 100MB default
            compressData = prefs.getBoolean(KEY_COMPRESS_DATA, true)
        )
    }

    fun onProfileSyncToggle(enabled: Boolean) {
        _state.value = _state.value.copy(profileSyncEnabled = enabled)
        savePreference(context, KEY_PROFILE_SYNC, enabled)
    }

    fun onAutoSyncToggle(enabled: Boolean) {
        _state.value = _state.value.copy(autoSync = enabled)
        savePreference(context, KEY_AUTO_SYNC, enabled)
    }

    fun onSyncFrequencyChange(frequency: String) {
        _state.value = _state.value.copy(syncFrequency = frequency)
        savePreference(context, KEY_SYNC_FREQUENCY, frequency)
    }

    fun onDataUsageLimitChange(limit: Long) {
        _state.value = _state.value.copy(dataUsageLimit = limit)
        savePreference(context, KEY_DATA_USAGE_LIMIT, limit)
    }

    fun onCompressDataToggle(enabled: Boolean) {
        _state.value = _state.value.copy(compressData = enabled)
        savePreference(context, KEY_COMPRESS_DATA, enabled)
    }

    private fun savePreference(context: Context, key: String, value: Any) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        when (value) {
            is Boolean -> prefs.putBoolean(key, value)
            is Long -> prefs.putLong(key, value)
            is String -> prefs.putString(key, value)
        }
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "GymCoachProfileSettings"
        private const val KEY_PROFILE_SYNC = "profile_sync_enabled"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
        private const val KEY_SYNC_FREQUENCY = "sync_frequency"
        private const val KEY_DATA_USAGE_LIMIT = "data_usage_limit_bytes"
        private const val KEY_COMPRESS_DATA = "compress_data"
    }
}

data class ProfileSettingsState(
    val profileSyncEnabled: Boolean = true,
    val autoSync: Boolean = true,
    val syncFrequency: String = "daily",
    val dataUsageLimit: Long = 100 * 1024 * 1024,
    val compressData: Boolean = true
)
