package vn.edu.vlu.milestone2.data

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiApi
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiCandidate
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiContent
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiDataSource
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiPart
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiSchemas
import vn.edu.vlu.milestone2.data.remote.gemini.GenerateContentRequest
import vn.edu.vlu.milestone2.data.remote.gemini.GenerateContentResponse

/** Kiểm chứng ràng buộc #1: request luôn có responseSchema; Markdown/JSON hỏng không làm crash. */
class GeminiDataSourceTest {

    private val product = Product("1", "Bánh quy", null, null, null, null, null, "bột mì, đường, bơ", emptyList(), Nutrition())

    private class FakeApi(private val text: String) : GeminiApi {
        var lastRequest: GenerateContentRequest? = null
        override suspend fun generateContent(model: String, apiKey: String, request: GenerateContentRequest): GenerateContentResponse {
            lastRequest = request
            return GenerateContentResponse(
                candidates = listOf(GeminiCandidate(GeminiContent(role = "model", parts = listOf(GeminiPart(text = text))), "STOP")),
                promptFeedback = null
            )
        }
    }

    @Test
    fun validJson_isParsed_andRequestUsesStructuredOutput() = runTest {
        val api = FakeApi(
            """{"productName":"Bánh quy","ingredients":["bột mì","đường"],"rating":"LIMIT",
               "flaggedIngredients":[{"name":"đường","reason":"nhiều đường","severity":"MEDIUM"}],
               "explanation":"Nên hạn chế","mainIngredientEn":"flour"}"""
        )
        val source = GeminiDataSource(api, Gson(), apiKey = "test", model = "gemini-test")
        val result = source.analyzeProduct(product, null)

        assertTrue(result is NetworkResult.Success)
        assertEquals(Rating.LIMIT, (result as NetworkResult.Success).data.rating)
        val config = api.lastRequest!!.generationConfig
        assertEquals("application/json", config.responseMimeType)
        assertEquals(GeminiSchemas.LABEL_ANALYSIS, config.responseSchema)
    }

    @Test
    fun markdownFencedText_returnsError_insteadOfCrashing() = runTest {
        val source = GeminiDataSource(FakeApi("```json\n{ \"rating\": \"AVOID\" \n```"), Gson(), "test", "gemini-test")
        val result = source.analyzeProduct(product, null)
        assertEquals(NetworkError.AI_INVALID_RESPONSE, (result as NetworkResult.Error).error)
    }

    @Test
    fun missingApiKey_returnsNotConfigured() = runTest {
        val source = GeminiDataSource(FakeApi("{}"), Gson(), apiKey = "", model = "gemini-test")
        assertEquals(NetworkError.AI_NOT_CONFIGURED, (source.analyzeProduct(product, null) as NetworkResult.Error).error)
    }
}
