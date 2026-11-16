package vn.edu.vlu.milestone2.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.ui.NavArgs

data class CookingUiState(
    val recipe: Recipe? = null,
    val stepIndex: Int = 0,
    val checked: Set<Int> = emptySet()
) {
    val totalSteps: Int get() = recipe?.steps?.size ?: 0
    val isLastStep: Boolean get() = stepIndex >= totalSteps - 1
}

/** CookingViewModel: đọc công thức từ Room (offline), giữ bước hiện tại qua SavedStateHandle. */
class CookingViewModel(
    private val savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository
) : ViewModel() {

    private val recipeId: String = checkNotNull(savedStateHandle.get<String>(NavArgs.RECIPE_ID))

    val uiState: StateFlow<CookingUiState> = combine(
        recipeRepository.observeRecipe(recipeId),
        savedStateHandle.getStateFlow(KEY_STEP, 0),
        savedStateHandle.getStateFlow(KEY_CHECKED, IntArray(0))
    ) { recipe, step, checked ->
        CookingUiState(recipe, step.coerceIn(0, ((recipe?.steps?.size ?: 1) - 1).coerceAtLeast(0)), checked.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CookingUiState())

    fun next() {
        val state = uiState.value
        if (!state.isLastStep) savedStateHandle[KEY_STEP] = state.stepIndex + 1
    }

    fun previous() {
        val state = uiState.value
        if (state.stepIndex > 0) savedStateHandle[KEY_STEP] = state.stepIndex - 1
    }

    fun toggleIngredient(index: Int, checked: Boolean) {
        val current = uiState.value.checked.toMutableSet()
        if (checked) current += index else current -= index
        savedStateHandle[KEY_CHECKED] = current.toIntArray()
    }

    companion object {
        private const val KEY_STEP = "step"
        private const val KEY_CHECKED = "checked"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CookingViewModel(createSavedStateHandle(), ServiceLocator.recipeRepository) }
        }
    }
}
