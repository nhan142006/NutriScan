package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.data.remote.firestore.SavedProductApi
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.OpenFoodFactsApi
import java.util.concurrent.TimeUnit

class DetailRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DetailRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        repository = DetailRepository(
            ProductRepository(retrofit.create(OpenFoodFactsApi::class.java)),
            retrofit.create(SavedProductApi::class.java)
        ) { SavedProductSession("demo-project", "test-user", "Bearer test-token") }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun delete_targetsOnlyTheSignedInUsersSavedProduct() = runTest {
        server.enqueue(MockResponse().setBody("{}"))

        assertEquals(NetworkResult.Success(false), repository.delete(BARCODE))

        val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("DELETE", request.method)
        assertEquals(DOCUMENT_PATH, request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        assertEquals(0L, request.bodySize)
    }

    @Test
    fun delete_http500_keepsTheServerErrorForRetry() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(NetworkResult.Error(NetworkError.SERVER, 500), repository.delete(BARCODE))
    }

    @Test
    fun savedDocument_http404_meansProductIsNotSaved() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(NetworkResult.Success(false), repository.isSaved(BARCODE))

        val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("GET", request.method)
        assertEquals(DOCUMENT_PATH, request.path)
    }

    private companion object {
        const val BARCODE = "8934673573017"
        const val DOCUMENT_PATH =
            "/v1/projects/demo-project/databases/(default)/documents/users/test-user/savedProducts/$BARCODE"
    }
}
