package vn.edu.vlu.milestone2.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.data.model.RecipeQuery
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.domain.RecipeCompatibilityChecker
import vn.edu.vlu.milestone2.ui.NavArgs

/** Trạng thái mục "Dành cho bạn". */
data class ForYouUiState(
    val isLoading: Boolean = false,
    val query: RecipeQuery? = null,
    val aiRecipes: List<RecipeCardItem> = emptyList(),
    val matches: List<RecipeCardItem> = emptyList(),
    val aiError: NetworkError? = null,
    val showingCachedAi: Boolean = false
)

/**
 * RecipeViewModel – kết hợp (combine) Flow HealthProfile + kết quả gợi ý dựng từ ScanResult
 * (ràng buộc Feature Cohesion). Không phải trình duyệt công thức độc lập.
 */
class RecipesViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    profileRepository: HealthProfileRepository,
    private val checker: RecipeCompatibilityChecker
) : ViewModel() {

    private val focusScanArg: Long = savedStateHandle.get<Long>(NavArgs.FOCUS_SCAN_ID) ?: -1L
    private val _focusScanId = MutableStateFlow(focusScanArg.takeIf { it > 0 })
    val focusScanId: StateFlow<Long?> = _focusScanId.asStateFlow()

    private val forYouResult = MutableStateFlow(ForYouUiState(isLoading = true))

    private val profile: StateFlow<HealthProfile?> = profileRepository.observeCurrentProfile()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** "Dành cho bạn" = kết quả mới nhất, hoặc gợi ý AI đã cache trong Room khi offline. */
    val forYou: StateFlow<ForYouUiState> = combine(
        forYouResult,
        recipeRepository.observeCachedAiSuggestions(),
        profile
    ) { state, cachedAi, currentProfile ->
        if (state.aiRecipes.isEmpty() && state.aiError != null && cachedAi.isNotEmpty()) {
            state.copy(aiRecipes = cachedAi.map { it.toCard(currentProfile) }, showingCachedAi = true)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForYouUiState(isLoading = true))

    val myRecipes: StateFlow<List<RecipeCardItem>> = combine(recipeRepository.observeMyRecipes(), profile) { list, p ->
        list.map { it.toCard(p) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _categories = MutableStateFlow<List<RecipeCategory>>(emptyList())
    val categories: StateFlow<List<RecipeCategory>> = _categories.asStateFlow()

    private val _browse = MutableStateFlow<UiState<List<RecipeCardItem>>>(UiState.Idle)
    val browse: StateFlow<UiState<List<RecipeCardItem>>> = _browse.asStateFlow()

    private var forYouJob: Job? = null
    private var browseJob: Job? = null
    private var lastBrowse: (suspend () -> NetworkResult<List<RecipeSummary>>)? = null

    init {
        refreshForYou()
        loadCategories()
    }

    fun refreshForYou() {
        forYouJob?.cancel()
        forYouJob = viewModelScope.launch {
            forYouResult.value = forYouResult.value.copy(isLoading = true)
            val result = recipeRepository.loadForYou(_focusScanId.value)
            val currentProfile = profile.value
            forYouResult.value = ForYouUiState(
                isLoading = false,
                query = result.query,
                aiRecipes = result.aiRecipes.map { it.toCard(currentProfile) },
                matches = result.scannedIngredientMatches.map { it.toCard() },
                aiError = result.aiError
            )
        }
    }

    fun clearFocus() {
        _focusScanId.value = null
        refreshForYou()
    }

    fun selectCategory(name: String) = runBrowse { recipeRepository.getRecipesByCategory(name) }

    fun search(query: String) {
        val text = query.trim()
        if (text.isNotEmpty()) runBrowse { recipeRepository.searchRecipes(text) }
    }

    fun retryBrowse() {
        lastBrowse?.let { runBrowse(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = recipeRepository.getCategories()) {
                is NetworkResult.Success -> {
                    _categories.value = result.data
                    // Danh mục mặc định phụ thuộc mục tiêu ăn uống trong HealthProfile.
                    val goal = profile.value?.dietGoal.orEmpty().lowercase()
                    val preferred = when {
                        goal.contains("chay") -> "Vegetarian"
                        goal.contains("giảm") -> "Seafood"
                        goal.contains("cơ") -> "Chicken"
                        else -> "Vegetarian"
                    }
                    (result.data.firstOrNull { it.name == preferred } ?: result.data.firstOrNull())
                        ?.let { selectCategory(it.name) }
                }
                is NetworkResult.Error -> _browse.value = UiState.Error(result.error, result.httpCode)
            }
        }
    }

    private fun runBrowse(block: suspend () -> NetworkResult<List<RecipeSummary>>) {
        lastBrowse = block
        if (_categories.value.isEmpty()) loadCategoriesOnly()
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _browse.value = UiState.Loading
            _browse.value = when (val result = block()) {
                is NetworkResult.Success -> UiState.Success(result.data.map { it.toCard() })
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
            }
        }
    }

    private fun loadCategoriesOnly() {
        viewModelScope.launch {
            (recipeRepository.getCategories() as? NetworkResult.Success)?.let { _categories.value = it.data }
        }
    }

    private fun Recipe.toCard(profile: HealthProfile?) = RecipeCardItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        subtitle = fitReason?.lineSequence()?.firstOrNull() ?: category,
        badge = checker.check(ingredients, profile)?.rating,
        isAi = source == RecipeSource.AI
    )

    private fun RecipeSummary.toCard() = RecipeCardItem(
        id = id,
        title = name,
        imageUrl = thumbnailUrl,
        subtitle = null,
        badge = null
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecipesViewModel(
                    createSavedStateHandle(),
                    ServiceLocator.recipeRepository,
                    ServiceLocator.healthProfileRepository,
                    ServiceLocator.compatibilityChecker
                )
            }
        }
    }
}
