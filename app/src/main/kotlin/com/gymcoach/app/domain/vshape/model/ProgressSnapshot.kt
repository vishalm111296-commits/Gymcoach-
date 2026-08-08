package com.gymcoach.app.domain.vshape.model

import java.time.Instant

data class ProgressSnapshot(
    val id: Long = 0,
    val userId: String,
    val snapshotType: SnapshotType,
    val metrics: Map<String, Double>,
    val date: Instant,
    val createdAt: Instant = Instant.now(),
    val targetValues: Map<String, Double>? = null,
    val comparisonDate: Instant? = null
)

enum class SnapshotType {
    DAILY,
    WEEKLY,
    MONTHLY,
    CHALLENGE_PROGRESS,
    PERSONAL_BEST
}