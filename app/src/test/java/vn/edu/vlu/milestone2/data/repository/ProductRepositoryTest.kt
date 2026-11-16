package vn.edu.vlu.milestone2.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.data.local.dao.FavoriteDao
import vn.edu.vlu.milestone2.data.local.dao.ProductDao
import vn.edu.vlu.milestone2.data.local.entity.FavoriteProductEntity
import vn.edu.vlu.milestone2.data.local.entity.ProductEntity
import vn.edu.vlu.milestone2.data.local.model.FavoriteWithProduct
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.OpenFoodFactsApi
import java.util.concurrent.TimeUnit

/**
 * Kiểm thử Network Layer với MockWebServer: JSON parsing + xử lý HTTP 400/404/500,
 * timeout và JSON hỏng mà không làm crash ứng dụng.
 */
class ProductRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ProductRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
        // Room được thay bằng DAO giả trong bộ nhớ → kiểm thử thuần JVM.
        repository = ProductRepository(api, FakeProductDao(), FakeFavoriteDao(), Gson())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getProduct_success_parsesJson() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "code": "8934673573017",
                  "status": 1,
                  "status_verbose": "product found",
                  "product": {
                    "code": "8934673573017",
                    "product_name": "Sữa tươi tiệt trùng",
                    "brands": "Vinamilk, Vietnam Dairy",
                    "image_front_url": "https://images.openfoodfacts.org/x.jpg",
                    "nutriscore_grade": "b",
                    "allergens_tags": ["en:milk"],
                    "nutriments": { "energy-kcal_100g": 75, "sugars_100g": 4.5, "salt_100g": 0.1 }
                  }
                }
                """.trimIndent()
            )
        )

        val result = repository.getProduct("8934673573017")

        assertTrue(result is NetworkResult.Success)
        val product = (result as NetworkResult.Success).data.product
        assertEquals("Sữa tươi tiệt trùng", product.name)
        assertEquals("Vinamilk", product.brand)
        assertEquals("B", product.nutriScore)
        assertEquals(listOf("milk"), product.allergens)
        assertEquals(4.5, product.nutrition.sugars!!, 0.001)
        assertEquals("/api/v2/product/8934673573017.json", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun getProduct_status0_returnsNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":"1","status":0,"status_verbose":"product not found"}"""))
        assertError(repository.getProduct("12345678"), NetworkError.NOT_FOUND)
    }

    @Test
    fun getProduct_http404_returnsNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"status":0}"""))
        assertError(repository.getProduct("12345678"), NetworkError.NOT_FOUND, 404)
    }

    @Test
    fun getProduct_http400_returnsBadRequest() = runTest {
        server.enqueue(MockResponse().setResponseCode(400))
        assertError(repository.getProduct("12345678"), NetworkError.BAD_REQUEST, 400)
    }

    @Test
    fun getProduct_http500_returnsServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertError(repository.getProduct("12345678"), NetworkError.SERVER, 500)
    }

    @Test
    fun getProduct_malformedJson_returnsParseError() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status": "abc", "product": [}"""))
        assertError(repository.getProduct("12345678"), NetworkError.PARSE)
    }

    @Test
    fun getProduct_noResponse_returnsTimeout() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertError(repository.getProduct("12345678"), NetworkError.TIMEOUT)
    }

    @Test
    fun searchProducts_skipsItemsWithoutBarcodeOrName() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"count": 3, "products": [
                  {"code": "111", "product_name": "Sữa chua", "brands": "Vinamilk", "nutriscore_grade": "unknown"},
                  {"code": "", "product_name": "Không có mã"},
                  {"code": "222"}
                ]}
                """.trimIndent()
            )
        )

        val result = repository.searchProducts("sữa")

        assertTrue(result is NetworkResult.Success)
        val items = (result as NetworkResult.Success).data
        assertEquals(1, items.size)
        assertEquals("111", items[0].barcode)
        assertEquals(null, items[0].nutriScore)
    }

    private fun assertError(result: NetworkResult<*>, expected: NetworkError, httpCode: Int? = null) {
        assertTrue("Expected Error but was $result", result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(expected, error.error)
        if (httpCode != null) assertEquals(httpCode, error.httpCode)
    }
}

private class FakeProductDao : ProductDao {
    private val items = MutableStateFlow<Map<String, ProductEntity>>(emptyMap())
    override suspend fun getProduct(barcode: String): ProductEntity? = items.value[barcode]
    override fun observeProduct(barcode: String): Flow<ProductEntity?> = items.map { it[barcode] }
    override suspend fun upsertProduct(product: ProductEntity) {
        items.value = items.value + (product.barcode to product)
    }
    override suspend fun pruneCache(olderThan: Long): Int = 0
}

private class FakeFavoriteDao : FavoriteDao {
    private val favorites = MutableStateFlow<List<FavoriteProductEntity>>(emptyList())
    override fun observeFavorites(): Flow<List<FavoriteWithProduct>> = MutableStateFlow(emptyList())
    override fun observeIsFavorite(barcode: String): Flow<Boolean> = favorites.map { list -> list.any { it.productBarcode == barcode } }
    override suspend fun insert(favorite: FavoriteProductEntity): Long {
        favorites.value = favorites.value + favorite
        return favorites.value.size.toLong()
    }
    override suspend fun delete(barcode: String): Int {
        val before = favorites.value.size
        favorites.value = favorites.value.filterNot { it.productBarcode == barcode }
        return before - favorites.value.size
    }
}
