package vn.edu.vlu.milestone2.performance

import android.content.Context
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.milestone2.MainActivity
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.local.NutriScanDatabase
import vn.edu.vlu.milestone2.data.local.entity.FavoriteProductEntity
import vn.edu.vlu.milestone2.data.local.entity.ProductEntity
import vn.edu.vlu.milestone2.data.local.entity.RecipeEntity
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import java.lang.ref.WeakReference

/** Test-only fixture data. Not included in Release, and never represented as live API data. */
@RunWith(AndroidJUnit4::class)
class Milestone05PerformanceTest {
    @Test fun seedPerformanceFixtures() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NutriScanDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        repeat(80) { index ->
            val barcode = "990000000" + index.toString().padStart(4, '0')
            db.productDao().upsertProduct(ProductEntity(
                barcode = barcode, name = "M05 fixture product ${index + 1}", brand = "Performance dataset",
                quantity = "100 g", imageUrl = null, nutriScore = "B", novaGroup = 1,
                ingredients = "oats, milk", allergens = "milk",
                nutritionJson = """{"energyKcal":120.0,"sugars":4.0,"fat":2.0,"proteins":6.0,"salt":0.1}""",
                cachedAt = now
            ))
            db.favoriteDao().insert(FavoriteProductEntity(productBarcode = barcode, addedAt = now - index))
        }
        repeat(40) { index ->
            db.recipeDao().upsert(RecipeEntity(
                recipeId = "user_m05_fixture_$index", title = "M05 fixture recipe ${index + 1}",
                imageUrl = null, category = "Performance dataset", ingredientsJson = "[\"oats\",\"milk\"]",
                stepsJson = "[\"Mix ingredients\",\"Cook for five minutes\"]", source = RecipeSource.USER,
                fitReason = null, basedOnScanIds = null, youtubeUrl = null, isSaved = true, updatedAt = now - index
            ))
        }
        assertTrue(db.favoriteDao().observeFavorites().first().size >= 80)
        assertTrue(db.recipeDao().observeMyRecipes().first().size >= 40)
        // Checkpoint before collecting a copy for the same-fixture Release comparison.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    @Test fun productViewsAreCollectableAfterTenRoundTrips() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val args = InstrumentationRegistry.getArguments()
            val email = args.getString("testEmail")
            val password = args.getString("testPassword")
            assumeNotNull(email, password)
            runBlocking { auth.signInWithEmailAndPassword(email!!, password!!).await() }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val references = mutableListOf<WeakReference<android.view.View>>()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                host.navController.navigate(R.id.favoritesFragment)
            }
            instrumentation.waitForIdleSync()
            repeat(10) {
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    host.navController.navigate(R.id.productDetailFragment, Bundle().apply {
                        putString("barcode", "9900000000000")
                    })
                }
                instrumentation.waitForIdleSync()
                Thread.sleep(150)
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    references += WeakReference(host.childFragmentManager.primaryNavigationFragment!!.requireView())
                    host.navController.popBackStack()
                }
                instrumentation.waitForIdleSync()
            }
        } finally { scenario.close() }
        instrumentation.waitForIdleSync()
        // LeakCanary's default watch delay is five seconds. Allow transient framework
        // transitions/subscription cancellation to settle before declaring a leak.
        repeat(32) {
            if (references.all { it.get() == null }) return@repeat
            Runtime.getRuntime().gc()
            System.runFinalization()
            Thread.sleep(250)
            instrumentation.waitForIdleSync()
        }
        assertEquals("Destroyed product views retained after GC", 0, references.count { it.get() != null })
    }
}
