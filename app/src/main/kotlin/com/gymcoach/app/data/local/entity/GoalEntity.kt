package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalType: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String,
    val startDate: Long,
    val targetDate: Long,
    val status: String,
    val createdAt: Long,
    val priority: String,
    val notes: String? = null
)

@androidx.room.TypeConverters
class GoalTypeConverter {
    fun fromEnumToString(type: com.gymcoach.app.domain.vshape.model.GoalType): String {
        return type.name
    }
    
    fun fromStringToEnum(value: String): com.gymcoach.app.domain.vshape.model.GoalType {
        return com.gymcoach.app.domain.vshape.model.GoalType.valueOf(value)
    }
}

@androidx.room.TypeConverters
class GoalStatusConverter {
    fun fromEnumToString(status: com.gymcoach.app.domain.vshape.model.GoalStatus): String {
        return status.name
    }
    
    fun fromStringToEnum(value: String): com.gymcoach.app.domain.vshape.model.GoalStatus {
        return com.gymcoach.app.domain.vshape.model.GoalStatus.valueOf(value)
    }
}

@androidx.room.TypeConverters
class GoalPriorityConverter {
    fun fromEnumToString(priority: com.gymcoach.app.domain.vshape.model.GoalPriority): String {
        return priority.name
    }
    
    fun fromStringToEnum(value: String): com.gymcoach.app.domain.vshape.model.GoalPriority {
        return com.gymcoach.app.domain.vshape.model.GoalPriority.valueOf(value)
    }
}