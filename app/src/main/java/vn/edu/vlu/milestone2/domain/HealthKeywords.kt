package vn.edu.vlu.milestone2.domain

/** Từ khóa dùng chung cho phân tích offline và huy hiệu tương thích công thức. */
object HealthKeywords {

    val DIABETES = listOf("tiểu đường", "đái tháo đường", "diabetes")
    val HYPERTENSION = listOf("huyết áp", "hypertension")
    val CHOLESTEROL = listOf("mỡ máu", "cholesterol", "tim mạch", "heart")
    val KIDNEY = listOf("thận", "kidney")

    /** Dị ứng người dùng chọn → (allergen tag Open Food Facts, từ khóa nguyên liệu tiếng Anh/Việt). */
    val ALLERGENS: List<Allergen> = listOf(
        Allergen(listOf("đậu phộng", "lạc", "peanut"), "peanuts", listOf("peanut", "đậu phộng", "lạc")),
        Allergen(listOf("sữa", "lactose", "milk"), "milk", listOf("milk", "cheese", "butter", "cream", "yogurt", "yoghurt", "parmesan", "mozzarella", "sữa", "bơ", "phô mai")),
        Allergen(listOf("trứng", "egg"), "eggs", listOf("egg", "trứng")),
        Allergen(listOf("gluten", "lúa mì", "wheat"), "gluten", listOf("flour", "wheat", "bread", "pasta", "spaghetti", "noodle", "breadcrumb", "bột mì", "mì")),
        Allergen(listOf("hải sản", "tôm", "cua", "shellfish"), "crustaceans", listOf("shrimp", "prawn", "crab", "lobster", "mussel", "clam", "squid", "oyster", "tôm", "cua", "mực", "nghêu", "hàu")),
        Allergen(listOf("dị ứng cá", "fish"), "fish", listOf("fish", "salmon", "tuna", "cod", "anchov", "cá")),
        Allergen(listOf("đậu nành", "soy"), "soybeans", listOf("soy", "tofu", "đậu nành", "đậu hũ", "đậu phụ")),
        Allergen(listOf("hạt", "nuts"), "nuts", listOf("almond", "walnut", "cashew", "hazelnut", "pecan", "hạt điều", "óc chó", "hạnh nhân")),
        Allergen(listOf("mè", "vừng", "sesame"), "sesame", listOf("sesame", "tahini", "mè", "vừng"))
    )

    val HIGH_SUGAR_INGREDIENTS = listOf("sugar", "honey", "syrup", "caramel", "chocolate", "đường", "mật ong", "sô cô la")
    val HIGH_SALT_INGREDIENTS = listOf("salt", "soy sauce", "fish sauce", "bacon", "ham", "sausage", "stock cube", "nước mắm", "xì dầu", "muối", "xúc xích")
    val HIGH_FAT_INGREDIENTS = listOf("butter", "cream", "lard", "bacon", "ghee", "bơ", "mỡ", "kem")
    val MEAT_INGREDIENTS = listOf("chicken", "beef", "pork", "lamb", "bacon", "ham", "sausage", "mince", "gà", "bò", "heo", "lợn", "thịt")

    /** Từ khóa sản phẩm (Việt/Anh) → nguyên liệu chính để gọi TheMealDB filter.php?i=. */
    val MAIN_INGREDIENTS: List<Pair<List<String>, String>> = listOf(
        listOf("sữa", "milk") to "milk",
        listOf("gà", "chicken") to "chicken",
        listOf("bò", "beef") to "beef",
        listOf("heo", "lợn", "pork") to "pork",
        listOf("cá hồi", "salmon") to "salmon",
        listOf("tôm", "shrimp", "prawn") to "prawns",
        listOf("trứng", "egg") to "eggs",
        listOf("gạo", "cơm", "rice") to "rice",
        listOf("đậu hũ", "đậu phụ", "tofu") to "tofu",
        listOf("mì", "noodle", "pasta") to "spaghetti",
        listOf("cà chua", "tomato") to "tomatoes",
        listOf("nấm", "mushroom") to "mushrooms",
        listOf("khoai tây", "potato") to "potatoes",
        listOf("phô mai", "cheese") to "cheese",
        listOf("chuối", "banana") to "banana",
        listOf("yến mạch", "oat") to "oats"
    )

    fun guessMainIngredient(vararg texts: String?): String? {
        val haystack = texts.filterNotNull().joinToString(" ").lowercase()
        if (haystack.isBlank()) return null
        return MAIN_INGREDIENTS.firstOrNull { (keywords, _) -> keywords.any { haystack.contains(it) } }?.second
    }

    fun String.containsAny(keywords: List<String>): Boolean {
        val lower = lowercase()
        return keywords.any { lower.contains(it) }
    }

    data class Allergen(
        /** Từ người dùng nhập/chọn trong hồ sơ. */
        val userTerms: List<String>,
        /** Tag allergens_tags của Open Food Facts (đã bỏ "en:"). */
        val offTag: String,
        /** Từ khóa xuất hiện trong danh sách nguyên liệu công thức. */
        val ingredientTerms: List<String>
    )
}
