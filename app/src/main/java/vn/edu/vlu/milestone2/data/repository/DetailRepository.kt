package vn.edu.vlu.milestone2.data.repository

import retrofit2.HttpException
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.network.safeApiCall
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.remote.firestore.FirestoreString
import vn.edu.vlu.milestone2.data.remote.firestore.SavedProductApi
import vn.edu.vlu.milestone2.data.remote.firestore.SavedProductDocument

internal class SavedProductSession(
    val project: String,
    val uid: String,
    val authorization: String
)

internal class DetailRepository(
    private val products: ProductRepository,
    private val savedProducts: SavedProductApi,
    private val session: suspend () -> SavedProductSession?
) {

    suspend fun getProduct(barcode: String): NetworkResult<Product> = products.getProduct(barcode)

    suspend fun isSaved(barcode: String): NetworkResult<Boolean> = withSession {
        try {
            savedProducts.get(project, uid, barcode, authorization)
            true
        } catch (error: HttpException) {
            if (error.code() != 404) throw error
            error.response()?.errorBody()?.close()
            false
        }
    }

    suspend fun save(barcode: String): NetworkResult<Boolean> = withSession {
        val document = SavedProductDocument(mapOf("barcode" to FirestoreString(barcode)))
        savedProducts.save(project, uid, barcode, authorization, document)
        true
    }

    suspend fun delete(barcode: String): NetworkResult<Boolean> = withSession {
        savedProducts.delete(project, uid, barcode, authorization)
        false
    }

    private suspend fun withSession(
        request: suspend SavedProductSession.() -> Boolean
    ): NetworkResult<Boolean> = when (val result = safeApiCall { session()?.request() }) {
        is NetworkResult.Error -> result
        is NetworkResult.Success -> result.data?.let { NetworkResult.Success(it) }
            ?: NetworkResult.Error(NetworkError.UNAUTHORIZED)
    }
}
