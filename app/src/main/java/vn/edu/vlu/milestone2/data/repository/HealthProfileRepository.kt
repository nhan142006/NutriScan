package vn.edu.vlu.milestone2.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.edu.vlu.milestone2.data.local.dao.HealthProfileDao
import vn.edu.vlu.milestone2.data.local.entity.HealthProfileEntity
import vn.edu.vlu.milestone2.data.local.model.ProfileWithConditions

data class HealthProfile(
    val id: Long,
    val fullName: String,
    val age: Int,
    val dailyCalorieTarget: Int,
    val conditions: List<String>,
    val updatedAt: Long
)

class HealthProfileRepository(
    private val healthProfileDao: HealthProfileDao
) {
    fun observeProfiles(): Flow<List<HealthProfile>> =
        healthProfileDao.observeProfiles().map { profiles ->
            profiles.map(ProfileWithConditions::toDomain)
        }

    fun observeProfile(profileId: Long): Flow<HealthProfile?> =
        healthProfileDao.observeProfile(profileId).map { it?.toDomain() }

    suspend fun saveProfile(
        id: Long,
        fullName: String,
        age: Int,
        dailyCalorieTarget: Int,
        conditions: List<String>
    ): Long {
        val normalizedConditions = conditions
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase() }

        return healthProfileDao.saveProfileWithConditions(
            profile = HealthProfileEntity(
                id = id,
                fullName = fullName.trim(),
                age = age,
                dailyCalorieTarget = dailyCalorieTarget,
                updatedAt = System.currentTimeMillis()
            ),
            conditionNames = normalizedConditions
        )
    }

    suspend fun deleteProfile(profileId: Long): Boolean =
        healthProfileDao.deleteProfileById(profileId) == 1
}

private fun ProfileWithConditions.toDomain(): HealthProfile = HealthProfile(
    id = profile.id,
    fullName = profile.fullName,
    age = profile.age,
    dailyCalorieTarget = profile.dailyCalorieTarget,
    conditions = conditions.map { it.name }.sorted(),
    updatedAt = profile.updatedAt
)
