package com.gymcoach.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.Instant

@Entity(tableName = "measurement_records")
data class MeasurementRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val measurementType: String,
    val value: Double,
    val unit: String,
    val date: Long,
    val notes: String? = null,
    val createdAt: Long
)

@TypeConverters
class MeasurementTypeConverter {
    fun fromEnumToString(type: com.gymcoach.app.domain.vshape.model.MeasurementType): String {
        return type.name
    }
    
    fun fromStringToEnum(value: String): com.gymcoach.app.domain.vshape.model.MeasurementType {
        return com.gymcoach.app.domain.vshape.model.MeasurementType.valueOf(value)
    }
}