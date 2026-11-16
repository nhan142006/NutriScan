package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Hồ sơ sức khỏe – mỗi tài khoản Firebase (userId) có đúng 1 hồ sơ. */
@Entity(
    tableName = "health_profiles",
    indices = [Index(value = ["user_id"], unique = true)]
)
data class HealthProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val age: Int,
    @ColumnInfo(name = "diet_goal") val dietGoal: String,
    @ColumnInfo(name = "daily_calorie_target") val dailyCalorieTarget: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

/** Tình trạng bệnh / dị ứng – quan hệ 1-n với [HealthProfileEntity], xóa dây chuyền. */
@Entity(
    tableName = "health_profile_items",
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
        Index(value = ["profile_id", "type", "name"], unique = true)
    ]
)
data class HealthProfileItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "profile_id") val profileId: Long,
    val type: ProfileItemType,
    val name: String
)
