package vn.edu.vlu.milestone2.ui.recipe

import androidx.annotation.StringRes
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.Recipe
import vn.edu.vlu.milestone2.data.model.RecipeCompatibility
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.domain.RecipeCompatibilityChecker
import vn.edu.vlu.milestone2.ui.NavArgs

data class RecipeFormErrors(
    @StringRes val title: Int? = null,
    @StringRes val ingredients: Int? = null,
    @StringRes val steps: Int? = null
)

/** AddEditRecipeFragment – Create/Update cho "Món của tôi"; kiểm tra tương thích với HealthProfile khi gõ. */
class AddEditRecipeViewModel(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val profileRepository: HealthProfileRepository,
    private val checker: RecipeCompatibilityChecker
) : ViewModel() {

    private val editingId: String? = savedStateHandle.get<String>(NavArgs.RECIPE_ID)?.takeIf { it.isNotBlank() }
    val isEditing: Boolean get() = editingId != null

    private val _existing = MutableStateFlow<Recipe?>(null)
    val existing: StateFlow<Recipe?> = _existing.asStateFlow()

    private val _errors = MutableStateFlow(RecipeFormErrors())
    val errors: StateFlow<RecipeFormErrors> = _errors.asStateFlow()

    private val _compatibility = MutableStateFlow<RecipeCompatibility?>(null)
    val compatibility: StateFlow<RecipeCompatibility?> = _compatibility.asStateFlow()

    private val _saved = Channel<String>(Channel.BUFFERED)
    val saved: Flow<String> = _saved.receiveAsFlow()

    private var profile: HealthProfile? = null

    init {
        viewModelScope.launch {
            profile = profileRepository.getCurrentProfile()
            editingId?.let { id -> _existing.value = recipeRepository.observeRecipe(id).firstOrNull() }
        }
    }

    fun onIngredientsChanged(text: String) {
        _compatibility.value = checker.check(text.toLines(), profile)
    }

    fun save(title: String, category: String, ingredientsText: String, stepsText: String) {
        val ingredients = ingredientsText.toLines()
        val steps = stepsText.toLines()
        val errors = RecipeFormErrors(
            title = if (title.isBlank()) R.string.add_recipe_error_title else null,
            ingredients = if (ingredients.isEmpty()) R.string.add_recipe_error_ingredients else null,
            steps = if (steps.isEmpty()) R.string.add_recipe_error_steps else null
        )
        _errors.value = errors
        if (errors != RecipeFormErrors()) return
        viewModelScope.launch {
            val id = recipeRepository.saveUserRecipe(editingId, title, category, ingredients, steps)
            _saved.send(id)
        }
    }

    private fun String.toLines(): List<String> =
        lines().map { it.trim().removePrefix("•").removePrefix("-").trim() }.filter { it.isNotEmpty() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddEditRecipeViewModel(
                    createSavedStateHandle(),
                    ServiceLocator.recipeRepository,
                    ServiceLocator.healthProfileRepository,
                    ServiceLocator.compatibilityChecker
                )
            }
        }
    }
}
