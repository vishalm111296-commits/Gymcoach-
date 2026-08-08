package com.gymcoach.app.presentation.settings

import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bell
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsState(
    val isDarkMode: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val volumeEnabled: Boolean = false,
    val vibrationEnabled: Boolean = false,
    val defaultRestTimerSeconds: Int = 90,
    val isAutoStartRestTimer: Boolean = true,
    val isMetricUnits: Boolean = true,
    val plateUnit: PlateUnit = PlateUnit.STANDARD_PLATES,
    val isSetCountEnabled: Boolean = true,
    val isAnimatedTransitions: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val notificationSoundUri: String? = null,
    val isBackupEnabled: Boolean = false,
    val isDebugMode: Boolean = false,
    val versionName: String = "0.1.0",
    val versionCode: Int = 1
)

enum class PlateUnit {
    STANDARD_PLATES,
    KG_PLATES,
    OLYMPIC_PLATES,
    METRIC_PLATES
}

sealed class SettingsScreenSection(val id: Int, val title: String, val icon: ImageVector) {
    data object General : SettingsScreenSection(1, "General", Icons.Default.Settings)
    data object Appearance : SettingsScreenSection(2, "Appearance", Icons.Default.DarkMode)
    data object Notifications : SettingsScreenSection(3, "Notifications", Icons.Default.Bell)
    data object Workout : SettingsScreenSection(4, "Workout", Icons.Default.Restart)
    data object RestTimer : SettingsScreenSection(5, "Rest Timer", Icons.Default.RestartAlt)
    data object Units : SettingsScreenSection(6, "Units", Icons.Default.Scale)
    data object PlateCalculator : SettingsScreenSection(7, "Plate Calculator", Icons.Default.Layers)
    data object History : SettingsScreenSection(8, "History", Icons.Default.History)
    data object Backup : SettingsScreenSection(9, "Backup", Icons.Default.Backup)
    data object Developer : SettingsScreenSection(10, "Developer", Icons.Default.Terminal)
    data object About : SettingsScreenSection(11, "About", Icons.Default.Info)
}

@Singleton
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val isDarkMode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE, false)
        val isNotificationsEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS, true)
        val volumeEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOLUME, false)
        val vibrationEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true)
        val defaultRestTimer = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REST_TIMER, 90)
        val isAutoStartRestTimer = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_START_REST_TIMER, true)
        val isMetricUnits = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_METRIC_UNITS, true)
        val plateUnit = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLATE_UNIT, PlateUnit.STANDARD_PLATES.name)
            ?.let { PlateUnit.valueOf(it) } ?: PlateUnit.STANDARD_PLATES
        val isSetCountEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SET_COUNT_ENABLED, true)
        val isAnimatedTransitions = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ANIMATED_TRANSITIONS, true)
        val notificationSoundUri = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFICATION_SOUND, null)
        val isBackupEnabled = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BACKUP_ENABLED, false)
        val isDebugMode = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG_MODE, false)

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            PackageInfo(versionName = "0.1.0", versionCode = 1)
        }

        _state.value = SettingsState(
            isDarkMode = isDarkMode,
            isNotificationsEnabled = isNotificationsEnabled,
            volumeEnabled = volumeEnabled,
            vibrationEnabled = vibrationEnabled,
            defaultRestTimerSeconds = defaultRestTimer,
            isAutoStartRestTimer = isAutoStartRestTimer,
            isMetricUnits = isMetricUnits,
            plateUnit = plateUnit,
            isSetCountEnabled = isSetCountEnabled,
            isAnimatedTransitions = isAnimatedTransitions,
            isVibrationEnabled = vibrationEnabled,
            notificationSoundUri = notificationSoundUri,
            isBackupEnabled = isBackupEnabled,
            isDebugMode = isDebugMode,
            versionName = packageInfo.versionName,
            versionCode = packageInfo.versionCode
        )
    }

    fun setDarkMode(enabled: Boolean) {
        _state.value = _state.value.copy(isDarkMode = enabled)
        savePreference(context, KEY_DARK_MODE, enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isNotificationsEnabled = enabled)
        savePreference(context, KEY_NOTIFICATIONS, enabled)
    }

    fun setVolumeEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(volumeEnabled = enabled)
        savePreference(context, KEY_VOLUME, enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isVibrationEnabled = enabled)
        savePreference(context, KEY_VIBRATION, enabled)
    }

    fun setDefaultRestTimer(seconds: Int) {
        _state.value = _state.value.copy(defaultRestTimerSeconds = seconds)
        savePreference(context, KEY_REST_TIMER, seconds)
    }

    fun setAutoStartRestTimer(enabled: Boolean) {
        _state.value = _state.value.copy(isAutoStartRestTimer = enabled)
        savePreference(context, KEY_AUTO_START_REST_TIMER, enabled)
    }

    fun setMetricUnits(enabled: Boolean) {
        _state.value = _state.value.copy(isMetricUnits = enabled)
        savePreference(context, KEY_METRIC_UNITS, enabled)
    }

    fun setPlateUnit(unit: PlateUnit) {
        _state.value = _state.value.copy(plateUnit = unit)
        savePreference(context, KEY_PLATE_UNIT, unit.name)
    }

    fun setSetCountEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isSetCountEnabled = enabled)
        savePreference(context, KEY_SET_COUNT_ENABLED, enabled)
    }

    fun setAnimatedTransitions(enabled: Boolean) {
        _state.value = _state.value.copy(isAnimatedTransitions = enabled)
        savePreference(context, KEY_ANIMATED_TRANSITIONS, enabled)
    }

    fun setNotificationSoundUri(uri: String?) {
        _state.value = _state.value.copy(notificationSoundUri = uri)
        savePreference(context, KEY_NOTIFICATION_SOUND, uri)
    }

    fun setBackupEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isBackupEnabled = enabled)
        savePreference(context, KEY_BACKUP_ENABLED, enabled)
    }

    fun setDebugMode(enabled: Boolean) {
        _state.value = _state.value.copy(isDebugMode = enabled)
        savePreference(context, KEY_DEBUG_MODE, enabled)
    }

    private fun savePreference(context: Context, key: String, value: Any) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        when (value) {
            is Boolean -> prefs.putBoolean(key, value)
            is Int -> prefs.putInt(key, value)
            is String -> prefs.putString(key, value)
        }
        prefs.apply()
    }

    companion object {
        private const val PREFS_NAME = "GymCoachSettings"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_VOLUME = "volume_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_REST_TIMER = "rest_timer_default"
        private const val KEY_AUTO_START_REST_TIMER = "auto_start_rest_timer"
        private const val KEY_METRIC_UNITS = "metric_units"
        private const val KEY_PLATE_UNIT = "plate_unit"
        private const val KEY_SET_COUNT_ENABLED = "set_count_enabled"
        private const val KEY_ANIMATED_TRANSITIONS = "animated_transitions"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound_uri"
        private const val KEY_BACKUP_ENABLED = "backup_enabled"
        private const val KEY_DEBUG_MODE = "debug_mode"

        data class PackageInfo(
            val versionName: String,
            val versionCode: Int
        )
    }
}