package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Daily readiness/recovery data.
 * Stores subjective user-reported metrics for recovery tracking.
 * 
 * All scores are 1-5 scale:
 * - Sleep quality: 1=terrible, 5=excellent
 * - Soreness: 1=very sore, 5=no soreness
 * - Energy: 1=exhausted, 5=energized
 * - Motivation: 1=no motivation, 5=highly motivated
 * 
 * Readiness score is computed as average of all metrics (1.0-5.0).
 * Conservative recommendation: if avg < 3.0, suggest lighter session.
 */
@Entity(tableName = "readiness")
data class ReadinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 1,
    
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "sleep_quality")
    val sleepQuality: Int = 3,  // 1-5
    
    @ColumnInfo(name = "soreness")
    val soreness: Int = 3,  // 1-5 (1=very sore, 5=no soreness)
    
    @ColumnInfo(name = "energy")
    val energy: Int = 3,  // 1-5
    
    @ColumnInfo(name = "motivation")
    val motivation: Int = 3,  // 1-5
    
    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    /**
     * Computed readiness score (1.0-5.0).
     * Average of all four subjective metrics.
     */
    val readinessScore: Double
        get() = (sleepQuality + soreness + energy + motivation) / 4.0
    
    /**
     * Conservative training recommendation based on readiness.
     * Does NOT claim physiological measurement.
     * Does NOT claim hormone/testosterone detection.
     * Simply provides a subjective recommendation based on user-reported data.
     */
    val trainingRecommendation: String
        get() = when {
            readinessScore >= 4.0 -> "Full intensity session recommended"
            readinessScore >= 3.0 -> "Moderate session recommended"
            readinessScore >= 2.0 -> "Light session or active recovery recommended"
            else -> "Rest day recommended. Listen to your body."
        }
    
    val isRestDayRecommended: Boolean
        get() = readinessScore < 2.5
}
