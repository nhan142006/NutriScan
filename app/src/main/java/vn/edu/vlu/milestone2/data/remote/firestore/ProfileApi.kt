package vn.edu.vlu.milestone2.data.remote.firestore

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

/** Read uses the encrypted token cache; SDK writes retain their offline queue. */
interface ProfileApi {
    @GET("v1/projects/{project}/databases/(default)/documents/users/{uid}")
    suspend fun getProfile(
        @Path("project") project: String,
        @Path("uid") uid: String,
        @Header("Authorization") authorization: String
    ): ProfileDocument
}

data class ProfileDocument(@SerializedName("fields") val fields: Map<String, ProfileField>?)
data class ProfileField(
    @SerializedName("stringValue") val stringValue: String?,
    @SerializedName("integerValue") val integerValue: String?,
    @SerializedName("arrayValue") val arrayValue: ProfileArray?
)
data class ProfileArray(@SerializedName("values") val values: List<ProfileField>?)
