package vn.edu.vlu.milestone2.data.remote.gemini

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Google Gemini REST API.
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 *
 * Mọi request đều đặt generationConfig.responseMimeType = "application/json" + responseSchema
 * (xem [GeminiSchemas]) để Gemini chỉ trả về 1 JSON object đúng schema – không có Markdown.
 */
interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}
