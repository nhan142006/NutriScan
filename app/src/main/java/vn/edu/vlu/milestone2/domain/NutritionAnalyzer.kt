package vn.edu.vlu.milestone2.domain

import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.model.FlaggedIngredient
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.LabelAnalysis
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.Severity
import vn.edu.vlu.milestone2.domain.HealthKeywords.containsAny
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Phân tích OFFLINE theo luật (dùng khi chưa có Gemini API key hoặc mất mạng).
 * Ngưỡng "cao/trung bình" theo hệ thống nhãn đèn giao thông FSA (Anh) trên 100 g.
 * Trả về cùng kiểu [LabelAnalysis] với Gemini để UI hiển thị thống nhất.
 * Lớp thuần Kotlin → kiểm thử được bằng JUnit.
 */
class NutritionAnalyzer {

    fun analyze(product: Product, profile: HealthProfile?): LabelAnalysis {
        val flags = mutableListOf<FlaggedIngredient>()
        val n = product.nutrition
        val conditions = profile?.conditions.orEmpty().joinToString(" ").lowercase()
        val allergies = profile?.allergies.orEmpty().joinToString(" ").lowercase()

        val diabetic = conditions.containsAny(HealthKeywords.DIABETES)
        val hypertensive = conditions.containsAny(HealthKeywords.HYPERTENSION)
        val cholesterol = conditions.containsAny(HealthKeywords.CHOLESTEROL)

        n.sugars?.let { sugar ->
            when {
                sugar > SUGAR_HIGH -> flags += flag("Đường", "Hàm lượng đường cao: ${sugar.fmt()} g/100 g.", if (diabetic) Severity.HIGH else Severity.MEDIUM)
                diabetic && sugar > SUGAR_MEDIUM -> flags += flag("Đường", "Người tiểu đường nên hạn chế: ${sugar.fmt()} g/100 g.", Severity.MEDIUM)
            }
        }
        n.salt?.let { salt ->
            when {
                salt > SALT_HIGH -> flags += flag("Muối", "Hàm lượng muối cao: ${salt.fmt()} g/100 g.", if (hypertensive) Severity.HIGH else Severity.MEDIUM)
                hypertensive && salt > SALT_MEDIUM -> flags += flag("Muối", "Người tăng huyết áp nên hạn chế: ${salt.fmt()} g/100 g.", Severity.MEDIUM)
            }
        }
        n.saturatedFat?.let { fat ->
            when {
                fat > SAT_FAT_HIGH -> flags += flag("Chất béo bão hòa", "Béo bão hòa cao: ${fat.fmt()} g/100 g.", if (cholesterol) Severity.HIGH else Severity.MEDIUM)
                cholesterol && fat > SAT_FAT_MEDIUM -> flags += flag("Chất béo bão hòa", "Người mỡ máu/tim mạch nên hạn chế: ${fat.fmt()} g/100 g.", Severity.MEDIUM)
            }
        }

        val productAllergens = product.allergens.map { it.lowercase() }
        val ingredientsText = product.ingredients.orEmpty().lowercase()
        HealthKeywords.ALLERGENS.forEach { allergen ->
            val userAllergic = allergies.containsAny(allergen.userTerms)
            val inTags = productAllergens.any { it.contains(allergen.offTag) }
            val inText = ingredientsText.containsAny(allergen.ingredientTerms)
            if (userAllergic && (inTags || inText)) {
                flags += flag(allergen.userTerms.first().replaceFirstChar { it.titlecase(Locale.ROOT) }, "Có chứa chất gây dị ứng bạn đã khai báo.", Severity.HIGH)
            }
        }

        val energy = n.energyKcal
        if (profile != null && energy != null && profile.dailyCalorieTarget > 0) {
            val percent = (energy / profile.dailyCalorieTarget * 100).roundToInt()
            if (percent >= CALORIE_PERCENT_WARNING) {
                flags += flag("Năng lượng", "100 g cung cấp $percent% mục tiêu ${profile.dailyCalorieTarget} kcal/ngày.", Severity.LOW)
            }
        }

        val rating = when {
            flags.any { it.severity == Severity.HIGH } -> Rating.AVOID
            flags.any { it.severity == Severity.MEDIUM } -> Rating.LIMIT
            else -> Rating.SUITABLE
        }
        val explanation = buildString {
            append("Phân tích offline theo ngưỡng dinh dưỡng/100 g")
            append(if (profile == null) " (chưa có hồ sơ sức khỏe). " else " và hồ sơ của bạn. ")
            append(
                when (rating) {
                    Rating.SUITABLE -> "Không phát hiện vấn đề đáng kể."
                    Rating.LIMIT -> "Có thể dùng nhưng nên hạn chế khẩu phần."
                    Rating.AVOID -> "Không nên sử dụng với tình trạng sức khỏe hiện tại."
                }
            )
        }
        return LabelAnalysis(
            productName = product.name,
            ingredients = splitIngredients(product.ingredients),
            rating = rating,
            flaggedIngredients = flags.distinctBy { it.name to it.reason },
            explanation = explanation,
            mainIngredientEn = HealthKeywords.guessMainIngredient(product.name, product.ingredients)
        )
    }

    private fun flag(name: String, reason: String, severity: Severity) = FlaggedIngredient(name, reason, severity)

    private fun Double.fmt(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.US, "%.1f", this)

    companion object {
        const val SUGAR_HIGH = 22.5
        const val SUGAR_MEDIUM = 5.0
        const val SAT_FAT_HIGH = 5.0
        const val SAT_FAT_MEDIUM = 1.5
        const val SALT_HIGH = 1.5
        const val SALT_MEDIUM = 0.3
        const val CALORIE_PERCENT_WARNING = 20

        fun splitIngredients(text: String?): List<String> =
            text.orEmpty()
                .split(',', ';')
                .map { it.trim().trimEnd('.') }
                .filter { it.isNotEmpty() }
                .take(60)
    }
}
