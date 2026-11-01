package vn.edu.vlu.milestone2.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import vn.edu.vlu.milestone2.data.local.entity.HealthConditionEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity

data class ProfileWithConditions(
    @Embedded
    val profile: HealthProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profile_id"
    )
    val conditions: List<HealthConditionEntity>
)
