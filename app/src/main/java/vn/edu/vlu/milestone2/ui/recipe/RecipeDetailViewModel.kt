package vn.edu.vlu.milestone2.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.model.RecipeCompatibility
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.domain.RecipeCompatibilityChecker
import vn.edu.vlu.milestone2.ui.NavArgs

data class RecipeDetailUi(val recipe: Recipe, val compatibility: RecipeCompatibility?)

sealed interface RecipeDetailEvent {
    data class SavedChanged(val saved: Boolean) : RecipeDetailEvent
    data object Deleted : RecipeDetailEvent
}

class RecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    profileRepository: HealthProfileRepository,
    checker: RecipeCompatibilityChecker
) : ViewModel() {

    val recipeId: String = checkNotNull(savedStateHandle.get<String>(NavArgs.RECIPE_ID))

    private val loadState = MutableStateFlow<UiState<Unit>>(UiState.Loading)

    /** Sau khi tải (cache-first), quan sát Room để nút Lưu/Bỏ lưu cập nhật tức thì. */
    val uiState: StateFlow<UiState<RecipeDetailUi>> = combine(
        loadState,
        recipeRepository.observeRecipe(recipeId),
        profileRepository.observeCurrentProfile()
    ) { load, recipe, profile ->
        when {
            recipe != null -> UiState.Success(RecipeDetailUi(recipe, checker.check(recipe.ingredients, profile)))
            load is UiState.Error -> load
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _events = Channel<RecipeDetailEvent>(Channel.BUFFERED)
    val events: Flow<RecipeDetailEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loadState.value = UiState.Loading
            loadState.value = when (val result = recipeRepository.loadRecipe(recipeId)) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
            }
        }
    }

    fun toggleSaved() {
        val recipe = (uiState.value as? UiState.Success)?.data?.recipe ?: return
        viewModelScope.launch {
            recipeRepository.setSaved(recipe.id, !recipe.isSaved)
            _events.send(RecipeDetailEvent.SavedChanged(!recipe.isSaved))
        }
    }

    fun delete() {
        viewModelScope.launch {
            recipeRepository.deleteRecipe(recipeId)
            _events.send(RecipeDetailEvent.Deleted)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecipeDetailViewModel(
                    createSavedStateHandle(),
                    ServiceLocator.recipeRepository,
                    ServiceLocator.healthProfileRepository,
                    ServiceLocator.compatibilityChecker
                )
            }
        }
    }
}
