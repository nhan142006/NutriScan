package vn.edu.vlu.milestone2.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.remote.firestore.ProfileApi

/**
 * Đồng bộ HealthProfile lên Cloud Firestore: users/{uid}.
 * Firestore tự xếp hàng ghi khi offline nên KHÔNG await() lệnh set (await sẽ treo tới khi có mạng).
 * Security rules gợi ý: allow read, write: if request.auth.uid == userId;
 */
class ProfileCloudSync(
    private val firestore: () -> FirebaseFirestore,
    private val profileApi: ProfileApi,
    private val projectId: String,
    private val tokenProvider: suspend () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun push(profile: HealthProfile) {
        runCatching {
            firestore().collection(USERS).document(profile.userId).set(
                mapOf(
                    "fullName" to profile.fullName,
                    "age" to profile.age,
                    "dietGoal" to profile.dietGoal,
                    "dailyCalorieTarget" to profile.dailyCalorieTarget,
                    "conditions" to profile.conditions,
                    "allergies" to profile.allergies,
                    "updatedAt" to profile.updatedAt
                )
            )
        }
    }

    fun delete(userId: String) {
        runCatching { firestore().collection(USERS).document(userId).delete() }
    }

    /** Lấy hồ sơ trên cloud (tối đa 5 giây) – dùng khi đăng nhập trên máy mới. */
    suspend fun pull(userId: String): HealthProfile? = withContext(ioDispatcher) { try {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val token = tokenProvider() ?: return@withTimeoutOrNull null
            val fields = profileApi.getProfile(projectId, userId, "Bearer $token").fields
                ?: return@withTimeoutOrNull null
            HealthProfile(
                id = 0,
                userId = userId,
                fullName = fields["fullName"]?.stringValue.orEmpty(),
                age = fields["age"]?.integerValue?.toIntOrNull() ?: 0,
                dietGoal = fields["dietGoal"]?.stringValue.orEmpty(),
                dailyCalorieTarget = fields["dailyCalorieTarget"]?.integerValue?.toIntOrNull() ?: 2000,
                conditions = fields["conditions"]?.arrayValue?.values?.mapNotNull { it.stringValue }.orEmpty(),
                allergies = fields["allergies"]?.arrayValue?.values?.mapNotNull { it.stringValue }.orEmpty(),
                updatedAt = fields["updatedAt"]?.integerValue?.toLongOrNull() ?: System.currentTimeMillis()
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null // Room remains authoritative if the document is missing or networking fails.
    } }

    private companion object {
        const val USERS = "users"
        const val PULL_TIMEOUT_MS = 5_000L
    }
}
