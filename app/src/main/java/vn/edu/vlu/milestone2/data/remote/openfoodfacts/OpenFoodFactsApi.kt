package vn.edu.vlu.milestone2.data.remote.openfoodfacts

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts - cơ sở dữ liệu thực phẩm mở (HTTPS, không cần API key).
 * Base URL: https://world.openfoodfacts.org/
 */
interface OpenFoodFactsApi {

    /** GET /api/v2/product/{barcode}.json - tra cứu 1 sản phẩm theo mã vạch. */
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = PRODUCT_FIELDS
    ): ProductResponseDto

    /** GET /cgi/search.pl - tìm kiếm sản phẩm theo tên (full-text). */
    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 24,
        @Query("fields") fields: String = SEARCH_FIELDS
    ): ProductSearchResponseDto

    companion object {
        const val PRODUCT_FIELDS =
            "code,product_name,brands,quantity,image_front_url,image_url,nutriscore_grade," +
                "nova_group,ingredients_text,allergens_tags,nutriments"
        const val SEARCH_FIELDS =
            "code,product_name,brands,image_front_small_url,image_front_url,nutriscore_grade"
    }
}
