package vn.edu.vlu.milestone2.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.map
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.local.dao.FavoriteDao
import vn.edu.vlu.milestone2.data.local.dao.ProductDao
import vn.edu.vlu.milestone2.data.local.entity.FavoriteProductEntity
import vn.edu.vlu.milestone2.data.model.FavoriteItem
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.model.ProductLookup
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.OpenFoodFactsApi
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.ProductDto

/**
 * Single Source of Truth cho sản phẩm:
 * Retrofit (Open Food Facts) ⇄ Room cache (products) → ViewModel → StateFlow → ViewBinding UI.
 */
class ProductRepository(
    private val api: OpenFoodFactsApi,
    private val productDao: ProductDao,
    private val favoriteDao: FavoriteDao,
    private val gson: Gson,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /** Cache-first: cache còn mới (< 7 ngày) → trả ngay; nếu không thì gọi API, lỗi mạng → dùng cache cũ. */
    suspend fun getProduct(barcode: String, forceRefresh: Boolean = false): NetworkResult<ProductLookup> {
        val code = barcode.trim()
        val cached = productDao.getProduct(code)
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && now - cached.cachedAt < CACHE_TTL_MS) {
            return NetworkResult.Success(ProductLookup(cached.toDomain(gson), fromCache = true))
        }
        return when (val result = safeApiCall(ioDispatcher) { api.getProduct(code) }) {
            is NetworkResult.Success -> {
                val dto = result.data.product
                if (result.data.status != PRODUCT_FOUND || dto == null) {
                    // Open Food Facts trả HTTP 200 + status = 0 khi không có mã vạch → chuyển sang luồng chụp nhãn.
                    NetworkResult.Error(NetworkError.NOT_FOUND, httpCode = 404)
                } else {
                    val product = dto.toDomain(fallbackBarcode = code)
                    productDao.upsertProduct(product.toEntity(gson, now))
                    NetworkResult.Success(ProductLookup(product, fromCache = false))
                }
            }
            is NetworkResult.Error ->
                if (cached != null && result.error != NetworkError.NOT_FOUND) {
                    NetworkResult.Success(ProductLookup(cached.toDomain(gson), fromCache = true))
                } else {
                    result
                }
        }
    }

    suspend fun searchProducts(query: String): NetworkResult<List<ProductSummary>> =
        safeApiCall(ioDispatcher) { api.searchProducts(query.trim()) }.map { body ->
            body.products.orEmpty().mapNotNull { it.toSummaryOrNull() }.distinctBy { it.barcode }
        }

    fun observeFavorites(): Flow<List<FavoriteItem>> =
        favoriteDao.observeFavorites().map { rows -> rows.map { FavoriteItem(it.product.toSummary(), it.addedAt) } }

    fun observeIsFavorite(barcode: String): Flow<Boolean> = favoriteDao.observeIsFavorite(barcode)

    suspend fun addFavorite(product: Product) {
        if (productDao.getProduct(product.barcode) == null) {
            productDao.upsertProduct(product.toEntity(gson, System.currentTimeMillis()))
        }
        favoriteDao.insert(FavoriteProductEntity(productBarcode = product.barcode, addedAt = System.currentTimeMillis()))
    }

    suspend fun removeFavorite(barcode: String) {
        favoriteDao.delete(barcode)
    }

    private companion object {
        const val PRODUCT_FOUND = 1
        const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
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

/** Chỉ chấp nhận Nutri-Score a..e; bỏ "unknown", "not-applicable". */
private fun String.normalizedGrade(): String? =
    lowercase().takeIf { it.length == 1 && it[0] in 'a'..'e' }?.uppercase()
