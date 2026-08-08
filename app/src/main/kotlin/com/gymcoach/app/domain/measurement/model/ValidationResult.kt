package com.gymcoach.app.domain.measurement.model

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)