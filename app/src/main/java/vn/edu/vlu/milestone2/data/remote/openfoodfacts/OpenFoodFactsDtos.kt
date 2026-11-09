package vn.edu.vlu.milestone2.data.remote.openfoodfacts

import com.google.gson.annotations.SerializedName

/*
 * DTO ánh xạ JSON của Open Food Facts. Mọi trường đều nullable vì dữ liệu cộng đồng
 * thường thiếu trường; Gson không dùng giá trị mặc định của Kotlin.
 */

data class ProductResponseDto(
    @SerializedName("code") val code: String?,
    @SerializedName("status") val status: Int?,
    @SerializedName("status_verbose") val statusVerbose: String?,
    @SerializedName("product") val product: ProductDto?
)

data class ProductSearchResponseDto(
    @SerializedName("count") val count: Int?,
    @SerializedName("products") val products: List<ProductDto>?
)

data class ProductDto(
    @SerializedName("code") val code: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("image_front_url") val imageFrontUrl: String?,
    @SerializedName("image_front_small_url") val imageFrontSmallUrl: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("nutriscore_grade") val nutriscoreGrade: String?,
    @SerializedName("nova_group") val novaGroup: Int?,
    @SerializedName("ingredients_text") val ingredientsText: String?,
    @SerializedName("allergens_tags") val allergensTags: List<String>?,
    @SerializedName("nutriments") val nutriments: NutrimentsDto?
)

data class NutrimentsDto(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("sugars_100g") val sugars100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?,
    @SerializedName("saturated-fat_100g") val saturatedFat100g: Double?,
    @SerializedName("salt_100g") val salt100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("fiber_100g") val fiber100g: Double?
)
