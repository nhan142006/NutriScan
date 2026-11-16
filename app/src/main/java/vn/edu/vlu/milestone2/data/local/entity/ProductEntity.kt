package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cache sản phẩm từ Open Food Facts (cache-first lookup khi offline). */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val brand: String?,
    val quantity: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "nutri_score") val nutriScore: String?,
    @ColumnInfo(name = "nova_group") val novaGroup: Int?,
    val ingredients: String?,
    /** Danh sách allergen, phân tách bằng dấu phẩy. */
    val allergens: String,
    /** Giá trị dinh dưỡng /100 g dạng JSON (Gson). */
    @ColumnInfo(name = "nutrition_json") val nutritionJson: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

/** Sản phẩm yêu thích – FK tới [ProductEntity], mỗi barcode chỉ lưu 1 lần. */
@Entity(
    tableName = "favorite_products",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["product_barcode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["product_barcode"], unique = true)]
)
data class FavoriteProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_barcode") val productBarcode: String,
    @ColumnInfo(name = "added_at") val addedAt: Long
)
