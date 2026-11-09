package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.OpenFoodFactsApi
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.ProductDto

/**
 * Single Source of Truth cho dữ liệu sản phẩm:
 * Retrofit API -> ProductRepository -> ViewModel -> StateFlow -> ViewBinding UI.
 */
class ProductRepository(
    private val api: OpenFoodFactsApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun getProduct(barcode: String): NetworkResult<Product> {
        val result = safeApiCall(ioDispatcher) { api.getProduct(barcode.trim()) }
        return when (result) {
            is NetworkResult.Error -> result
            is NetworkResult.Success -> {
                val body = result.data
                val product = body.product
                if (body.status != PRODUCT_FOUND || product == null) {
                    // Open Food Facts có thể trả HTTP 200 kèm status = 0 khi không có mã vạch.
                    NetworkResult.Error(NetworkError.NOT_FOUND, httpCode = 404)
                } else {
                    NetworkResult.Success(product.toDomain(fallbackBarcode = barcode))
                }
            }
        }
    }

    suspend fun searchProducts(query: String): NetworkResult<List<ProductSummary>> {
        val result = safeApiCall(ioDispatcher) { api.searchProducts(query.trim()) }
        return when (result) {
            is NetworkResult.Error -> result
            is NetworkResult.Success -> NetworkResult.Success(
                result.data.products.orEmpty()
                    .mapNotNull { it.toSummaryOrNull() }
                    .distinctBy { it.barcode }
            )
        }
    }

    private companion object {
        const val PRODUCT_FOUND = 1
    }
}

private fun ProductDto.toSummaryOrNull(): ProductSummary? {
    val barcode = code?.takeIf { it.isNotBlank() } ?: return null
    val name = productName?.takeIf { it.isNotBlank() } ?: return null
    return ProductSummary(
        barcode = barcode,
        name = name,
        brand = brands?.firstBrand(),
        imageUrl = imageFrontSmallUrl ?: imageFrontUrl,
        nutriScore = nutriscoreGrade?.normalizedGrade()
    )
}

private fun ProductDto.toDomain(fallbackBarcode: String): Product = Product(
    barcode = code?.takeIf { it.isNotBlank() } ?: fallbackBarcode,
    name = productName?.takeIf { it.isNotBlank() } ?: "Sản phẩm không tên",
    brand = brands?.firstBrand(),
    quantity = quantity?.takeIf { it.isNotBlank() },
    imageUrl = imageFrontUrl ?: imageUrl,
    nutriScore = nutriscoreGrade?.normalizedGrade(),
    novaGroup = novaGroup,
    ingredients = ingredientsText?.takeIf { it.isNotBlank() },
    allergens = allergensTags.orEmpty().map { it.substringAfter(':').replace('-', ' ') },
    nutrition = Nutrition(
        energyKcal = nutriments?.energyKcal100g,
        sugars = nutriments?.sugars100g,
        fat = nutriments?.fat100g,
        saturatedFat = nutriments?.saturatedFat100g,
        salt = nutriments?.salt100g,
        proteins = nutriments?.proteins100g,
        carbohydrates = nutriments?.carbohydrates100g,
        fiber = nutriments?.fiber100g
    )
)

private fun String.firstBrand(): String? = split(',').firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

/** Chỉ chấp nhận Nutri-Score a..e; bỏ các giá trị như "unknown", "not-applicable". */
private fun String.normalizedGrade(): String? =
    lowercase().takeIf { it.length == 1 && it[0] in 'a'..'e' }?.uppercase()
