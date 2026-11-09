package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.data.model.RecipeDetail
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDbApi
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDto

/**
 * Single Source of Truth cho dữ liệu món ăn:
 * Retrofit API -> RecipeRepository -> RecipeViewModel -> StateFlow -> ViewBinding UI.
 */
class RecipeRepository(
    private val api: MealDbApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getCategories(): NetworkResult<List<RecipeCategory>> =
        safeApiCall(ioDispatcher) { api.getCategories() }.map { response ->
            response.categories.orEmpty().mapNotNull { dto ->
                val name = dto.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                RecipeCategory(name = name, thumbnailUrl = dto.thumbnailUrl)
            }
        }

    suspend fun getRecipesByCategory(category: String): NetworkResult<List<RecipeSummary>> =
        safeApiCall(ioDispatcher) { api.filterByCategory(category) }
            .map { response -> response.meals.orEmpty().mapNotNull { it.toSummaryOrNull() } }

    suspend fun searchRecipes(query: String): NetworkResult<List<RecipeSummary>> =
        safeApiCall(ioDispatcher) { api.searchMeals(query.trim()) }
            .map { response -> response.meals.orEmpty().mapNotNull { it.toSummaryOrNull() } }

    suspend fun getRecipeDetail(id: String): NetworkResult<RecipeDetail> {
        val result = safeApiCall(ioDispatcher) { api.lookupMeal(id) }
        return when (result) {
            is NetworkResult.Error -> result
            is NetworkResult.Success -> {
                val meal = result.data.meals?.firstOrNull()
                    ?: return NetworkResult.Error(NetworkError.NOT_FOUND, httpCode = 404)
                NetworkResult.Success(meal.toRecipeDetail(fallbackId = id))
            }
        }
    }
}

private inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
}

private fun MealDto.toSummaryOrNull(): RecipeSummary? {
    val mealId = id?.takeIf { it.isNotBlank() } ?: return null
    val mealName = name?.takeIf { it.isNotBlank() } ?: return null
    return RecipeSummary(id = mealId, name = mealName, thumbnailUrl = thumbnailUrl)
}

private fun Map<String, String?>.toRecipeDetail(fallbackId: String): RecipeDetail {
    val ingredients = (1..MAX_INGREDIENTS).mapNotNull { index ->
        val ingredient = this["strIngredient$index"]?.trim().orEmpty()
        val measure = this["strMeasure$index"]?.trim().orEmpty()
        when {
            ingredient.isEmpty() -> null
            measure.isEmpty() -> ingredient
            else -> "$ingredient - $measure"
        }
    }
    return RecipeDetail(
        id = this["idMeal"] ?: fallbackId,
        name = this["strMeal"].orEmpty(),
        category = this["strCategory"],
        area = this["strArea"],
        thumbnailUrl = this["strMealThumb"],
        ingredients = ingredients,
        instructions = this["strInstructions"]?.trim().orEmpty(),
        youtubeUrl = this["strYoutube"]?.takeIf { it.isNotBlank() }
    )
}

private const val MAX_INGREDIENTS = 20
