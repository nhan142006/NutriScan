package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_conditions",
    foreignKeys = [
        ForeignKey(
            entity = HealthProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["profile_id", "name"], unique = true)
    ]
)
data class HealthConditionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: Long,
    val name: String
)
