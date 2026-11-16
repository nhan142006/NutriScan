package vn.edu.vlu.milestone2.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Công thức đã lưu / AI gợi ý / người dùng tự tạo – xem offline trong Chế độ nấu. */
@Entity(
    tableName = "recipes",
    indices = [Index(value = ["source"]), Index(value = ["is_saved"])]
)
data class RecipeEntity(
    @PrimaryKey @ColumnInfo(name = "recipe_id") val recipeId: String,
    val title: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    val category: String?,
    /** JSON array List<String>. */
    @ColumnInfo(name = "ingredients_json") val ingredientsJson: String,
    /** JSON array List<String> – từng bước nấu. */
    @ColumnInfo(name = "steps_json") val stepsJson: String,
    val source: RecipeSource,
    @ColumnInfo(name = "fit_reason") val fitReason: String?,
    /** Danh sách scanId (phân tách bằng dấu phẩy) đã dùng để AI gợi ý món này. */
    @ColumnInfo(name = "based_on_scan_ids") val basedOnScanIds: String?,
    @ColumnInfo(name = "youtube_url") val youtubeUrl: String?,
    @ColumnInfo(name = "is_saved") val isSaved: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
