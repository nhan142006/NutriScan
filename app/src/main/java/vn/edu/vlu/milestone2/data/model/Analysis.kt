package vn.edu.vlu.milestone2.data.model

import com.google.gson.annotations.SerializedName
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus

enum class Severity { LOW, MEDIUM, HIGH }

/** Kết quả phân tích – khớp 1-1 với GeminiSchemas.LABEL_ANALYSIS (Proposal 4.3). */
data class LabelAnalysis(
    @SerializedName("productName") val productName: String,
    @SerializedName("ingredients") val ingredients: List<String>,
    @SerializedName("rating") val rating: Rating,
    @SerializedName("flaggedIngredients") val flaggedIngredients: List<FlaggedIngredient>,
    @SerializedName("explanation") val explanation: String,
    @SerializedName("mainIngredientEn") val mainIngredientEn: String? = null
)

data class FlaggedIngredient(
    @SerializedName("name") val name: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("severity") val severity: Severity
)

/** Model miền của 1 bản ghi ScanResult. */
data class ScanResult(
    val scanId: Long,
    val productBarcode: String?,
    /** File URI của JPEG đã nén (< 1 MB) – không bao giờ là Base64. */
    val imagePath: String?,
    val productName: String,
    val ingredients: List<String>,
    val rating: Rating?,
    val analysis: LabelAnalysis?,
    val note: String?,
    val status: ScanStatus,
    val source: ScanSource,
    val errorMessage: String?,
    val createdAt: Long
)
