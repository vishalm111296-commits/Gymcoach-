package com.gymcoach.app.domain.vshape.model

import java.time.Instant

data class MeasurementRecord(
    val id: Long = 0,
    val userId: String,
    val measurementType: MeasurementType,
    val value: Double,
    val unit: String,
    val date: Instant,
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)
