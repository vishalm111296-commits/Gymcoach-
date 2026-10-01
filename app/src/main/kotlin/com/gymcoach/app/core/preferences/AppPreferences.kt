package com.gymcoach.app.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.gymcoach.app.core.audio.AudioCoachPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class WeightUnit(val code: String, val label: String) {
    KG("kg", "Metric (kg)"),
    LBS("lbs", "Imperial (lbs)");

    fun toDisplayWeight(weightKg: Double): Double = when (this) {
        KG -> weightKg
        LBS -> weightKg * LBS_PER_KG
    }

    fun toCanonicalKg(displayWeight: Double): Double = when (this) {
        KG -> displayWeight
        LBS -> displayWeight / LBS_PER_KG
    }

    companion object {
        const val LBS_PER_KG = 2.20462262185

        fun fromCode(code: String?): WeightUnit =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: KG

        fun kgToLbs(kg: Double): Double = kg * LBS_PER_KG
        fun lbsToKg(lbs: Double): Double = lbs / LBS_PER_KG
    }
}

data class AppPreferencesState(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val soundEnabled: Boolean = true,
    val audioPreset: AudioCoachPreset = AudioCoachPreset.CLASSIC_BEEPS,
    val autoStartRestTimer: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = true
)

interface AppPreferences {
    val preferencesState: StateFlow<AppPreferencesState>
    fun setWeightUnit(unit: WeightUnit)
    fun setSoundEnabled(enabled: Boolean)
    fun setAudioPreset(preset: AudioCoachPreset)
    fun setAutoStartRestTimer(enabled: Boolean)
    fun setVibrationEnabled(enabled: Boolean)
    fun setKeepScreenOn(enabled: Boolean)
}

class InMemoryAppPreferences(
    initialState: AppPreferencesState = AppPreferencesState()
) : AppPreferences {
    private val _state = MutableStateFlow(initialState)
    override val preferencesState: StateFlow<AppPreferencesState> = _state.asStateFlow()

    override fun setWeightUnit(unit: WeightUnit) {
        _state.value = _state.value.copy(weightUnit = unit)
    }

    override fun setSoundEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
    }

    override fun setAudioPreset(preset: AudioCoachPreset) {
        _state.value = _state.value.copy(audioPreset = preset)
    }

    override fun setAutoStartRestTimer(enabled: Boolean) {
        _state.value = _state.value.copy(autoStartRestTimer = enabled)
    }

    override fun setVibrationEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(vibrationEnabled = enabled)
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        _state.value = _state.value.copy(keepScreenOn = enabled)
    }
}

@Singleton
class DefaultAppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferences {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _preferencesState = MutableStateFlow(loadInitialState())
    override val preferencesState: StateFlow<AppPreferencesState> = _preferencesState.asStateFlow()

    private fun loadInitialState(): AppPreferencesState {
        return try {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val unitStr = p.getString(KEY_WEIGHT_UNIT, WeightUnit.KG.code)
            val sound = p.getBoolean(KEY_REST_SOUND, true)
            val presetStr = p.getString(KEY_REST_PRESET, AudioCoachPreset.CLASSIC_BEEPS.name)
            val autoStart = p.getBoolean(KEY_AUTO_START_REST, true)
            val vibration = p.getBoolean(KEY_REST_VIBRATION, true)
            val keepScreen = p.getBoolean(KEY_KEEP_SCREEN_ON, true)

            AppPreferencesState(
                weightUnit = WeightUnit.fromCode(unitStr),
                soundEnabled = sound,
                audioPreset = try {
                    AudioCoachPreset.valueOf(presetStr ?: AudioCoachPreset.CLASSIC_BEEPS.name)
                } catch (_: Exception) {
                    AudioCoachPreset.CLASSIC_BEEPS
                },
                autoStartRestTimer = autoStart,
                vibrationEnabled = vibration,
                keepScreenOn = keepScreen
            )
        } catch (_: Throwable) {
            AppPreferencesState()
        }
    }

    override fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString(KEY_WEIGHT_UNIT, unit.code).apply()
        _preferencesState.value = _preferencesState.value.copy(weightUnit = unit)
    }

    override fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REST_SOUND, enabled).apply()
        _preferencesState.value = _preferencesState.value.copy(soundEnabled = enabled)
    }

    override fun setAudioPreset(preset: AudioCoachPreset) {
        prefs.edit().putString(KEY_REST_PRESET, preset.name).apply()
        _preferencesState.value = _preferencesState.value.copy(audioPreset = preset)
    }

    override fun setAutoStartRestTimer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_REST, enabled).apply()
        _preferencesState.value = _preferencesState.value.copy(autoStartRestTimer = enabled)
    }

    override fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REST_VIBRATION, enabled).apply()
        _preferencesState.value = _preferencesState.value.copy(vibrationEnabled = enabled)
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
        _preferencesState.value = _preferencesState.value.copy(keepScreenOn = enabled)
    }

    companion object {
        const val PREFS_NAME = "gymcoach_prefs"
        const val KEY_WEIGHT_UNIT = "pref_weight_unit"
        const val KEY_REST_SOUND = "pref_rest_timer_sound"
        const val KEY_REST_PRESET = "pref_rest_timer_preset"
        const val KEY_AUTO_START_REST = "pref_auto_start_rest_timer"
        const val KEY_REST_VIBRATION = "pref_rest_timer_vibration"
        const val KEY_KEEP_SCREEN_ON = "pref_keep_screen_on"
    }
}
