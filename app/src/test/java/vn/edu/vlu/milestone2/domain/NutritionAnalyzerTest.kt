package vn.edu.vlu.milestone2.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.repository.HealthProfile

class NutritionAnalyzerTest {

    private val analyzer = NutritionAnalyzer()

    @Test
    fun lowSugarProduct_noProfile_isSafe() {
        val result = analyzer.analyze(product(sugars = 2.0, salt = 0.1), profile = null)
        assertEquals(Verdict.SAFE, result.verdict)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun highSugarProduct_diabeticProfile_isAvoid() {
        val result = analyzer.analyze(product(sugars = 30.0), profile(listOf("Tiểu đường")))
        assertEquals(Verdict.AVOID, result.verdict)
    }

    @Test
    fun mediumSalt_hypertensionProfile_isCaution() {
        val result = analyzer.analyze(product(salt = 0.8), profile(listOf("Tăng huyết áp")))
        assertEquals(Verdict.CAUTION, result.verdict)
    }

    @Test
    fun peanutAllergen_peanutAllergyProfile_isAvoid() {
        val result = analyzer.analyze(
            product(allergens = listOf("peanuts")),
            profile(listOf("Dị ứng đậu phộng"))
        )
        assertEquals(Verdict.AVOID, result.verdict)
    }

    @Test
    fun energyAbove20PercentOfTarget_isCaution() {
        val result = analyzer.analyze(product(energy = 550.0), profile(emptyList(), calories = 2000))
        assertEquals(Verdict.CAUTION, result.verdict)
    }

    private fun product(
        sugars: Double? = null,
        salt: Double? = null,
        energy: Double? = null,
        allergens: List<String> = emptyList()
    ) = Product(
        barcode = "12345678",
        name = "Test",
        brand = null,
        quantity = null,
        imageUrl = null,
        nutriScore = null,
        novaGroup = null,
        ingredients = null,
        allergens = allergens,
        nutrition = Nutrition(
            energyKcal = energy,
            sugars = sugars,
            fat = null,
            saturatedFat = null,
            salt = salt,
            proteins = null,
            carbohydrates = null,
            fiber = null
        )
    )

    private fun profile(conditions: List<String>, calories: Int = 2000) = HealthProfile(
        id = 1,
        fullName = "Test",
        age = 30,
        dailyCalorieTarget = calories,
        conditions = conditions,
        updatedAt = 0
    )
}
