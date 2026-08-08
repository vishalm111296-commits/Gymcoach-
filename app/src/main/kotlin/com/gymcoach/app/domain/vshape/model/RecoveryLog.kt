package com.gymcoach.app.domain.vshape.model

import java.time.Instant

data class RecoveryLog(
    val id: Long = 0,
    val userId: String,
    val date: Instant,
    val sleepHours: Double,
    val sorenessLevel: Int,
    val fatigueLevel: Int,
    val motivationLevel: Int,
    val recoveryScore: Int,
    val workoutCompleted: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val source: RecoverySource = RecoverySource.MANUAL
)

enum class RecoverySource {
    MANUAL,
    AUTO_DETECTED,
    WEARABLE,
    APP_FEEDBACK
}