package vn.edu.vlu.milestone2.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class CachedSessionToken(
    val userId: String,
    val token: String,
    val cachedAtMillis: Long,
    val expiresAtMillis: Long
) {
    fun isUsableFor(userId: String, nowMillis: Long): Boolean =
        this.userId == userId && token.isNotBlank() &&
            nowMillis >= cachedAtMillis && nowMillis < expiresAtMillis &&
            nowMillis - cachedAtMillis < MAX_CACHE_AGE_MILLIS

    companion object {
        const val MAX_CACHE_AGE_MILLIS = 5 * 60 * 1_000L
    }
}

/** This app-owned cache is separate from Firebase SDK's own session persistence. */
interface SessionTokenStore {
    fun read(): CachedSessionToken?
    fun save(value: CachedSessionToken)
    fun clear()
}

/** Only call from an I/O dispatcher: Keystore initialization and commit both touch disk. */
@Suppress("DEPRECATION") // Explicit course requirement; isolated here for future migration.
class EncryptedSessionStore(context: Context, private val fileName: String = FILE_NAME) : SessionTokenStore {
    private val appContext = context.applicationContext
    private val preferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun read(): CachedSessionToken? {
        val uid = preferences.getString("uid", null) ?: return null
        val token = preferences.getString("id_token", null) ?: return null
        return CachedSessionToken(
            uid, token, preferences.getLong("cached_at", 0), preferences.getLong("expires_at", 0)
        )
    }

    override fun save(value: CachedSessionToken) {
        check(preferences.edit()
            .putString("uid", value.userId)
            .putString("id_token", value.token)
            .putLong("cached_at", value.cachedAtMillis)
            .putLong("expires_at", value.expiresAtMillis)
            .commit()) { "Could not persist encrypted session" }
    }

    override fun clear() {
        check(preferences.edit().clear().commit()) { "Could not clear encrypted session" }
    }

    companion object { const val FILE_NAME = "nutriscan_secure_session" }
}
