package vn.edu.vlu.milestone2.ui.profile

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.local.dao.HealthProfileDao
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity
import vn.edu.vlu.milestone2.data.local.entity.ProfileItemType
import vn.edu.vlu.milestone2.data.local.model.ProfileWithItems
import vn.edu.vlu.milestone2.data.remote.firestore.ProfileApi
import vn.edu.vlu.milestone2.data.remote.firestore.ProfileDocument
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthResult
import vn.edu.vlu.milestone2.data.repository.AuthUser
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.sync.ProfileCloudSync

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var dao: DelayedProfileDao
    private lateinit var auth: FakeAuthRepository
    private lateinit var viewModel: ProfileViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = DelayedProfileDao()
        auth = FakeAuthRepository()
        val cloud = ProfileCloudSync(
            firestore = { error("These tests must not initialize Firebase") },
            profileApi = object : ProfileApi {
                override suspend fun getProfile(project: String, uid: String, authorization: String): ProfileDocument =
                    error("These tests must not use the network")
            },
            projectId = "test-project",
            tokenProvider = { null }
        )
        viewModel = ProfileViewModel(HealthProfileRepository(dao, auth, cloud), auth)
        store.put("profile", viewModel)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun doesNotTreatPendingDatabaseReadAsAnEmptyProfile() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.profile.collect() }
        runCurrent()

        assertTrue(viewModel.profile.value.isLoading)
        dao.profiles.emit(savedProfile())
        runCurrent()

        assertFalse(viewModel.profile.value.isLoading)
        assertEquals("Saved User", viewModel.profile.value.profile?.fullName)
        assertEquals(2300, viewModel.profile.value.profile?.dailyCalorieTarget)
        assertEquals(setOf("Tiểu đường"), viewModel.form.value.conditions)
        assertEquals(setOf("Trứng"), viewModel.form.value.allergies)
    }

    @Test fun anActualEmptyDatabaseFinishesLoadingWithFormDefaults() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.profile.collect() }
        dao.profiles.emit(null)
        runCurrent()

        assertFalse(viewModel.profile.value.isLoading)
        assertNull(viewModel.profile.value.profile)
        assertEquals(2000, viewModel.form.value.defaultCalorieTarget)
        assertEquals("Giữ cân", viewModel.form.value.defaultDietGoal)
        assertTrue(viewModel.form.value.dietGoalOptions.contains("Giảm cân"))
    }

    @Test fun databaseUpdatesDoNotOverwriteSelectionsBeingEdited() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.profile.collect() }
        dao.profiles.emit(savedProfile())
        runCurrent()
        viewModel.toggleCondition("Tiểu đường", false)
        viewModel.addCustomCondition("  Bệnh khác  ")
        viewModel.toggleAllergy("Trứng", false)
        viewModel.addCustomAllergy("  Dị ứng khác  ")

        dao.profiles.emit(savedProfile().let { it.copy(profile = it.profile.copy(updatedAt = 2)) })
        runCurrent()

        assertEquals(setOf("Bệnh khác"), viewModel.form.value.conditions)
        assertEquals(setOf("Dị ứng khác"), viewModel.form.value.allergies)
    }

    @Test fun invalidFieldsStayInViewModelAndNeverWriteToTheDatabase() = runTest(dispatcher) {
        viewModel.save(" ", "0", "not-a-number", "Giữ cân")
        runCurrent()

        assertEquals(R.string.profile_error_name, viewModel.form.value.nameError)
        assertEquals(R.string.profile_error_age, viewModel.form.value.ageError)
        assertEquals(R.string.profile_error_calories, viewModel.form.value.calorieError)
        assertFalse(viewModel.form.value.isSaving)
    }

    @Test fun accountLabelTracksAuthenticationChanges() = runTest(dispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.accountEmail.collect() }
        runCurrent()
        assertEquals("first@example.com", viewModel.accountEmail.value)

        auth.user.value = AuthUser("second", "second@example.com", null)
        runCurrent()
        assertEquals("second@example.com", viewModel.accountEmail.value)

        viewModel.signOut()
        runCurrent()
        assertNull(viewModel.accountEmail.value)
    }

    private fun savedProfile() = ProfileWithItems(
        profile = HealthProfileEntity(
            id = 1, userId = "first", fullName = "Saved User", age = 24,
            dietGoal = "Tăng cơ", dailyCalorieTarget = 2300, updatedAt = 1
        ),
        items = listOf(
            HealthProfileItemEntity(profileId = 1, type = ProfileItemType.CONDITION, name = "Tiểu đường"),
            HealthProfileItemEntity(profileId = 1, type = ProfileItemType.ALLERGY, name = "Trứng")
        )
    )

    /** No initial emission: models the interval before Room returns its first result. */
    private class DelayedProfileDao : HealthProfileDao() {
        val profiles = MutableSharedFlow<ProfileWithItems?>(replay = 1)
        override fun observeProfile(userId: String): Flow<ProfileWithItems?> = profiles
        override suspend fun getProfile(userId: String): ProfileWithItems? = error("Unexpected database read")
        override suspend fun findProfileId(userId: String): Long? = error("Unexpected database read")
        override suspend fun insertProfile(profile: HealthProfileEntity): Long = error("Unexpected database write")
        override suspend fun updateProfile(profile: HealthProfileEntity): Int = error("Unexpected database write")
        override suspend fun deleteProfile(userId: String): Int = error("Unexpected database write")
        override suspend fun insertItems(items: List<HealthProfileItemEntity>): Unit = error("Unexpected database write")
        override suspend fun deleteItems(profileId: Long): Unit = error("Unexpected database write")
    }

    private class FakeAuthRepository : AuthRepository {
        val user = MutableStateFlow<AuthUser?>(AuthUser("first", "first@example.com", null))
        override val currentUser: AuthUser? get() = user.value
        override fun observeAuthState(): Flow<AuthUser?> = user
        override suspend fun signIn(email: String, password: String): AuthResult = error("Not used")
        override suspend fun register(displayName: String, email: String, password: String): AuthResult = error("Not used")
        override suspend fun signOut() { user.value = null }
        override suspend fun getIdToken(): String? = null
    }
}
