package com.gymcoach.app.core.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymcoach.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class BackupManager(private val context: Context, private val json: Json = Json { ignoreUnknownKeys = true }) {

    companion object {
        private const val WORKOUT_EXPORT_FILE = "gymcoach_backup.json"
    }

    interface ExportProgressListener {
        fun onExportProgress(percent: Int)
        fun onExportComplete(files: List<File>)
    }

    interface RestoreProgressListener {
        fun onRestoreProgress(percent: Int)
        fun onRestoreComplete()
    }

    suspend fun getBackupVersions(): List<BackupMetadata> {
        val backupFiles = context.getExternalFilesDir(null)?.listFiles { _, name ->
            name.endsWith(".json")
        }?.filterNotNull()?.sortedByDescending { it.lastModified() } ?: emptyList()

        return backupFiles.mapNotNull { file ->
            try {
                val content = file.readText()
                json.decodeFromString<BackupMetadata>(content)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun exportBackup(listener: ExportProgressListener? = null) {
        try {
            listener?.onExportProgress(10)

            val exportJson = BackupMetadata(
                version = BuildConfig.VERSION_CODE,
                packageName = context.packageName,
                exportDate = System.currentTimeMillis(),
                workouts = emptyList(),
                exercises = emptyList(),
                settings = emptyMap()
            )

            listener?.onExportProgress(50)

            val content = json.encodeToString(exportJson)
            val file = File(context.getExternalFilesDir(null), WORKOUT_EXPORT_FILE)
            file.writeText(content)

            listener?.onExportProgress(100)
            listener?.onExportComplete(listOf(file))

        } catch (e: Exception) {
            throw BackupException("Export failed", e)
        }
    }

    suspend fun createBackup(): BackupMetadata {
        val exportJson = BackupMetadata(
            version = BuildConfig.VERSION_CODE,
            packageName = context.packageName,
            exportDate = System.currentTimeMillis(),
            workouts = emptyList(),
            exercises = emptyList(),
            settings = createSettingsSnapshot()
        )
        return exportJson
    }

    suspend fun restoreBackup(fromFile: File, listener: RestoreProgressListener? = null): Boolean {
        try {
            val content = fromFile.readText()
            val metadata = json.decodeFromString<BackupMetadata>(content)

            listener?.onRestoreProgress(30)

            listener?.onRestoreProgress(100)
            listener?.onRestoreComplete()

            return true
        } catch (e: Exception) {
            throw BackupException("Restore failed: ${e.message}", e)
        }
    }

    private fun createSettingsSnapshot(): Map<String, String> {
        val settings = HashMap<String, String>()
        settings["theme"] = ""
        settings["units"] = ""
        settings["hapticFeedback"] = "true"
        return settings
    }

    fun getExportFile(): File? {
        return try {
            File(context.getExternalFilesDir(null), WORKOUT_EXPORT_FILE)
        } catch (e: Exception) {
            null
        }
    }

    fun clearExportFile() {
        getExportFile()?.delete()
    }

    fun interface Listener {
        fun onExportProgress(percent: Int)
        fun onRestoreProgress(percent: Int)
    }

    suspend fun isBackupAvailable(): Boolean {
        return getExportFile()?.exists() ?: false
    }
}

data class BackupMetadata(
    val version: Int,
    val packageName: String,
    val exportDate: Long,
    val workouts: List<WorkoutExportData>,
    val exercises: List<ExerciseExportData>,
    val settings: Map<String, String>
)

data class WorkoutExportData(
    val id: Long,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val notes: String,
    val completed: Boolean,
    val exercises: List<ExerciseExportData>
)

data class ExerciseExportData(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val sets: List<SetExportData>
)

data class SetExportData(
    val id: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val restSeconds: Int,
    val completed: Boolean,
    val setType: SetType
)

enum class SetType {
    NORMAL, WARMUP, DROP, FAILURE
}

class BackupException(message: String, cause: Throwable) : Exception(message, cause)