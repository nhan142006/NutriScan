package vn.edu.vlu.milestone2.data.remote.mealdb

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB - API công thức nấu ăn miễn phí (HTTPS, test key "1").
 * Base URL: https://www.themealdb.com/api/json/v1/1/
 */
interface MealDbApi {

    /** GET categories.php - danh sách nhóm món ăn. */
    @GET("categories.php")
    suspend fun getCategories(): CategoriesResponseDto

    /** GET filter.php?c={category} - danh sách món theo nhóm. */
    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): MealsResponseDto

    /** GET search.php?s={query} - tìm món theo tên. */
    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): MealsResponseDto

    /** GET lookup.php?i={id} - chi tiết 1 món (nguyên liệu + cách nấu). */
    @GET("lookup.php")
    suspend fun lookupMeal(@Query("i") id: String): MealDetailResponseDto
}
