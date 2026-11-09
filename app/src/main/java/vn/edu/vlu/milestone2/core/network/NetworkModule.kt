package vn.edu.vlu.milestone2.core.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.vlu.milestone2.BuildConfig
import vn.edu.vlu.milestone2.data.remote.mealdb.MealDbApi
import vn.edu.vlu.milestone2.data.remote.openfoodfacts.OpenFoodFactsApi
import java.util.concurrent.TimeUnit

/**
 * Cấu hình Retrofit + OkHttp dùng chung cho toàn ứng dụng.
 */
class NetworkModule(context: Context) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(ConnectivityInterceptor(context))
        .addInterceptor(userAgentInterceptor())
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        createRetrofit(BuildConfig.OPEN_FOOD_FACTS_BASE_URL).create(OpenFoodFactsApi::class.java)
    }

    val mealDbApi: MealDbApi by lazy {
        createRetrofit(BuildConfig.MEAL_DB_BASE_URL).create(MealDbApi::class.java)
    }

    private fun createRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /** Open Food Facts yêu cầu User-Agent định danh ứng dụng. */
    private fun userAgentInterceptor() = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "NutriScan-Android/${BuildConfig.VERSION_NAME} (VLU student project)")
                .build()
        )
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 20L
    }
}
