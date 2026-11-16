package vn.edu.vlu.milestone2.data.local.entity

/** Kết luận AI cho 1 sản phẩm (khớp enum trong responseSchema của Gemini). */
enum class Rating { SUITABLE, LIMIT, AVOID }

/** Trạng thái của 1 bản ghi quét trong hàng đợi offline. */
enum class ScanStatus { PENDING, DONE, FAILED }

/** Nguồn tạo ra ScanResult. */
enum class ScanSource { BARCODE, LABEL_PHOTO }

/** Nguồn của công thức: TheMealDB, Gemini gợi ý, hoặc do người dùng tự tạo. */
enum class RecipeSource { API, AI, USER }

/** Phân loại dòng con của hồ sơ sức khỏe. */
enum class ProfileItemType { CONDITION, ALLERGY }
