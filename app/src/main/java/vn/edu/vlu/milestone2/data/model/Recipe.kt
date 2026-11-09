package vn.edu.vlu.milestone2.data.model

data class RecipeCategory(
    val name: String,
    val thumbnailUrl: String?
)

data class RecipeSummary(
    val id: String,
    val name: String,
    val thumbnailUrl: String?
)

data class RecipeDetail(
    val id: String,
    val name: String,
    val category: String?,
    val area: String?,
    val thumbnailUrl: String?,
    val ingredients: List<String>,
    val instructions: String,
    val youtubeUrl: String?
)
