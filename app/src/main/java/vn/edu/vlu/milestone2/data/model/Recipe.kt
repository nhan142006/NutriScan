package vn.edu.vlu.milestone2.data.model

import com.google.gson.annotations.SerializedName
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource

data class RecipeCategory(val name: String, val thumbnailUrl: String?)

data class RecipeSummary(val id: String, val name: String, val thumbnailUrl: String?)

/** Công thức đầy đủ (TheMealDB / Gemini / người dùng). */
data class Recipe(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val category: String?,
    val ingredients: List<String>,
    val steps: List<String>,
    val source: RecipeSource,
    val fitReason: String?,
    val basedOnScanIds: List<Long>,
    val youtubeUrl: String?,
    val isSaved: Boolean,
    val updatedAt: Long
)

/** 1 phần tử trong mảng JSON Gemini trả về (GeminiSchemas.RECIPE_SUGGESTIONS). */
data class RecipeSuggestion(
    @SerializedName("title") val title: String,
    @SerializedName("ingredients") val ingredients: List<String>,
    @SerializedName("steps") val steps: List<String>,
    @SerializedName("fitReason") val fitReason: String,
    @SerializedName("usedScannedItems") val usedScannedItems: List<String>
)

/** Huy hiệu tương thích của món ăn so với HealthProfile. */
data class RecipeCompatibility(val rating: Rating, val reasons: List<String>)

/**
 * Tham số truy vấn của module Công thức – được dựng từ HealthProfile + ScanResult gần đây
 * (yêu cầu "Feature Cohesion" của giảng viên).
 */
data class RecipeQuery(
    val profile: HealthProfile?,
    val preferredScans: List<ScanResult>,
    val excludedIngredients: List<String>,
    val focusScan: ScanResult?,
    val mainIngredient: String?
) {
    val scanIds: List<Long> get() = (listOfNotNull(focusScan) + preferredScans).map { it.scanId }.distinct()
}

/** Kết quả của mục "Dành cho bạn". */
data class ForYouResult(
    val query: RecipeQuery,
    val aiRecipes: List<Recipe>,
    val scannedIngredientMatches: List<RecipeSummary>,
    val aiError: vn.edu.vlu.milestone2.core.network.NetworkError?
)
