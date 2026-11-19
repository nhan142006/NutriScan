package vn.edu.vlu.milestone2.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.getOrNull
import vn.edu.vlu.milestone2.core.network.map
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.local.dao.RecipeDao
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import vn.edu.vlu.milestone2.data.model.ForYouResult
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.data.model.RecipeQuery
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiDataSource
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDbApi
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDto
import vn.edu.vlu.milestone2.data.remote.mealdb.MealsResponseDto
import vn.edu.vlu.milestone2.domain.HealthKeywords
import vn.edu.vlu.milestone2.domain.RecipeSearchTranslator
import java.util.UUID

/**
 * Module "Healthy Recipes & Cooking Guide".
 *
 * Ràng buộc #3 (Feature Cohesion): [buildQuery] đọc HealthProfile (HealthProfileDao) và các
 * ScanResult gần đây (ScanResultDao) rồi truyền chúng làm tham số truy vấn cho Gemini và
 * TheMealDB (filter.php?i={nguyên liệu từ sản phẩm đã quét}). Module không hoạt động độc lập.
 */
class RecipeRepository(
    private val api: MealDbApi,
    private val recipeDao: RecipeDao,
    private val gemini: GeminiDataSource,
    private val profileRepository: HealthProfileRepository,
    private val scanRepository: ScanRepository,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Dựng tham số truy vấn từ dữ liệu Room dùng chung của các module 3.1 → 3.3. */
    suspend fun buildQuery(focusScanId: Long?): RecipeQuery {
        val profile = profileRepository.getCurrentProfile()
        val focus = focusScanId?.let { scanRepository.getScan(it) }
        val usable = scanRepository.getRecentUsable(RECENT_SCAN_LIMIT)
        val avoided = scanRepository.getRecentAvoided(RECENT_SCAN_LIMIT)
        val excluded = buildList {
            addAll(profile?.allergies.orEmpty())
            avoided.forEach { scan ->
                add(scan.productName)
                scan.analysis?.flaggedIngredients?.forEach { add(it.name) }
            }
            focus?.analysis?.flaggedIngredients
                ?.filter { it.severity == vn.edu.vlu.milestone2.data.model.Severity.HIGH }
                ?.forEach { add(it.name) }
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

        val mainIngredient = (listOfNotNull(focus) + usable).firstNotNullOfOrNull { scan ->
            scan.analysis?.mainIngredientEn ?: HealthKeywords.guessMainIngredient(scan.productName, scan.ingredients.joinToString())
        }
        return RecipeQuery(
            profile = profile,
            preferredScans = usable.filter { it.scanId != focus?.scanId },
            excludedIngredients = excluded,
            focusScan = focus,
            mainIngredient = mainIngredient
        )
    }

    /** "Dành cho bạn": Gemini (profile + scans) + TheMealDB lọc theo nguyên liệu đã quét. */
    suspend fun loadForYou(focusScanId: Long?): ForYouResult =
        withContext(ioDispatcher) { loadForYouOnIo(focusScanId) }

    private suspend fun loadForYouOnIo(focusScanId: Long?): ForYouResult {
        val query = buildQuery(focusScanId)

        val matches = query.mainIngredient?.let { ingredient ->
            getRecipesByIngredient(ingredient).getOrNull()
        }.orEmpty().take(MATCH_LIMIT)

        val aiResult = gemini.suggestRecipes(query)
        val aiRecipes = when (aiResult) {
            is NetworkResult.Success -> {
                val now = System.currentTimeMillis()
                val recipes = aiResult.data.mapIndexed { index, suggestion ->
                    Recipe(
                        id = "ai_" + UUID.randomUUID().toString(),
                        title = suggestion.title,
                        imageUrl = null,
                        category = "Gợi ý AI",
                        ingredients = suggestion.ingredients,
                        steps = suggestion.steps,
                        source = RecipeSource.AI,
                        fitReason = buildFitReason(suggestion.fitReason, suggestion.usedScannedItems),
                        basedOnScanIds = query.scanIds,
                        youtubeUrl = null,
                        isSaved = false,
                        updatedAt = now - index // giữ đúng thứ tự Gemini trả về
                    )
                }
                recipeDao.clearUnsavedAiSuggestions()
                recipeDao.upsertAll(recipes.map { it.toEntity(gson) })
                recipes
            }
            is NetworkResult.Error -> emptyList()
        }
        return ForYouResult(
            query = query,
            aiRecipes = aiRecipes,
            scannedIngredientMatches = matches,
            aiError = (aiResult as? NetworkResult.Error)?.error
        )
    }

    /** Gợi ý AI đã cache trong Room – vẫn xem được khi offline. */
    fun observeCachedAiSuggestions(): Flow<List<Recipe>> =
        recipeDao.observeAiSuggestions(AI_CACHE_LIMIT).map { list -> list.map { it.toDomain(gson) } }.flowOn(ioDispatcher)

    fun observeMyRecipes(): Flow<List<Recipe>> =
        recipeDao.observeMyRecipes().map { list -> list.map { it.toDomain(gson) } }.flowOn(ioDispatcher)

    fun observeRecipe(id: String): Flow<Recipe?> = recipeDao.observe(id).map { it?.toDomain(gson) }.flowOn(ioDispatcher)

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

    suspend fun getRecipesByIngredient(ingredient: String): NetworkResult<List<RecipeSummary>> =
        safeApiCall(ioDispatcher) { api.filterByIngredient(ingredient) }
            .map { response -> response.meals.orEmpty().mapNotNull { it.toSummaryOrNull() } }

    /**
     * Tìm món bằng tiếng Việt hoặc tiếng Anh. TheMealDB chỉ có tên tiếng Anh nên từ khóa được dịch
     * offline ([RecipeSearchTranslator]: "gà rán" → "fried chicken"), rồi gọi song song search.php
     * theo tên, filter.php theo nguyên liệu/nhóm món; gộp, bỏ trùng và xếp món khớp nhiều từ khóa lên đầu.
     * Chỉ báo lỗi khi mọi request đều lỗi.
     */
    suspend fun searchRecipes(query: String): NetworkResult<List<RecipeSummary>> = coroutineScope {
        val plan = RecipeSearchTranslator.translate(query)
        val requests = buildList<suspend () -> MealsResponseDto> {
            plan.nameQueries.forEach { name -> add { api.searchMeals(name) } }
            plan.ingredientFilters.forEach { ingredient -> add { api.filterByIngredient(ingredient) } }
            plan.categories.forEach { category -> add { api.filterByCategory(category) } }
        }.take(MAX_SEARCH_REQUESTS)
        val results = requests
            .map { request -> async { safeApiCall(ioDispatcher) { request() } } }
            .awaitAll()
        val firstError = results.firstOrNull { it is NetworkResult.Error }
        if (firstError != null && results.all { it is NetworkResult.Error }) {
            firstError.map { emptyList<RecipeSummary>() }
        } else {
            val meals = results
                .flatMap { it.getOrNull()?.meals.orEmpty() }
                .mapNotNull { it.toSummaryOrNull() }
                .distinctBy { it.id }
            val ranked = meals.withIndex()
                .sortedWith(compareByDescending<IndexedValue<RecipeSummary>> { plan.score(it.value.name) }.thenBy { it.index })
                .map { it.value }
                .take(SEARCH_RESULT_LIMIT)
            NetworkResult.Success(ranked)
        }
    }

    /** Cache-first: món đã lưu/AI/tự tạo đọc từ Room; món TheMealDB lấy API rồi cache để nấu offline. */
    suspend fun loadRecipe(id: String): NetworkResult<Recipe> = withContext(ioDispatcher) { loadRecipeOnIo(id) }

    private suspend fun loadRecipeOnIo(id: String): NetworkResult<Recipe> {
        recipeDao.get(id)?.let { return NetworkResult.Success(it.toDomain(gson)) }
        val result = safeApiCall(ioDispatcher) { api.lookupMeal(id) }
        return when (result) {
            is NetworkResult.Error -> result
            is NetworkResult.Success -> {
                val meal = result.data.meals?.firstOrNull()
                    ?: return NetworkResult.Error(NetworkError.NOT_FOUND, httpCode = 404)
                val recipe = meal.toRecipe(fallbackId = id)
                recipeDao.upsert(recipe.toEntity(gson))
                NetworkResult.Success(recipe)
            }
        }
    }

    suspend fun setSaved(recipeId: String, saved: Boolean) {
        recipeDao.setSaved(recipeId, saved, System.currentTimeMillis())
    }

    /** CRUD "Món của tôi": tạo mới khi [id] = null, ngược lại cập nhật. */
    suspend fun saveUserRecipe(
        id: String?,
        title: String,
        category: String?,
        ingredients: List<String>,
        steps: List<String>
    ): String = withContext(ioDispatcher) {
        val recipeId = id ?: ("user_" + UUID.randomUUID().toString())
        val existing = id?.let { recipeDao.get(it) }
        recipeDao.upsert(
            Recipe(
                id = recipeId,
                title = title.trim(),
                imageUrl = existing?.imageUrl,
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                ingredients = ingredients,
                steps = steps,
                source = RecipeSource.USER,
                fitReason = null,
                basedOnScanIds = emptyList(),
                youtubeUrl = null,
                isSaved = true,
                updatedAt = System.currentTimeMillis()
            ).toEntity(gson)
        )
        recipeId
    }

    suspend fun deleteRecipe(id: String) {
        recipeDao.delete(id)
    }

    private fun buildFitReason(reason: String, used: List<String>): String =
        if (used.isEmpty()) reason else "$reason\nDùng sản phẩm đã quét: ${used.joinToString()}"

    companion object {
        const val RECENT_SCAN_LIMIT = 10
        const val MATCH_LIMIT = 10
        const val AI_CACHE_LIMIT = 6
        const val MAX_SEARCH_REQUESTS = 6
        const val SEARCH_RESULT_LIMIT = 60

        /** Dùng cho UI: map Rating → nhãn tiếng Việt. */
        fun ratingLabel(rating: Rating): String = when (rating) {
            Rating.SUITABLE -> "Phù hợp"
            Rating.LIMIT -> "Hạn chế"
            Rating.AVOID -> "Tránh"
        }
    }
}

private fun MealDto.toSummaryOrNull(): RecipeSummary? {
    val mealId = id?.takeIf { it.isNotBlank() } ?: return null
    val mealName = name?.takeIf { it.isNotBlank() } ?: return null
    return RecipeSummary(id = mealId, name = mealName, thumbnailUrl = thumbnailUrl)
}

private fun Map<String, String?>.toRecipe(fallbackId: String): Recipe {
    val ingredients = (1..MAX_INGREDIENTS).mapNotNull { index ->
        val ingredient = this["strIngredient$index"]?.trim().orEmpty()
        val measure = this["strMeasure$index"]?.trim().orEmpty()
        when {
            ingredient.isEmpty() -> null
            measure.isEmpty() -> ingredient
            else -> "$ingredient – $measure"
        }
    }
    val steps = this["strInstructions"].orEmpty()
        .split("\r\n", "\n")
        .map { it.trim().removePrefix("STEP").trim() }
        .filter { it.length > 3 && !it.matches(Regex("^\\d+$")) }
    return Recipe(
        id = this["idMeal"] ?: fallbackId,
        title = this["strMeal"].orEmpty(),
        imageUrl = this["strMealThumb"],
        category = listOfNotNull(this["strCategory"], this["strArea"]).joinToString(" · ").ifBlank { null },
        ingredients = ingredients,
        steps = steps,
        source = RecipeSource.API,
        fitReason = null,
        basedOnScanIds = emptyList(),
        youtubeUrl = this["strYoutube"]?.takeIf { it.isNotBlank() },
        isSaved = false,
        updatedAt = System.currentTimeMillis()
    )
}

private const val MAX_INGREDIENTS = 20
