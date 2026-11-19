package vn.edu.vlu.milestone2.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSearchTranslatorTest {

    private fun translate(query: String) = RecipeSearchTranslator.translate(query)

    @Test
    fun friedChicken_inVietnamese_becomesEnglishPhraseAndIngredientFilter() {
        val plan = translate("Gà rán")
        assertEquals("fried chicken", plan.englishPhrase)
        assertEquals(listOf("fried chicken", "chicken", "fried"), plan.nameQueries)
        assertEquals(listOf("chicken"), plan.ingredientFilters)
    }

    @Test
    fun unaccentedInput_matchesSameTerms() {
        assertEquals("fried chicken", translate("ga ran").englishPhrase)
        assertEquals("fried rice", translate("com chien").englishPhrase)
    }

    @Test
    fun accentedInput_mustMatchItsOwnAccents() {
        assertEquals(listOf("beef"), translate("bò").ingredientFilters)
        assertEquals(listOf("butter"), translate("bơ").ingredientFilters)
        // "cà chua" là cà chua, không phải "cá" + "chua".
        assertEquals(listOf("tomatoes"), translate("cà chua").ingredientFilters)
    }

    @Test
    fun longestPhraseWins() {
        val plan = translate("cá hồi nướng")
        assertEquals("grilled salmon", plan.englishPhrase)
        assertEquals(listOf("salmon"), plan.ingredientFilters)
    }

    @Test
    fun categoryTerms_requestCategoryFilter() {
        assertEquals(listOf("Seafood"), translate("hải sản").categories)
        assertEquals(listOf("Dessert"), translate("món tráng miệng").categories)
    }

    @Test
    fun englishQuery_isPassedThrough() {
        val plan = translate("Chicken Curry")
        assertFalse(plan.isTranslated)
        assertEquals(listOf("chicken curry"), plan.nameQueries)

        val unknown = translate("lasagne")
        assertFalse(unknown.isTranslated)
        assertEquals(listOf("lasagne"), unknown.nameQueries)
    }

    @Test
    fun score_ranksFullMatchAboveIngredientAboveMethod() {
        val plan = translate("gà rán")
        val kfc = plan.score("Kentucky Fried Chicken")
        val curry = plan.score("Chicken Curry")
        val rice = plan.score("Egg Fried Rice")
        val unrelated = plan.score("Beef Wellington")
        assertTrue(kfc > curry)
        assertTrue(curry > rice)
        assertTrue(rice > unrelated)
        assertEquals(0, unrelated)
    }
}
