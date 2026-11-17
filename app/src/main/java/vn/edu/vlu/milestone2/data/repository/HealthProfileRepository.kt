package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import vn.edu.vlu.milestone2.data.local.dao.HealthProfileDao
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.ProfileItemType
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.sync.ProfileCloudSync

/** Room là nguồn sự thật; Firestore chỉ là bản sao lưu trên cloud. */
class HealthProfileRepository(
    private val dao: HealthProfileDao,
    private val authRepository: AuthRepository,
    private val cloudSync: ProfileCloudSync
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCurrentProfile(): Flow<HealthProfile?> =
        authRepository.observeAuthState().flatMapLatest { user ->
            if (user == null) flowOf(null) else dao.observeProfile(user.uid).map { it?.toDomain() }
        }

    suspend fun getCurrentProfile(): HealthProfile? {
        val uid = authRepository.currentUser?.uid ?: return null
        return dao.getProfile(uid)?.toDomain()
    }

    suspend fun saveProfile(
        fullName: String,
        age: Int,
        dietGoal: String,
        dailyCalorieTarget: Int,
        conditions: List<String>,
        allergies: List<String>
    ): Long {
        val uid = checkNotNull(authRepository.currentUser?.uid) { "Chưa đăng nhập" }
        val now = System.currentTimeMillis()
        val id = dao.upsertProfileWithItems(
            profile = HealthProfileEntity(
                userId = uid,
                fullName = fullName.trim(),
                age = age,
                dietGoal = dietGoal.trim(),
                dailyCalorieTarget = dailyCalorieTarget,
                updatedAt = now
            ),
            items = conditions.toItems(ProfileItemType.CONDITION) + allergies.toItems(ProfileItemType.ALLERGY)
        )
        dao.getProfile(uid)?.toDomain()?.let(cloudSync::push)
        return id
    }

    suspend fun deleteProfile(): Boolean {
        val uid = authRepository.currentUser?.uid ?: return false
        cloudSync.delete(uid)
        return dao.deleteProfile(uid) > 0
    }

    /** Đăng nhập trên máy mới: nếu Room trống thì khôi phục hồ sơ từ Firestore. */
    suspend fun restoreFromCloudIfMissing() {
        val uid = authRepository.currentUser?.uid ?: return
        if (dao.findProfileId(uid) != null) return
        val remote = cloudSync.pull(uid) ?: return
        dao.upsertProfileWithItems(
            profile = HealthProfileEntity(
                userId = uid,
                fullName = remote.fullName,
                age = remote.age,
                dietGoal = remote.dietGoal,
                dailyCalorieTarget = remote.dailyCalorieTarget,
                updatedAt = remote.updatedAt
            ),
            items = remote.conditions.toItems(ProfileItemType.CONDITION) + remote.allergies.toItems(ProfileItemType.ALLERGY)
        )
    }
}
