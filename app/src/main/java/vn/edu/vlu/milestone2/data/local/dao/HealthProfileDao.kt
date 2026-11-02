package vn.edu.vlu.milestone2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import vn.edu.vlu.milestone2.data.local.entity.HealthConditionEntity
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.model.ProfileWithConditions

@Dao
abstract class HealthProfileDao {

    @Transaction
    @Query("SELECT * FROM health_profiles ORDER BY updated_at DESC")
    abstract fun observeProfiles(): Flow<List<ProfileWithConditions>>

    @Transaction
    @Query("SELECT * FROM health_profiles WHERE id = :profileId")
    abstract fun observeProfile(profileId: Long): Flow<ProfileWithConditions?>

    @Query("SELECT * FROM health_conditions WHERE profile_id = :profileId ORDER BY name")
    abstract fun observeConditions(profileId: Long): Flow<List<HealthConditionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertProfile(profile: HealthProfileEntity): Long

    @Update
    abstract suspend fun updateProfile(profile: HealthProfileEntity): Int

    @Delete
    abstract suspend fun deleteProfile(profile: HealthProfileEntity): Int

    @Query("DELETE FROM health_profiles WHERE id = :profileId")
    abstract suspend fun deleteProfileById(profileId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertCondition(condition: HealthConditionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertConditions(conditions: List<HealthConditionEntity>): List<Long>

    @Update
    abstract suspend fun updateCondition(condition: HealthConditionEntity): Int

    @Delete
    abstract suspend fun deleteCondition(condition: HealthConditionEntity): Int

    @Query("DELETE FROM health_conditions WHERE profile_id = :profileId")
    abstract suspend fun deleteConditionsForProfile(profileId: Long): Int

    @Transaction
    open suspend fun saveProfileWithConditions(
        profile: HealthProfileEntity,
        conditionNames: List<String>
    ): Long {
        val profileId = if (profile.id == 0L) {
            insertProfile(profile)
        } else {
            check(updateProfile(profile) == 1) { "Không tìm thấy hồ sơ có id=${profile.id}" }
            profile.id
        }

        deleteConditionsForProfile(profileId)
        if (conditionNames.isNotEmpty()) {
            insertConditions(
                conditionNames.map { name ->
                    HealthConditionEntity(profileId = profileId, name = name)
                }
            )
        }
        return profileId
    }
}
