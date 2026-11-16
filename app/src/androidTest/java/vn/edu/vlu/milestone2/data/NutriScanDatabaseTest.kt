package vn.edu.vlu.milestone2.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.milestone2.data.local.NutriScanDatabase
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity
import vn.edu.vlu.milestone2.data.local.entity.ProfileItemType
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.ScanResultEntity
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus

/** Kiểm thử CRUD Room (Milestone 07) trên database in-memory. */
@RunWith(AndroidJUnit4::class)
class NutriScanDatabaseTest {

    private lateinit var db: NutriScanDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NutriScanDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun profileUpsert_replacesItems_andCascadeDeletes() = runBlocking {
        val dao = db.healthProfileDao()
        val profile = HealthProfileEntity(userId = "u1", fullName = "A", age = 20, dietGoal = "Giữ cân", dailyCalorieTarget = 2000, updatedAt = 1)
        dao.upsertProfileWithItems(profile, listOf(HealthProfileItemEntity(profileId = 0, type = ProfileItemType.ALLERGY, name = "Đậu phộng")))
        dao.upsertProfileWithItems(profile.copy(age = 21), listOf(HealthProfileItemEntity(profileId = 0, type = ProfileItemType.CONDITION, name = "Tiểu đường")))

        val loaded = dao.observeProfile("u1").first()!!
        assertEquals(21, loaded.profile.age)
        assertEquals(listOf("Tiểu đường"), loaded.items.map { it.name })

        dao.deleteProfile("u1")
        assertNull(dao.getProfile("u1"))
    }

    @Test
    fun scanResult_storesOnlyFileUri_andUpdatesStatus() = runBlocking {
        val dao = db.scanResultDao()
        val id = dao.insert(
            ScanResultEntity(
                productBarcode = null, imagePath = "file:///data/user/0/app/files/scans/1.jpg", productName = "Pending",
                ingredientsJson = "[]", rating = null, analysisJson = null, note = null, status = ScanStatus.PENDING,
                source = ScanSource.LABEL_PHOTO, errorMessage = null, createdAt = 1
            )
        )
        assertEquals(listOf(id), dao.getPendingIds())
        dao.update(dao.get(id)!!.copy(status = ScanStatus.DONE, rating = Rating.SUITABLE))
        dao.updateNote(id, "mua cho bố")
        val saved = dao.get(id)!!
        assertEquals(ScanStatus.DONE, saved.status)
        assertEquals("mua cho bố", saved.note)
        assertTrue(saved.imagePath!!.startsWith("file://"))
        assertEquals(1, dao.getRecentUsable(10).size)
    }
}
