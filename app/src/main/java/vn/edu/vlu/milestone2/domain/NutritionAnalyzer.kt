package vn.edu.vlu.milestone2.domain

import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.repository.HealthProfile
import kotlin.math.roundToInt

enum class Verdict { SAFE, CAUTION, AVOID }

data class AnalysisWarning(val level: Verdict, val message: String)

data class AnalysisResult(
    val verdict: Verdict,
    val warnings: List<AnalysisWarning>
)

/**
 * Đối chiếu dữ liệu dinh dưỡng (lấy từ API) với Hồ sơ sức khỏe (lấy từ Room).
 * Ngưỡng "cao/trung bình" theo hệ thống nhãn đèn giao thông của FSA (Anh) trên 100 g.
 * Lớp thuần Kotlin, không phụ thuộc Android nên kiểm thử được bằng JUnit.
 */
class NutritionAnalyzer {

    fun analyze(product: Product, profile: HealthProfile?): AnalysisResult {
        val warnings = mutableListOf<AnalysisWarning>()
        val nutrition = product.nutrition
        val conditions = profile?.conditions.orEmpty().map { it.lowercase() }

        // 1. Cảnh báo chung theo ngưỡng dinh dưỡng.
        nutrition.sugars?.let { sugar ->
            if (sugar > SUGAR_HIGH) warnings += caution("Hàm lượng đường cao: ${sugar.format()} g/100 g.")
        }
        nutrition.saturatedFat?.let { fat ->
            if (fat > SAT_FAT_HIGH) warnings += caution("Chất béo bão hòa cao: ${fat.format()} g/100 g.")
        }
        nutrition.salt?.let { salt ->
            if (salt > SALT_HIGH) warnings += caution("Hàm lượng muối cao: ${salt.format()} g/100 g.")
        }

        // 2. Cảnh báo cá nhân hóa theo tình trạng sức khỏe.
        if (conditions.any { it.containsAny(DIABETES_KEYWORDS) }) {
            nutrition.sugars?.let { sugar ->
                when {
                    sugar > SUGAR_HIGH -> warnings += avoid("Không phù hợp người tiểu đường: đường ${sugar.format()} g/100 g.")
                    sugar > SUGAR_MEDIUM -> warnings += caution("Người tiểu đường nên hạn chế: đường ${sugar.format()} g/100 g.")
                }
            }
        }
        if (conditions.any { it.containsAny(HYPERTENSION_KEYWORDS) }) {
            nutrition.salt?.let { salt ->
                when {
                    salt > SALT_HIGH -> warnings += avoid("Không phù hợp người tăng huyết áp: muối ${salt.format()} g/100 g.")
                    salt > SALT_MEDIUM -> warnings += caution("Người tăng huyết áp nên hạn chế: muối ${salt.format()} g/100 g.")
                }
            }
        }
        if (conditions.any { it.containsAny(HEART_KEYWORDS) }) {
            nutrition.saturatedFat?.let { fat ->
                when {
                    fat > SAT_FAT_HIGH -> warnings += avoid("Không phù hợp bệnh tim mạch/mỡ máu: béo bão hòa ${fat.format()} g/100 g.")
                    fat > SAT_FAT_MEDIUM -> warnings += caution("Bệnh tim mạch/mỡ máu nên hạn chế: béo bão hòa ${fat.format()} g/100 g.")
                }
            }
        }

        // 3. Dị ứng: so khớp từ khóa trong hồ sơ với allergens_tags của sản phẩm.
        val productAllergens = product.allergens.map { it.lowercase() }
        ALLERGEN_KEYWORDS.forEach { (allergenTag, keywords) ->
            val userHasAllergy = conditions.any { it.containsAny(keywords) }
            val productContains = productAllergens.any { it.contains(allergenTag) }
            if (userHasAllergy && productContains) {
                warnings += avoid("Có chứa chất gây dị ứng: $allergenTag.")
            }
        }

        // 4. Năng lượng so với mục tiêu ngày.
        val energy = nutrition.energyKcal
        if (profile != null && energy != null && profile.dailyCalorieTarget > 0) {
            val percent = (energy / profile.dailyCalorieTarget * 100).roundToInt()
            if (percent >= CALORIE_PERCENT_WARNING) {
                warnings += caution("100 g sản phẩm cung cấp $percent% mục tiêu ${profile.dailyCalorieTarget} kcal/ngày.")
            }
        }

        val verdict = when {
            warnings.any { it.level == Verdict.AVOID } -> Verdict.AVOID
            warnings.isNotEmpty() -> Verdict.CAUTION
            else -> Verdict.SAFE
        }
        return AnalysisResult(verdict = verdict, warnings = warnings)
    }

    private fun caution(message: String) = AnalysisWarning(Verdict.CAUTION, message)
    private fun avoid(message: String) = AnalysisWarning(Verdict.AVOID, message)

    private fun String.containsAny(keywords: List<String>) = keywords.any { contains(it) }

    private fun Double.format(): String =
        if (this % 1.0 == 0.0) toInt().toString() else String.format(java.util.Locale.US, "%.1f", this)

    companion object {
        const val SUGAR_HIGH = 22.5
        const val SUGAR_MEDIUM = 5.0
        const val SAT_FAT_HIGH = 5.0
        const val SAT_FAT_MEDIUM = 1.5
        const val SALT_HIGH = 1.5
        const val SALT_MEDIUM = 0.3
        const val CALORIE_PERCENT_WARNING = 20

        val DIABETES_KEYWORDS = listOf("tiểu đường", "đái tháo đường", "diabetes")
        val HYPERTENSION_KEYWORDS = listOf("huyết áp", "hypertension")
        val HEART_KEYWORDS = listOf("tim mạch", "mỡ máu", "cholesterol", "heart")

        /** allergen tag (Open Food Facts, đã bỏ tiền tố "en:") -> từ khóa người dùng nhập. */
        val ALLERGEN_KEYWORDS: Map<String, List<String>> = mapOf(
            "peanuts" to listOf("đậu phộng", "lạc", "peanut"),
            "milk" to listOf("sữa", "lactose", "milk"),
            "eggs" to listOf("trứng", "egg"),
            "gluten" to listOf("gluten", "lúa mì", "wheat"),
            "crustaceans" to listOf("hải sản", "tôm", "cua", "shellfish"),
            "fish" to listOf("dị ứng cá", "fish"),
            "soybeans" to listOf("đậu nành", "soy"),
            "nuts" to listOf("hạt", "nuts"),
            "sesame" to listOf("mè", "vừng", "sesame")
        )
    }
}
