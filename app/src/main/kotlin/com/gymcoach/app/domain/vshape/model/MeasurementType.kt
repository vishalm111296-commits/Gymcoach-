package com.gymcoach.app.domain.vshape.model

enum class MeasurementType(val displayName: String, val unit: String) {
    WEIGHT("Weight", "kg"),
    BODY_FAT("Body Fat", "%"),
    CHEST("Chest", "cm"),
    WAIST("Waist", "cm"),
    ARMS("Arms", "cm"),
    SHOULDERS("Shoulders", "cm"),
    HIPS("Hips", "cm"),
    THIGHS("Thighs", "cm"),
    CALVES("Calves", "cm"),
    NECK("Neck", "cm")
}
