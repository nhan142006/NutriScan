package vn.edu.vlu.milestone2.ui.recipe

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecipeViewModel : ViewModel() {

    private val _recipeState = MutableStateFlow("Đang tải danh sách món ăn lành mạnh...")
    val recipeState: StateFlow<String> = _recipeState.asStateFlow()

    fun loadRecommendedRecipes() {
        // Sau này nhóm sẽ gắn logic lấy dữ liệu món ăn tại đây
        _recipeState.value = "Gợi ý hôm nay: Salad ức gà, Canh rong biển tofu, Cá hồi nướng."
    }
}