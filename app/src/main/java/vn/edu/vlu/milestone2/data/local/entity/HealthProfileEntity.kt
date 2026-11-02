package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_profiles")
data class HealthProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    val age: Int,
    @ColumnInfo(name = "daily_calorie_target")
    val dailyCalorieTarget: Int,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
