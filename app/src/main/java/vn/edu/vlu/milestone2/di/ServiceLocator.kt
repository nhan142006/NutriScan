package vn.edu.vlu.milestone2.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import vn.edu.vlu.milestone2.core.network.NetworkModule
import vn.edu.vlu.milestone2.data.local.NutriScanDatabase
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.DetailRepository
import vn.edu.vlu.milestone2.data.repository.FirebaseAuthRepository
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.data.repository.SavedProductSession
import vn.edu.vlu.milestone2.domain.NutritionAnalyzer
import java.io.IOException

/**
 * Dependency container thủ công (manual DI). Khởi tạo 1 lần trong [vn.edu.vlu.milestone2.NutriScanApp].
 * Activity/Fragment KHÔNG gọi API trực tiếp; chúng chỉ lấy ViewModel, ViewModel lấy Repository từ đây.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val networkModule: NetworkModule by lazy { NetworkModule(appContext) }

    val productRepository: ProductRepository by lazy {
        ProductRepository(networkModule.openFoodFactsApi)
    }

    internal val detailRepository: DetailRepository by lazy {
        DetailRepository(productRepository, networkModule.savedProductApi, ::savedProductSession)
    }

    private suspend fun savedProductSession(): SavedProductSession? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val project = checkNotNull(FirebaseApp.getInstance().options.projectId)
        val token = try {
            user.getIdToken(false).await().token ?: return null
        } catch (error: FirebaseNetworkException) {
            throw IOException(error)
        }
        return SavedProductSession(project, user.uid, "Bearer $token")
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(networkModule.mealDbApi)
    }

    val healthProfileRepository: HealthProfileRepository by lazy {
        HealthProfileRepository(NutriScanDatabase.getInstance(appContext).healthProfileDao())
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance())
    }

    val nutritionAnalyzer: NutritionAnalyzer by lazy { NutritionAnalyzer() }
}
