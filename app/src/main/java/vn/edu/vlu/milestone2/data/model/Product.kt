package vn.edu.vlu.milestone2.data.model

/** Sản phẩm rút gọn hiển thị trong danh sách tìm kiếm / yêu thích. */
data class ProductSummary(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val nutriScore: String?
)

/** Thông tin đầy đủ của 1 sản phẩm. */
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
    val energyKcal: Double? = null,
    val sugars: Double? = null,
    val fat: Double? = null,
    val saturatedFat: Double? = null,
    val salt: Double? = null,
    val proteins: Double? = null,
    val carbohydrates: Double? = null,
    val fiber: Double? = null
)

/** Kết quả tra cứu: [fromCache] = true khi dữ liệu lấy từ Room do mất mạng / cache còn mới. */
data class ProductLookup(val product: Product, val fromCache: Boolean)

data class FavoriteItem(val product: ProductSummary, val addedAt: Long)
