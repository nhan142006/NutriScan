package vn.edu.vlu.milestone2.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity

@RunWith(AndroidJUnit4::class)
class HealthProfileDaoTest {
    private lateinit var database: NutriScanDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NutriScanDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun crudProfileWithConditions_andCascadeDelete() = runBlocking {
        val dao = database.healthProfileDao()
        val profileId = dao.saveProfileWithConditions(
            profile = HealthProfileEntity(
                fullName = "Nguyễn Văn A",
                age = 22,
                dailyCalorieTarget = 2_100,
                updatedAt = 1L
            ),
            conditionNames = listOf("Tiểu đường", "Dị ứng đậu phộng")
        )

        val inserted = dao.observeProfile(profileId).first()
        assertEquals("Nguyễn Văn A", inserted?.profile?.fullName)
        assertEquals(2, inserted?.conditions?.size)

        dao.saveProfileWithConditions(
            profile = inserted!!.profile.copy(age = 23, updatedAt = 2L),
            conditionNames = listOf("Tiểu đường")
        )
        val updated = dao.observeProfile(profileId).first()
        assertEquals(23, updated?.profile?.age)
        assertEquals(listOf("Tiểu đường"), updated?.conditions?.map { it.name })

        assertEquals(1, dao.deleteProfileById(profileId))
        assertEquals(null, dao.observeProfile(profileId).first())
        assertTrue(dao.observeConditions(profileId).first().isEmpty())
    }
}
