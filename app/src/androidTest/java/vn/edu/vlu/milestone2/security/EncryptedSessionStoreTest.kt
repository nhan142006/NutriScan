package vn.edu.vlu.milestone2.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import vn.edu.vlu.milestone2.data.security.CachedSessionToken
import vn.edu.vlu.milestone2.data.security.EncryptedSessionStore
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedSessionStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "m05_verification_session"

    @After fun cleanup() { context.deleteSharedPreferences(name) }

    @Test fun tokenRoundTripsAcrossInstancesWithoutPlaintextOnDisk() = runBlocking(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val value = CachedSessionToken("m05-private-user", "m05-private-bearer-token", now, now + 300_000)
        EncryptedSessionStore(context, name).save(value)
        assertEquals(value, EncryptedSessionStore(context, name).read())
        val disk = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml").readText()
        assertFalse(disk.contains(value.userId))
        assertFalse(disk.contains(value.token))
        assertFalse(disk.contains("id_token"))
    }

    @Test fun logoutClearsPersistentToken() = runBlocking(Dispatchers.IO) {
        val store = EncryptedSessionStore(context, name)
        store.save(CachedSessionToken("m05-user", "m05-token", 1, 100))
        store.clear()
        assertNull(EncryptedSessionStore(context, name).read())
    }
}
