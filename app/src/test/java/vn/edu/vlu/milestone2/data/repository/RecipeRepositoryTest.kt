package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDbApi

class RecipeRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RecipeRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealDbApi::class.java)
        repository = RecipeRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun filterByCategory_parsesMeals() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"meals":[{"strMeal":"Baked salmon","strMealThumb":"https://img/1.jpg","idMeal":"52959"}]}"""
            )
        )

        val result = repository.getRecipesByCategory("Seafood")

        assertTrue(result is NetworkResult.Success)
        val meals = (result as NetworkResult.Success).data
        assertEquals("52959", meals.single().id)
        assertEquals("/filter.php?c=Seafood", server.takeRequest().path)
    }

    @Test
    fun search_nullMeals_returnsEmptyList() = runTest {
        server.enqueue(MockResponse().setBody("""{"meals":null}"""))

        val result = repository.searchRecipes("xyz")

        assertEquals(NetworkResult.Success(emptyList<Any>()), result)
    }

    @Test
    fun lookup_buildsIngredientList() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                {"meals":[{"idMeal":"1","strMeal":"Soup","strCategory":"Vegetarian","strArea":"Thai",
                  "strInstructions":"Boil.","strMealThumb":"https://img/2.jpg","strYoutube":"",
                  "strIngredient1":"Tofu","strMeasure1":"200g",
                  "strIngredient2":"Salt","strMeasure2":" ",
                  "strIngredient3":"","strMeasure3":"",
                  "strIngredient4":null,"strMeasure4":null}]}
                """.trimIndent()
            )
        )

        val result = repository.getRecipeDetail("1")

        assertTrue(result is NetworkResult.Success)
        val detail = (result as NetworkResult.Success).data
        assertEquals(listOf("Tofu - 200g", "Salt"), detail.ingredients)
        assertEquals(null, detail.youtubeUrl)
    }

    @Test
    fun lookup_nullMeals_returnsNotFound() = runTest {
        server.enqueue(MockResponse().setBody("""{"meals":null}"""))

        val result = repository.getRecipeDetail("999")

        assertEquals(NetworkResult.Error(NetworkError.NOT_FOUND, 404), result)
    }
}
