package com.gymcoach.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long = System.currentTimeMillis(),
    // in cm
    @ColumnInfo(name = "weight_kg") val weightKg: Double = 0.0,
    @ColumnInfo(name = "body_fat_pct") val bodyFatPct: Double? = null,
    @ColumnInfo(name = "chest_cm") val chestCm: Double? = null,
    @ColumnInfo(name = "waist_cm") val waistCm: Double? = null,
    @ColumnInfo(name = "hips_cm") val hipsCm: Double? = null,
    @ColumnInfo(name = "shoulders_cm") val shouldersCm: Double? = null,
    @ColumnInfo(name = "left_arm_cm") val leftArmCm: Double? = null,
    @ColumnInfo(name = "right_arm_cm") val rightArmCm: Double? = null,
    @ColumnInfo(name = "left_thigh_cm") val leftThighCm: Double? = null,
    @ColumnInfo(name = "right_thigh_cm") val rightThighCm: Double? = null,
    @ColumnInfo(name = "left_calf_cm") val leftCalfCm: Double? = null,
    @ColumnInfo(name = "right_calf_cm") val rightCalfCm: Double? = null,
    @ColumnInfo(name = "notes") val notes: String = ""
)
