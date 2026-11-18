package vn.edu.vlu.milestone2.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedSessionTokenTest {
    private val token = CachedSessionToken("user-a", "sensitive-token", 1_000, 301_000)

    @Test fun validTokenIsReusableForTheSameUser() = assertTrue(token.isUsableFor("user-a", 2_000))
    @Test fun changingAccountsNeverReusesAnotherUsersToken() = assertFalse(token.isUsableFor("user-b", 2_000))
    @Test fun expiryIsExclusive() = assertFalse(token.isUsableFor("user-a", 301_000))
    @Test fun cacheCannotOutliveFiveMinutesEvenIfStoredExpiryIsLonger() =
        assertFalse(token.copy(expiresAtMillis = Long.MAX_VALUE).isUsableFor("user-a", 301_001))
    @Test fun clockMovingBackwardsInvalidatesCache() = assertFalse(token.isUsableFor("user-a", 999))
    @Test fun blankTokenIsNeverUsable() = assertFalse(token.copy(token = " ").isUsableFor("user-a", 2_000))
    @Test fun firebaseExpiryCanInvalidateBeforeCacheTtl() =
        assertFalse(token.copy(expiresAtMillis = 1_500).isUsableFor("user-a", 1_500))
}
