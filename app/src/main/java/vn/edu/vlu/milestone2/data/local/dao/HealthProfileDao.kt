package vn.edu.vlu.milestone2.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileItemEntity
import vn.edu.vlu.milestone2.data.local.model.ProfileWithItems

@Dao
abstract class HealthProfileDao {

    @Transaction
    @Query("SELECT * FROM health_profiles WHERE user_id = :userId LIMIT 1")
    abstract fun observeProfile(userId: String): Flow<ProfileWithItems?>

    @Transaction
    @Query("SELECT * FROM health_profiles WHERE user_id = :userId LIMIT 1")
    abstract suspend fun getProfile(userId: String): ProfileWithItems?

    @Query("SELECT id FROM health_profiles WHERE user_id = :userId LIMIT 1")
    abstract suspend fun findProfileId(userId: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertProfile(profile: HealthProfileEntity): Long

    @Update
    abstract suspend fun updateProfile(profile: HealthProfileEntity): Int

    @Query("DELETE FROM health_profiles WHERE user_id = :userId")
    abstract suspend fun deleteProfile(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertItems(items: List<HealthProfileItemEntity>)

    @Query("DELETE FROM health_profile_items WHERE profile_id = :profileId")
    abstract suspend fun deleteItems(profileId: Long)

    /** Tạo mới hoặc cập nhật hồ sơ + thay toàn bộ danh sách bệnh/dị ứng trong 1 transaction. */
    @Transaction
    open suspend fun upsertProfileWithItems(
        profile: HealthProfileEntity,
        items: List<HealthProfileItemEntity>
    ): Long {
        val existingId = findProfileId(profile.userId)
        val profileId = if (existingId == null) {
            insertProfile(profile.copy(id = 0))
        } else {
            updateProfile(profile.copy(id = existingId))
            existingId
        }
        deleteItems(profileId)
        if (items.isNotEmpty()) insertItems(items.map { it.copy(id = 0, profileId = profileId) })
        return profileId
    }
}
