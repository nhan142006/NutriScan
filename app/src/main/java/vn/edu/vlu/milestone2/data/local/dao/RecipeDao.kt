package vn.edu.vlu.milestone2.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.milestone2.data.local.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE recipe_id = :recipeId")
    fun observe(recipeId: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE recipe_id = :recipeId")
    suspend fun get(recipeId: String): RecipeEntity?

    /** "Món của tôi" = đã lưu từ API/AI + tự tạo. */
    @Query("SELECT * FROM recipes WHERE is_saved = 1 OR source = 'USER' ORDER BY updated_at DESC")
    fun observeMyRecipes(): Flow<List<RecipeEntity>>

    /** Lần gợi ý AI gần nhất (cache để xem lại khi offline). */
    @Query("SELECT * FROM recipes WHERE source = 'AI' ORDER BY updated_at DESC LIMIT :limit")
    fun observeAiSuggestions(limit: Int): Flow<List<RecipeEntity>>

    @Upsert
    suspend fun upsert(recipe: RecipeEntity)

    @Upsert
    suspend fun upsertAll(recipes: List<RecipeEntity>)

    @Query("UPDATE recipes SET is_saved = :saved, updated_at = :now WHERE recipe_id = :recipeId")
    suspend fun setSaved(recipeId: String, saved: Boolean, now: Long): Int

    @Query("DELETE FROM recipes WHERE recipe_id = :recipeId")
    suspend fun delete(recipeId: String): Int

    /** Xóa gợi ý AI cũ chưa được lưu trước khi ghi lô gợi ý mới. */
    @Query("DELETE FROM recipes WHERE source = 'AI' AND is_saved = 0")
    suspend fun clearUnsavedAiSuggestions(): Int
}
