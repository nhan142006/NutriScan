package vn.edu.vlu.milestone2.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vn.edu.vlu.milestone2.BuildConfig
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.network.NetworkModule
import vn.edu.vlu.milestone2.core.network.NetworkMonitor
import vn.edu.vlu.milestone2.data.local.NutriScanDatabase
import vn.edu.vlu.milestone2.data.remote.gemini.GeminiDataSource
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.FirebaseAuthRepository
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.data.repository.RecipeRepository
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.data.storage.ScanImageStorage
import vn.edu.vlu.milestone2.data.security.EncryptedSessionStore
import vn.edu.vlu.milestone2.data.sync.ProfileCloudSync
import vn.edu.vlu.milestone2.domain.NutritionAnalyzer
import vn.edu.vlu.milestone2.domain.RecipeCompatibilityChecker
import vn.edu.vlu.milestone2.work.ScanWorkScheduler

/**
 * Dependency container thủ công (manual DI), khởi tạo trong NutriScanApp.
 * Fragment chỉ lấy ViewModel; ViewModel lấy Repository từ đây – không ViewModel nào giữ Context.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
    }

    private val database by lazy { NutriScanDatabase.getInstance(appContext) }
    private val networkModule by lazy { NetworkModule(appContext) }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(FirebaseAuth.getInstance(), EncryptedSessionStore(appContext))
    }

    private val geminiDataSource by lazy {
        GeminiDataSource(
            api = networkModule.geminiApi,
            gson = networkModule.gson,
            apiKey = BuildConfig.GEMINI_API_KEY,
            model = BuildConfig.GEMINI_MODEL
        )
    }

    val isAiConfigured: Boolean get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    val healthProfileRepository: HealthProfileRepository by lazy {
        HealthProfileRepository(
            dao = database.healthProfileDao(),
            authRepository = authRepository,
            cloudSync = ProfileCloudSync(
                firestore = { FirebaseFirestore.getInstance() },
                profileApi = networkModule.profileApi,
                projectId = appContext.getString(R.string.project_id),
                tokenProvider = authRepository::getIdToken
            )
        )
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(
            api = networkModule.openFoodFactsApi,
            productDao = database.productDao(),
            favoriteDao = database.favoriteDao(),
            gson = networkModule.gson
        )
    }

    val scanRepository: ScanRepository by lazy {
        ScanRepository(
            scanDao = database.scanResultDao(),
            gemini = geminiDataSource,
            analyzer = NutritionAnalyzer(),
            imageStorage = imageStorage,
            scheduler = ScanWorkScheduler(appContext),
            profileRepository = healthProfileRepository,
            gson = networkModule.gson
        )
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepository(
            api = networkModule.mealDbApi,
            recipeDao = database.recipeDao(),
            gemini = geminiDataSource,
            profileRepository = healthProfileRepository,
            scanRepository = scanRepository,
            gson = networkModule.gson
        )
    }

    val imageStorage: ScanImageStorage by lazy { ScanImageStorage(appContext) }

    val compatibilityChecker: RecipeCompatibilityChecker by lazy { RecipeCompatibilityChecker() }
}
