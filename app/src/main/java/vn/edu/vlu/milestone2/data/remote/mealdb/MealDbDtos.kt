package vn.edu.vlu.milestone2.data.remote.mealdb

import com.google.gson.annotations.SerializedName

data class CategoriesResponseDto(
    @SerializedName("categories") val categories: List<CategoryDto>?
)

data class CategoryDto(
    @SerializedName("idCategory") val id: String?,
    @SerializedName("strCategory") val name: String?,
    @SerializedName("strCategoryThumb") val thumbnailUrl: String?
)

/** TheMealDB trả về `"meals": null` khi không có kết quả. */
data class MealsResponseDto(
    @SerializedName("meals") val meals: List<MealDto>?
)

data class MealDto(
    @SerializedName("idMeal") val id: String?,
    @SerializedName("strMeal") val name: String?,
    @SerializedName("strMealThumb") val thumbnailUrl: String?,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?
)

/**
 * Chi tiết món có 20 cặp strIngredientN / strMeasureN nên được đọc dưới dạng Map
 * rồi chuyển đổi trong Repository.
 */
data class MealDetailResponseDto(
    @SerializedName("meals") val meals: List<Map<String, String?>>?
)
