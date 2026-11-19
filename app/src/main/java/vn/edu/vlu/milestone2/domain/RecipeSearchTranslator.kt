package vn.edu.vlu.milestone2.domain

import java.text.Normalizer

/**
 * Dịch từ khóa tìm món tiếng Việt sang kế hoạch truy vấn TheMealDB (API chỉ có tên món tiếng Anh).
 *
 * Ví dụ "gà rán" → cụm "fried chicken"; tìm theo tên ("fried chicken", "chicken", "fried")
 * và theo nguyên liệu chính (filter.php?i=chicken), rồi xếp món khớp nhiều từ khóa lên trước.
 * Nhận cả chữ không dấu ("ga ran") và từ tiếng Anh ("chicken"). Chạy offline, không gọi AI.
 */
object RecipeSearchTranslator {

    // Khai báo trước DICTIONARY/index vì khởi tạo object chạy theo thứ tự từ trên xuống.
    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")

    /** Từ nối không mang nghĩa món ăn. */
    private val STOP_WORDS = setOf("mon", "voi", "va", "kieu", "thit", "the", "and", "with", "of")

    enum class Kind { METHOD, INGREDIENT, DISH }

    /**
     * Một từ/cụm từ tiếng Việt. [english] phần tử đầu dùng để ghép cụm tìm kiếm, các phần tử sau
     * là từ đồng nghĩa dùng để xếp hạng. [ingredient] là tên nguyên liệu cho filter.php?i=,
     * [category] là nhóm món cho filter.php?c=.
     */
    data class Term(
        val vietnamese: String,
        val kind: Kind,
        val english: List<String>,
        val ingredient: String? = null,
        val category: String? = null
    )

    data class Plan(
        val original: String,
        val terms: List<Term>,
        /** Từ không có trong từ điển (thường là từ tiếng Anh người dùng gõ thẳng). */
        val passthrough: List<String>
    ) {
        val isTranslated: Boolean get() = terms.isNotEmpty()

        /** Cụm tiếng Anh theo trật tự tự nhiên: cách nấu + nguyên liệu + loại món ("fried chicken"). */
        val englishPhrase: String
            get() = (terms.filter { it.kind == Kind.METHOD } + terms.filter { it.kind == Kind.INGREDIENT } +
                terms.filter { it.kind == Kind.DISH })
                .map { it.english.first() }
                .plus(passthrough)
                .distinct()
                .joinToString(" ")

        /** Truy vấn search.php?s= theo thứ tự ưu tiên, không trùng. */
        val nameQueries: List<String>
            get() = buildList {
                if (!isTranslated) {
                    add(original)
                    return@buildList
                }
                add(englishPhrase)
                terms.filter { it.kind == Kind.INGREDIENT }.forEach { add(it.english.first()) }
                terms.filter { it.kind != Kind.INGREDIENT }.forEach { add(it.english.first()) }
                addAll(passthrough)
            }.filter { it.isNotBlank() }.distinct()

        val ingredientFilters: List<String>
            get() = terms.mapNotNull { it.ingredient }.distinct()

        val categories: List<String>
            get() = terms.mapNotNull { it.category }.distinct()

        /** Điểm liên quan của 1 tên món: khớp cả cụm > khớp nguyên liệu > khớp cách nấu/loại món. */
        fun score(mealName: String): Int {
            val name = mealName.lowercase()
            var score = 0
            if (englishPhrase.isNotBlank() && name.contains(englishPhrase)) score += PHRASE_SCORE
            terms.forEach { term ->
                if (term.english.any { name.contains(it) }) {
                    score += if (term.kind == Kind.INGREDIENT) INGREDIENT_SCORE else OTHER_SCORE
                }
            }
            passthrough.forEach { if (name.contains(it)) score += OTHER_SCORE }
            return score
        }
    }

    private const val PHRASE_SCORE = 5
    private const val INGREDIENT_SCORE = 3
    private const val OTHER_SCORE = 2
    private const val MAX_PHRASE_WORDS = 3

    private fun method(vi: String, vararg en: String) = Term(vi, Kind.METHOD, en.toList())
    private fun dish(vi: String, vararg en: String, category: String? = null) =
        Term(vi, Kind.DISH, en.toList(), category = category)
    private fun ingredient(vi: String, vararg en: String, filter: String? = en.first(), category: String? = null) =
        Term(vi, Kind.INGREDIENT, en.toList(), ingredient = filter, category = category)

    /**
     * Từ điển món ăn thường gặp. Khi hai mục trùng nhau sau khi bỏ dấu (bò/bơ, cá/cà),
     * mục khớp đúng dấu được ưu tiên, nếu không thì lấy mục đứng trước.
     */
    val DICTIONARY: List<Term> = listOf(
        // Cách chế biến
        method("rán", "fried", "fry"),
        method("chiên", "fried", "fry"),
        method("chiên giòn", "fried", "crispy"),
        method("nướng", "grilled", "roast", "baked", "bbq", "barbecue"),
        method("quay", "roast", "roasted"),
        method("luộc", "boiled", "poached"),
        method("hấp", "steamed"),
        method("xào", "stir fry", "stir-fry", "fried"),
        method("kho", "braised", "stew"),
        method("om", "braised", "stew"),
        method("hầm", "stew", "casserole", "braised"),
        method("áp chảo", "pan fried", "seared"),
        method("chua ngọt", "sweet and sour", "sweet sour"),
        method("cay", "spicy", "chilli", "chili"),
        method("sốt", "sauce"),
        method("tỏi", "garlic"),
        // Nguyên liệu chính
        ingredient("gà", "chicken"),
        ingredient("thịt gà", "chicken"),
        ingredient("cánh gà", "chicken wings", "wings", filter = "chicken"),
        ingredient("bò", "beef"),
        ingredient("thịt bò", "beef"),
        ingredient("heo", "pork"),
        ingredient("lợn", "pork"),
        ingredient("thịt heo", "pork"),
        ingredient("thịt lợn", "pork"),
        ingredient("ba chỉ", "pork belly", "pork", filter = "pork"),
        ingredient("sườn", "ribs", "rib", filter = "pork"),
        ingredient("cừu", "lamb", category = "Lamb"),
        ingredient("dê", "goat", filter = null, category = "Goat"),
        ingredient("vịt", "duck"),
        ingredient("cá hồi", "salmon"),
        ingredient("cá ngừ", "tuna"),
        ingredient("cá tuyết", "cod"),
        ingredient("cá", "fish", "salmon", "cod", "tuna", "haddock", filter = null, category = "Seafood"),
        ingredient("tôm", "prawn", "shrimp", filter = "prawns"),
        ingredient("cua", "crab", filter = null, category = "Seafood"),
        ingredient("mực", "squid", "calamari", filter = null, category = "Seafood"),
        ingredient("hải sản", "seafood", "fish", "prawn", filter = null, category = "Seafood"),
        ingredient("trứng", "egg", filter = "eggs"),
        ingredient("cơm", "rice"),
        ingredient("gạo", "rice"),
        ingredient("mì", "noodle", "noodles", "lo mein", "ramen", filter = null),
        ingredient("mỳ", "noodle", "noodles", "lo mein", "ramen", filter = null),
        ingredient("bún", "noodle", "noodles", "vermicelli", filter = null),
        ingredient("phở", "pho", "noodle", "noodles", filter = null),
        ingredient("mì ý", "spaghetti", "pasta", category = "Pasta"),
        ingredient("mỳ ý", "spaghetti", "pasta", category = "Pasta"),
        ingredient("nui", "pasta", "macaroni", filter = null, category = "Pasta"),
        ingredient("đậu hũ", "tofu"),
        ingredient("đậu phụ", "tofu"),
        ingredient("nấm", "mushroom", filter = "mushrooms"),
        ingredient("khoai tây", "potato", filter = "potatoes"),
        ingredient("cà chua", "tomato", filter = "tomatoes"),
        ingredient("cà tím", "aubergine", "eggplant", filter = "aubergine"),
        ingredient("rau", "vegetable", "veg", filter = null, category = "Vegetarian"),
        ingredient("chay", "vegetarian", "vegan", "veg", filter = null, category = "Vegetarian"),
        ingredient("xúc xích", "sausage", filter = "sausages"),
        ingredient("giăm bông", "ham"),
        ingredient("phô mai", "cheese"),
        ingredient("sô cô la", "chocolate"),
        ingredient("chuối", "banana"),
        ingredient("táo", "apple", filter = "apples"),
        ingredient("dâu", "strawberry", "strawberries", filter = "strawberries"),
        ingredient("xoài", "mango"),
        ingredient("dứa", "pineapple"),
        ingredient("chanh", "lemon", "lime"),
        ingredient("đậu", "bean", "beans", filter = null),
        ingredient("bơ", "butter", "avocado", filter = "butter"),
        ingredient("yến mạch", "oat", "oats", "porridge", filter = "oats"),
        // Loại món
        dish("súp", "soup"),
        dish("canh", "soup", "broth"),
        dish("lẩu", "hotpot", "hot pot", "stew"),
        dish("cà ri", "curry"),
        dish("gỏi", "salad"),
        dish("nộm", "salad"),
        dish("salad", "salad"),
        dish("cháo", "porridge", "congee"),
        dish("bánh mì", "bread", "sandwich", "burger"),
        dish("bánh xèo", "pancake"),
        dish("bánh kem", "cake"),
        dish("bánh ngọt", "cake", "tart", "pie", category = "Dessert"),
        dish("bánh", "cake", "pie", "tart", "bread"),
        dish("tráng miệng", "dessert", "pudding", category = "Dessert"),
        dish("chè", "dessert", "pudding", category = "Dessert"),
        dish("kem", "ice cream"),
        dish("bữa sáng", "breakfast", category = "Breakfast"),
        dish("ăn sáng", "breakfast", category = "Breakfast"),
        dish("bánh bao", "bun", "dumpling", "dumplings"),
        dish("há cảo", "dumpling", "dumplings"),
        dish("pizza", "pizza")
    )

    /** Khóa bỏ dấu → các mục cùng khóa, giữ thứ tự khai báo. */
    private val index: Map<String, List<Term>> = DICTIONARY.groupBy { fold(it.vietnamese) }

    fun translate(query: String): Plan {
        val original = query.trim().lowercase().replace(WHITESPACE, " ")
        val words = tokenize(original)
        val terms = mutableListOf<Term>()
        val passthrough = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            var matched: Term? = null
            var length = minOf(MAX_PHRASE_WORDS, words.size - i)
            while (length > 0 && matched == null) {
                val phrase = words.subList(i, i + length).joinToString(" ")
                matched = index[fold(phrase)]?.let { candidates ->
                    // Gõ có dấu thì phải khớp đúng dấu ("bơ" ≠ "bò", "của" ≠ "cua");
                    // gõ không dấu thì lấy mục khai báo trước ("bo" → bò).
                    candidates.firstOrNull { it.vietnamese == phrase }
                        ?: candidates.first().takeIf { phrase == fold(phrase) }
                }
                if (matched == null) length--
            }
            val term = matched
            if (term != null) {
                if (terms.none { it.vietnamese == term.vietnamese }) terms += term
                i += length
            } else {
                // Từ không có trong từ điển: giữ lại nếu là chữ Latin không dấu (thường là tiếng Anh).
                val word = words[i]
                if (word == fold(word) && word.length > 1 && word !in STOP_WORDS) passthrough += word
                i++
            }
        }
        return Plan(original = original, terms = terms, passthrough = passthrough.distinct())
    }

    private fun tokenize(text: String): List<String> =
        text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }

    /** Bỏ dấu tiếng Việt để khớp cả khi người dùng gõ không dấu ("ga ran"). */
    fun fold(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('đ', 'd')

}
