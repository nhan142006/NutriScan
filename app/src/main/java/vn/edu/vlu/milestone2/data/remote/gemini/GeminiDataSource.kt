package vn.edu.vlu.milestone2.data.remote.gemini

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.model.FlaggedIngredient
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.LabelAnalysis
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.RecipeQuery
import vn.edu.vlu.milestone2.data.model.RecipeSuggestion
import vn.edu.vlu.milestone2.data.model.Severity

/**
 * Lớp duy nhất gọi Gemini. Đảm bảo ràng buộc #1 của giảng viên:
 *  - mọi generateContent đều có responseMimeType = "application/json" + responseSchema;
 *  - JSON được parse trong try/catch → JsonSyntaxException không bao giờ làm crash app;
 *  - prompt chỉ mô tả nhiệm vụ + hồ sơ, KHÔNG yêu cầu "trả lời bằng JSON".
 */
class GeminiDataSource(
    private val api: GeminiApi,
    private val gson: Gson,
    private val apiKey: String,
    private val model: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** Phân tích ẢNH nhãn thành phần. [jpegBytes] chỉ được Base64 trong RAM cho request này. */
    suspend fun analyzeLabelImage(jpegBytes: ByteArray, profile: HealthProfile?): NetworkResult<LabelAnalysis> {
        val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val parts = listOf(
            GeminiPart(text = labelPrompt(profile)),
            GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = base64))
        )
        return generate(parts, GeminiSchemas.LABEL_ANALYSIS, ::parseLabelAnalysis)
    }

    /** Phân tích sản phẩm tra được từ Open Food Facts (dạng văn bản). */
    suspend fun analyzeProduct(product: Product, profile: HealthProfile?): NetworkResult<LabelAnalysis> {
        val n = product.nutrition
        val text = buildString {
            appendLine("Hãy đánh giá mức độ phù hợp của sản phẩm thực phẩm sau với hồ sơ sức khỏe của người dùng.")
            appendLine("Tên sản phẩm: ${product.name}${product.brand?.let { " ($it)" }.orEmpty()}")
            appendLine("Thành phần: ${product.ingredients ?: "không rõ"}")
            appendLine("Chất gây dị ứng: ${product.allergens.joinToString().ifBlank { "không rõ" }}")
            appendLine(
                "Dinh dưỡng /100g: năng lượng ${n.energyKcal} kcal, đường ${n.sugars} g, chất béo ${n.fat} g, " +
                    "béo bão hòa ${n.saturatedFat} g, muối ${n.salt} g, đạm ${n.proteins} g, chất xơ ${n.fiber} g."
            )
            appendLine()
            appendLine("Hồ sơ sức khỏe:")
            appendLine(profile?.toPromptText() ?: "Chưa có hồ sơ – đánh giá theo khuyến nghị chung.")
            appendLine()
            append(COMMON_RULES)
        }
        return generate(listOf(GeminiPart(text = text)), GeminiSchemas.LABEL_ANALYSIS, ::parseLabelAnalysis)
    }

    /**
     * Gợi ý món ăn: HealthProfile + các ScanResult gần đây được truyền làm THAM SỐ TRUY VẤN
     * (ràng buộc #3 – Feature Cohesion).
     */
    suspend fun suggestRecipes(query: RecipeQuery, count: Int = 4): NetworkResult<List<RecipeSuggestion>> {
        val text = buildString {
            appendLine("Gợi ý $count món ăn gia đình lành mạnh, dễ nấu tại Việt Nam cho người dùng sau.")
            appendLine()
            appendLine("Hồ sơ sức khỏe:")
            appendLine(query.profile?.toPromptText() ?: "Chưa có hồ sơ.")
            appendLine()
            query.focusScan?.let { scan ->
                appendLine("Món ăn PHẢI sử dụng sản phẩm vừa quét: ${scan.productName} (thành phần: ${scan.ingredients.take(12).joinToString()}).")
            }
            if (query.preferredScans.isNotEmpty()) {
                appendLine("Ưu tiên dùng các sản phẩm người dùng đã quét gần đây và được đánh giá phù hợp/hạn chế:")
                query.preferredScans.forEach { scan ->
                    appendLine("- ${scan.productName} [${scan.rating?.name ?: "?"}]")
                }
            }
            if (query.excludedIngredients.isNotEmpty()) {
                appendLine("TUYỆT ĐỐI KHÔNG dùng các nguyên liệu sau (dị ứng hoặc bị đánh giá AVOID): ${query.excludedIngredients.joinToString()}.")
            }
            appendLine()
            appendLine("Viết tiêu đề, nguyên liệu (kèm định lượng), các bước và lý do phù hợp bằng tiếng Việt.")
            appendLine("Trường usedScannedItems liệt kê tên các sản phẩm đã quét được dùng trong món (có thể rỗng).")
        }
        return generate(listOf(GeminiPart(text = text)), GeminiSchemas.RECIPE_SUGGESTIONS, ::parseRecipeSuggestions)
    }

    private suspend fun <T> generate(
        parts: List<GeminiPart>,
        schema: Map<String, Any>,
        parse: (String) -> T?
    ): NetworkResult<T> {
        if (!isConfigured) return NetworkResult.Error(NetworkError.AI_NOT_CONFIGURED)
        val request = GenerateContentRequest(
            contents = listOf(GeminiContent(role = "user", parts = parts)),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))),
            generationConfig = GenerationConfig(responseSchema = schema)
        )
        val response = safeApiCall(ioDispatcher) { api.generateContent(model, apiKey, request) }
        if (response is NetworkResult.Error) return response
        val body = (response as NetworkResult.Success).data
        val jsonText = body.candidates?.firstOrNull()?.content?.parts
            ?.firstOrNull { !it.text.isNullOrBlank() }?.text
            ?: return NetworkResult.Error(NetworkError.AI_INVALID_RESPONSE)
        // Defensive parsing: response bị chặn / bị cắt ngắn → lỗi có kiểm soát, không crash.
        val parsed = try {
            parse(jsonText)
        } catch (error: JsonParseException) { // bao gồm JsonSyntaxException
            null
        } catch (error: IllegalStateException) {
            null
        }
        return parsed?.let { NetworkResult.Success(it) } ?: NetworkResult.Error(NetworkError.AI_INVALID_RESPONSE)
    }

    fun parseLabelAnalysis(json: String): LabelAnalysis? =
        gson.fromJson(json, LabelAnalysisDto::class.java)?.toDomainOrNull()

    private fun parseRecipeSuggestions(json: String): List<RecipeSuggestion>? =
        gson.fromJson(json, Array<RecipeSuggestionDto>::class.java)
            ?.mapNotNull { it?.toDomainOrNull() }
            ?.takeIf { it.isNotEmpty() }

    private fun labelPrompt(profile: HealthProfile?): String = buildString {
        appendLine("Ảnh đính kèm là nhãn thành phần của một sản phẩm thực phẩm đóng gói (có thể là sản phẩm Việt Nam).")
        appendLine("Hãy đọc tên sản phẩm và toàn bộ danh sách thành phần, rồi đánh giá mức độ phù hợp với hồ sơ sức khỏe sau:")
        appendLine(profile?.toPromptText() ?: "Chưa có hồ sơ – đánh giá theo khuyến nghị chung.")
        appendLine()
        append(COMMON_RULES)
    }

    private companion object {
        const val SYSTEM_INSTRUCTION =
            "Bạn là chuyên gia dinh dưỡng của ứng dụng NutriScan. Đánh giá thận trọng, dựa trên bằng chứng, " +
                "không chẩn đoán bệnh. Kết quả chỉ mang tính tham khảo."

        const val COMMON_RULES =
            "Quy tắc: rating = AVOID nếu có chất gây dị ứng của người dùng hoặc nguy cơ cao với bệnh lý; " +
                "LIMIT nếu nên hạn chế (đường, muối, chất béo bão hòa, phụ gia E-number đáng lưu ý); " +
                "SUITABLE nếu phù hợp. flaggedIngredients chỉ liệt kê thành phần đáng lưu ý kèm lý do và mức độ. " +
                "explanation và reason viết bằng tiếng Việt, ngắn gọn. " +
                "mainIngredientEn là 1 từ tiếng Anh chỉ nguyên liệu nấu ăn chính của sản phẩm (ví dụ: milk, chicken, rice, tofu)."
    }
}

/* DTO nullable: Gson không tôn trọng non-null của Kotlin, nên kiểm tra thủ công rồi mới đổi sang model miền. */

private data class LabelAnalysisDto(
    @SerializedName("productName") val productName: String?,
    @SerializedName("ingredients") val ingredients: List<String?>?,
    @SerializedName("rating") val rating: Rating?,
    @SerializedName("flaggedIngredients") val flaggedIngredients: List<FlaggedIngredientDto?>?,
    @SerializedName("explanation") val explanation: String?,
    @SerializedName("mainIngredientEn") val mainIngredientEn: String?
) {
    fun toDomainOrNull(): LabelAnalysis? {
        val rating = rating ?: return null
        return LabelAnalysis(
            productName = productName?.takeIf { it.isNotBlank() } ?: "Sản phẩm không rõ tên",
            ingredients = ingredients.orEmpty().filterNotNull().map { it.trim() }.filter { it.isNotEmpty() },
            rating = rating,
            flaggedIngredients = flaggedIngredients.orEmpty().mapNotNull { it?.toDomainOrNull() },
            explanation = explanation.orEmpty(),
            mainIngredientEn = mainIngredientEn?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        )
    }
}

private data class FlaggedIngredientDto(
    @SerializedName("name") val name: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("severity") val severity: Severity?
) {
    fun toDomainOrNull(): FlaggedIngredient? {
        val name = name?.takeIf { it.isNotBlank() } ?: return null
        return FlaggedIngredient(name, reason.orEmpty(), severity ?: Severity.MEDIUM)
    }
}

private data class RecipeSuggestionDto(
    @SerializedName("title") val title: String?,
    @SerializedName("ingredients") val ingredients: List<String?>?,
    @SerializedName("steps") val steps: List<String?>?,
    @SerializedName("fitReason") val fitReason: String?,
    @SerializedName("usedScannedItems") val usedScannedItems: List<String?>?
) {
    fun toDomainOrNull(): RecipeSuggestion? {
        val title = title?.takeIf { it.isNotBlank() } ?: return null
        val steps = steps.orEmpty().filterNotNull().filter { it.isNotBlank() }
        if (steps.isEmpty()) return null
        return RecipeSuggestion(
            title = title,
            ingredients = ingredients.orEmpty().filterNotNull().filter { it.isNotBlank() },
            steps = steps,
            fitReason = fitReason.orEmpty(),
            usedScannedItems = usedScannedItems.orEmpty().filterNotNull()
        )
    }
}
