package vn.edu.vlu.milestone2.data.model

/** Sản phẩm rút gọn hiển thị trong danh sách tìm kiếm. */
data class ProductSummary(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val nutriScore: String?
)

/** Thông tin đầy đủ của 1 sản phẩm dùng cho màn hình Phân tích. */
data class Product(
    val barcode: String,
    val name: String,
    val brand: String?,
    val quantity: String?,
    val imageUrl: String?,
    val nutriScore: String?,
    val novaGroup: Int?,
    val ingredients: String?,
    val allergens: List<String>,
    val nutrition: Nutrition
)

/** Giá trị dinh dưỡng trên 100 g / 100 ml. */
data class Nutrition(
    val energyKcal: Double?,
    val sugars: Double?,
    val fat: Double?,
    val saturatedFat: Double?,
    val salt: Double?,
    val proteins: Double?,
    val carbohydrates: Double?,
    val fiber: Double?
)
