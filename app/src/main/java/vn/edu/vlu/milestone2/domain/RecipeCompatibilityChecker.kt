package vn.edu.vlu.milestone2.domain

import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.RecipeCompatibility
import vn.edu.vlu.milestone2.domain.HealthKeywords.containsAny

/** Tính huy hiệu tương thích (Phù hợp / Hạn chế / Tránh) của 1 công thức so với HealthProfile. */
class RecipeCompatibilityChecker {

    fun check(ingredients: List<String>, profile: HealthProfile?): RecipeCompatibility? {
        if (profile == null || ingredients.isEmpty()) return null
        val text = ingredients.joinToString(" | ").lowercase()
        val reasons = mutableListOf<String>()
        var avoid = false

        val allergies = profile.allergies.joinToString(" ").lowercase()
        HealthKeywords.ALLERGENS.forEach { allergen ->
            if (allergies.containsAny(allergen.userTerms) && text.containsAny(allergen.ingredientTerms)) {
                reasons += "Chứa nguyên liệu gây dị ứng (${allergen.userTerms.first()})"
                avoid = true
            }
        }

        val conditions = profile.conditions.joinToString(" ").lowercase()
        if (conditions.containsAny(HealthKeywords.DIABETES) && text.containsAny(HealthKeywords.HIGH_SUGAR_INGREDIENTS)) {
            reasons += "Có đường/chất ngọt – người tiểu đường nên hạn chế"
        }
        if (conditions.containsAny(HealthKeywords.HYPERTENSION) && text.containsAny(HealthKeywords.HIGH_SALT_INGREDIENTS)) {
            reasons += "Nhiều muối/nước chấm – người tăng huyết áp nên giảm lượng"
        }
        if (conditions.containsAny(HealthKeywords.CHOLESTEROL) && text.containsAny(HealthKeywords.HIGH_FAT_INGREDIENTS)) {
            reasons += "Có chất béo động vật – người mỡ máu nên hạn chế"
        }
        val goal = profile.dietGoal.lowercase()
        if ((goal.contains("chay") || goal.contains("vegetarian")) && text.containsAny(HealthKeywords.MEAT_INGREDIENTS)) {
            reasons += "Có thịt – không phù hợp chế độ ăn chay"
            avoid = true
        }

        val rating = when {
            avoid -> Rating.AVOID
            reasons.isNotEmpty() -> Rating.LIMIT
            else -> Rating.SUITABLE
        }
        return RecipeCompatibility(rating, reasons)
    }
}
