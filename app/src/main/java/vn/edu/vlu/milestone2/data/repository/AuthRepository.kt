package vn.edu.vlu.milestone2.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?
)

enum class AuthError {
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NETWORK,
    TOO_MANY_REQUESTS,
    UNKNOWN
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/** Hợp đồng xác thực - ViewModel chỉ phụ thuộc interface này, không gọi Firebase trực tiếp. */
interface AuthRepository {
    /** Firebase tự lưu phiên đăng nhập; giá trị này khác null sau khi mở lại app. */
    val currentUser: AuthUser?

    fun observeAuthState(): Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun register(displayName: String, email: String, password: String): AuthResult

    fun signOut()
}

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = runAuth {
        val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
        requireNotNull(result.user).toAuthUser()
    }

    override suspend fun register(displayName: String, email: String, password: String): AuthResult = runAuth {
        val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user)
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()
        user.updateProfile(profileUpdate).await()
        AuthUser(uid = user.uid, email = user.email, displayName = displayName.trim())
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun runAuth(block: suspend () -> AuthUser): AuthResult = withContext(ioDispatcher) {
        try {
            AuthResult.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AuthResult.Failure(throwable.toAuthError())
        }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email, displayName = displayName)

private fun Throwable.toAuthError(): AuthError = when (this) {
    // WeakPassword là lớp con của InvalidCredentials nên phải kiểm tra trước.
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_ALREADY_IN_USE
    is FirebaseAuthInvalidUserException -> AuthError.USER_NOT_FOUND
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.INVALID_CREDENTIALS
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    else -> AuthError.UNKNOWN
}
