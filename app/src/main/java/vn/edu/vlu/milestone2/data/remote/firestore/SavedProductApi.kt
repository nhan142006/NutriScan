package vn.edu.vlu.milestone2.data.remote.firestore

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

interface SavedProductApi {

    @GET(DOCUMENT_PATH)
    suspend fun get(
        @Path("project") project: String,
        @Path("uid") uid: String,
        @Path("barcode") barcode: String,
        @Header("Authorization") authorization: String
    )

    @PATCH(DOCUMENT_PATH)
    suspend fun save(
        @Path("project") project: String,
        @Path("uid") uid: String,
        @Path("barcode") barcode: String,
        @Header("Authorization") authorization: String,
        @Body document: SavedProductDocument
    )

    @DELETE(DOCUMENT_PATH)
    suspend fun delete(
        @Path("project") project: String,
        @Path("uid") uid: String,
        @Path("barcode") barcode: String,
        @Header("Authorization") authorization: String
    )

    private companion object {
        const val DOCUMENT_PATH =
            "v1/projects/{project}/databases/(default)/documents/users/{uid}/savedProducts/{barcode}"
    }
}

data class SavedProductDocument(
    @SerializedName("fields") val fields: Map<String, FirestoreString>
)

data class FirestoreString(@SerializedName("stringValue") val value: String)
