package vn.edu.vlu.milestone2.data.remote.gemini

import com.google.gson.annotations.SerializedName

data class GenerateContentRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig
)

data class GeminiContent(
    @SerializedName("role") val role: String? = null,
    @SerializedName("parts") val parts: List<GeminiPart>
)

/** Gson bỏ qua field null → mỗi part chỉ chứa text HOẶC inlineData. */
data class GeminiPart(
    @SerializedName("text") val text: String? = null,
    @SerializedName("inlineData") val inlineData: InlineData? = null
)

/** Ảnh JPEG mã hóa Base64 – chỉ tồn tại trong RAM lúc gửi request, không bao giờ lưu xuống Room. */
data class InlineData(
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("data") val data: String
)

data class GenerationConfig(
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json",
    @SerializedName("responseSchema") val responseSchema: Map<String, Any>,
    @SerializedName("temperature") val temperature: Double = 0.2
)

data class GenerateContentResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("promptFeedback") val promptFeedback: PromptFeedback?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?,
    @SerializedName("finishReason") val finishReason: String?
)

data class PromptFeedback(
    @SerializedName("blockReason") val blockReason: String?
)
