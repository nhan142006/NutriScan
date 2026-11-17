package vn.edu.vlu.milestone2.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity

data class ProfileWithItems(
    @Embedded val profile: HealthProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profile_id")
    val items: List<HealthProfileItemEntity>
)

/** Kết quả JOIN favorite_products ⨝ products. */
data class FavoriteWithProduct(
    @Embedded val product: vn.edu.vlu.milestone2.data.local.entity.ProductEntity,
    val addedAt: Long
)
