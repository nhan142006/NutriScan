package vn.edu.vlu.milestone2.data.remote.gemini

/**
 * responseSchema (OpenAPI subset) gửi kèm generationConfig.
 * Khớp 1-1 với data class [LabelAnalysis] và [RecipeSuggestion] (Proposal mục 4.3).
 */
object GeminiSchemas {

    private val STRING = mapOf("type" to "STRING")
    private val STRING_ARRAY = mapOf("type" to "ARRAY", "items" to STRING)

    val LABEL_ANALYSIS: Map<String, Any> = mapOf(
        "type" to "OBJECT",
        "properties" to mapOf(
            "productName" to STRING,
            "ingredients" to STRING_ARRAY,
            "rating" to mapOf("type" to "STRING", "enum" to listOf("SUITABLE", "LIMIT", "AVOID")),
            "flaggedIngredients" to mapOf(
                "type" to "ARRAY",
                "items" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "name" to STRING,
                        "reason" to STRING,
                        "severity" to mapOf("type" to "STRING", "enum" to listOf("LOW", "MEDIUM", "HIGH"))
                    ),
                    "required" to listOf("name", "reason", "severity")
                )
            ),
            "explanation" to STRING,
            "mainIngredientEn" to STRING
        ),
        "required" to listOf("productName", "ingredients", "rating", "flaggedIngredients", "explanation"),
        "propertyOrdering" to listOf(
            "productName", "ingredients", "rating", "flaggedIngredients", "explanation", "mainIngredientEn"
        )
    )

    val RECIPE_SUGGESTIONS: Map<String, Any> = mapOf(
        "type" to "ARRAY",
        "items" to mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "title" to STRING,
                "ingredients" to STRING_ARRAY,
                "steps" to STRING_ARRAY,
                "fitReason" to STRING,
                "usedScannedItems" to STRING_ARRAY
            ),
            "required" to listOf("title", "ingredients", "steps", "fitReason", "usedScannedItems"),
            "propertyOrdering" to listOf("title", "ingredients", "steps", "fitReason", "usedScannedItems")
        )
    )
}
