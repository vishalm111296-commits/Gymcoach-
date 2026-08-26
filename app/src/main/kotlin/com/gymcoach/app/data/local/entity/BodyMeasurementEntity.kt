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
    @ColumnInfo(name = "body_fat_pct") val bodyFatPct: Double = 0.0,
    @ColumnInfo(name = "chest_cm") val chestCm: Double = 0.0,
    @ColumnInfo(name = "waist_cm") val waistCm: Double = 0.0,
    @ColumnInfo(name = "hips_cm") val hipsCm: Double = 0.0,
    @ColumnInfo(name = "shoulders_cm") val shouldersCm: Double = 0.0,
    @ColumnInfo(name = "left_arm_cm") val leftArmCm: Double = 0.0,
    @ColumnInfo(name = "right_arm_cm") val rightArmCm: Double = 0.0,
    @ColumnInfo(name = "left_thigh_cm") val leftThighCm: Double = 0.0,
    @ColumnInfo(name = "right_thigh_cm") val rightThighCm: Double = 0.0,
    @ColumnInfo(name = "left_calf_cm") val leftCalfCm: Double = 0.0,
    @ColumnInfo(name = "right_calf_cm") val rightCalfCm: Double = 0.0,
    @ColumnInfo(name = "notes") val notes: String = ""
)
