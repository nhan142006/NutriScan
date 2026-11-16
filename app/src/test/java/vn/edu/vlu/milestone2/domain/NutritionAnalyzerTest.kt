package vn.edu.vlu.milestone2.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product

class NutritionAnalyzerTest {

    private val profile = HealthProfile(
        id = 1, userId = "uid", fullName = "Test", age = 21, dietGoal = "Giảm cân", dailyCalorieTarget = 2000,
        conditions = listOf("Tiểu đường", "Cao huyết áp"), allergies = listOf("Đậu phộng", "Sữa / Lactose"), updatedAt = 0
    )

    private fun product(ingredients: String, allergens: List<String>, nutrition: Nutrition) =
        Product("893", "Test", null, null, null, null, null, ingredients, allergens, nutrition)

    @Test
    fun allergenInProduct_isAvoid() {
        val result = NutritionAnalyzer().analyze(product("Sữa bò tươi, đường", listOf("milk"), Nutrition(sugars = 4.0)), profile)
        assertEquals(Rating.AVOID, result.rating)
        assertEquals("milk", result.mainIngredientEn)
    }

    @Test
    fun plainWater_isSuitable() {
        val result = NutritionAnalyzer().analyze(product("Nước khoáng", emptyList(), Nutrition(sugars = 0.0, salt = 0.01)), profile)
        assertEquals(Rating.SUITABLE, result.rating)
    }

    @Test
    fun saltySnack_withoutProfile_isLimit() {
        val result = NutritionAnalyzer().analyze(product("khoai tây, muối", emptyList(), Nutrition(salt = 2.0)), null)
        assertEquals(Rating.LIMIT, result.rating)
    }

    @Test
    fun recipeChecker_flagsAllergyAndVegetarian() {
        val checker = RecipeCompatibilityChecker()
        assertEquals(Rating.AVOID, checker.check(listOf("Peanut butter", "Bread"), profile)?.rating)
        assertEquals(Rating.LIMIT, checker.check(listOf("Chicken", "Soy sauce"), profile)?.rating)
        assertEquals(Rating.SUITABLE, checker.check(listOf("Tofu", "Broccoli"), profile)?.rating)
        val vegetarian = profile.copy(dietGoal = "Ăn chay", conditions = emptyList(), allergies = emptyList())
        assertEquals(Rating.AVOID, checker.check(listOf("Beef"), vegetarian)?.rating)
        assertNull(checker.check(listOf("Tofu"), null))
    }
}
