package vn.edu.vlu.milestone2.data.sync

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.edu.vlu.milestone2.data.remote.firestore.ProfileApi

class ProfileCloudSyncTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ProfileApi

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(ProfileApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    private fun sync(token: String?) = ProfileCloudSync(
        firestore = { error("REST reads must not initialize the Firestore SDK") },
        profileApi = api,
        projectId = "test-project",
        tokenProvider = { token }
    )

    @Test fun restoreUsesBearerHeaderAndParsesFirestoreFields() = runTest {
        server.enqueue(MockResponse().setBody("""{"fields":{
          "fullName":{"stringValue":"Test User"},"age":{"integerValue":"24"},
          "dietGoal":{"stringValue":"Keep fit"},"dailyCalorieTarget":{"integerValue":"2100"},
          "conditions":{"arrayValue":{"values":[{"stringValue":"diabetes"}]}},
          "allergies":{"arrayValue":{}},"updatedAt":{"integerValue":"12345"}
        }}"""))
        val profile = sync("private-test-token").pull("user-a")!!
        assertEquals("Test User", profile.fullName)
        assertEquals(24, profile.age)
        assertEquals(2100, profile.dailyCalorieTarget)
        assertEquals(listOf("diabetes"), profile.conditions)
        assertEquals(emptyList<String>(), profile.allergies)
        val request = server.takeRequest()
        assertEquals("Bearer private-test-token", request.getHeader("Authorization"))
        assertEquals("/v1/projects/test-project/databases/(default)/documents/users/user-a", request.path)
    }

    @Test fun missingTokenNeverMakesANetworkRequest() = runTest {
        assertNull(sync(null).pull("user-a"))
        assertEquals(0, server.requestCount)
    }

    @Test fun missingCloudDocumentPreservesLocalFallback() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(sync("test-token").pull("user-a"))
    }
}
