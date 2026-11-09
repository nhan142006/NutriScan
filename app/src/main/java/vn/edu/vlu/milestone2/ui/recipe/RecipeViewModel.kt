package vn.edu.vlu.milestone2.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.data.model.RecipeDetail
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

/**
 * RecipeRepository (Retrofit/TheMealDB) -> RecipeViewModel -> StateFlow<UiState> -> RecipeFragment.
 */
class RecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val defaultCategory: String = DEFAULT_CATEGORY
) : ViewModel() {

    private val _categories = MutableStateFlow<List<RecipeCategory>>(emptyList())
    val categories: StateFlow<List<RecipeCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _recipesState = MutableStateFlow<UiState<List<RecipeSummary>>>(UiState.Idle)
    val recipesState: StateFlow<UiState<List<RecipeSummary>>> = _recipesState.asStateFlow()

    private val _detailState = MutableStateFlow<UiState<RecipeDetail>>(UiState.Idle)
    val detailState: StateFlow<UiState<RecipeDetail>> = _detailState.asStateFlow()

    private sealed interface RecipeRequest {
        data object Categories : RecipeRequest
        data class ByCategory(val category: String) : RecipeRequest
        data class Search(val query: String) : RecipeRequest
    }

    private var lastRequest: RecipeRequest = RecipeRequest.Categories
    private var recipesJob: Job? = null
    private var detailJob: Job? = null

    init {
        loadCategories()
    }

    private fun loadCategories() {
        lastRequest = RecipeRequest.Categories
        recipesJob?.cancel()
        recipesJob = viewModelScope.launch {
            _recipesState.value = UiState.Loading
            when (val result = recipeRepository.getCategories()) {
                is NetworkResult.Error -> _recipesState.value = UiState.Error(result.error, result.httpCode)
                is NetworkResult.Success -> {
                    _categories.value = result.data
                    val initial = result.data.firstOrNull { it.name.equals(defaultCategory, ignoreCase = true) }
                        ?: result.data.firstOrNull()
                    if (initial == null) {
                        _recipesState.value = UiState.Success(emptyList())
                    } else {
                        fetchByCategory(initial.name)
                    }
                }
            }
        }
    }

    fun selectCategory(category: String) {
        val current = lastRequest
        if (current is RecipeRequest.ByCategory && current.category == category &&
            _recipesState.value !is UiState.Error
        ) return
        recipesJob?.cancel()
        recipesJob = viewModelScope.launch { fetchByCategory(category) }
    }

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        lastRequest = RecipeRequest.Search(normalized)
        _selectedCategory.value = null
        recipesJob?.cancel()
        recipesJob = viewModelScope.launch {
            _recipesState.value = UiState.Loading
            _recipesState.value = recipeRepository.searchRecipes(normalized).toUiState()
        }
    }

    fun retry() {
        when (val request = lastRequest) {
            RecipeRequest.Categories -> loadCategories()
            is RecipeRequest.ByCategory -> {
                recipesJob?.cancel()
                recipesJob = viewModelScope.launch { fetchByCategory(request.category) }
            }
            is RecipeRequest.Search -> search(request.query)
        }
    }

    fun openRecipe(recipeId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailState.value = UiState.Loading
            _detailState.value = recipeRepository.getRecipeDetail(recipeId).toUiState()
        }
    }

    /** Gọi sau khi Fragment đã hiển thị dialog / thông báo lỗi để không hiện lại khi xoay màn hình. */
    fun onDetailConsumed() {
        _detailState.value = UiState.Idle
    }

    private suspend fun fetchByCategory(category: String) {
        lastRequest = RecipeRequest.ByCategory(category)
        _selectedCategory.value = category
        _recipesState.value = UiState.Loading
        _recipesState.value = recipeRepository.getRecipesByCategory(category).toUiState()
    }

    private fun <T> NetworkResult<T>.toUiState(): UiState<T> = when (this) {
        is NetworkResult.Success -> UiState.Success(data)
        is NetworkResult.Error -> UiState.Error(error, httpCode)
    }

    companion object {
        const val DEFAULT_CATEGORY = "Vegetarian"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RecipeViewModel(ServiceLocator.recipeRepository) }
        }
    }
}
