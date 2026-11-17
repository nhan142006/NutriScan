package vn.edu.vlu.milestone2.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import vn.edu.vlu.milestone2.data.model.HealthProfile

/**
 * Đồng bộ HealthProfile lên Cloud Firestore: users/{uid}.
 * Firestore tự xếp hàng ghi khi offline nên KHÔNG await() lệnh set (await sẽ treo tới khi có mạng).
 * Security rules gợi ý: allow read, write: if request.auth.uid == userId;
 */
class ProfileCloudSync(private val firestore: () -> FirebaseFirestore) {

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
    suspend fun pull(userId: String): HealthProfile? = runCatching {
        withTimeoutOrNull(PULL_TIMEOUT_MS) {
            val snapshot = firestore().collection(USERS).document(userId).get().await()
            if (!snapshot.exists()) return@withTimeoutOrNull null
            HealthProfile(
                id = 0,
                userId = userId,
                fullName = snapshot.getString("fullName").orEmpty(),
                age = snapshot.getLong("age")?.toInt() ?: 0,
                dietGoal = snapshot.getString("dietGoal").orEmpty(),
                dailyCalorieTarget = snapshot.getLong("dailyCalorieTarget")?.toInt() ?: 2000,
                conditions = (snapshot.get("conditions") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                allergies = (snapshot.get("allergies") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        }
    }.getOrNull()

    private companion object {
        const val USERS = "users"
        const val PULL_TIMEOUT_MS = 5_000L
    }
}
