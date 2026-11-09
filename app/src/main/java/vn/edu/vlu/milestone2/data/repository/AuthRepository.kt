package vn.edu.vlu.milestone2.data.repository

firebase-authentication
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp

main
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
firebase-authentication
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext

    private val auth: FirebaseAuth? by lazy {
        val firebaseApp = FirebaseApp.getApps(appContext).firstOrNull()
            ?: FirebaseApp.initializeApp(appContext)
        firebaseApp?.let(FirebaseAuth::getInstance)
    }

    val currentUserEmail: String?
        get() = auth?.currentUser?.email

    val isSignedIn: Boolean
        get() = auth?.currentUser != null

    suspend fun signIn(email: String, password: String) {
        val firebaseAuth = requireConfiguredAuth()
        try {
            firebaseAuth.signInWithEmailAndPassword(email, password).awaitCompletion()
        } catch (throwable: Throwable) {
            throw throwable.toFriendlyAuthException(isRegistration = false)
        }
    }

    suspend fun register(email: String, password: String) {
        val firebaseAuth = requireConfiguredAuth()
        try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).awaitCompletion()
        } catch (throwable: Throwable) {
            throw throwable.toFriendlyAuthException(isRegistration = true)
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    private fun requireConfiguredAuth(): FirebaseAuth = auth ?: throw AuthException(
        "Firebase chưa được cấu hình. Hãy thêm app/google-services.json và build lại ứng dụng."
    )

    private fun Throwable.toFriendlyAuthException(isRegistration: Boolean): AuthException {
        val message = when (this) {
            is FirebaseAuthWeakPasswordException ->
                "Mật khẩu quá yếu. Mật khẩu cần có ít nhất 6 ký tự."
            is FirebaseAuthUserCollisionException ->
                "Email này đã được đăng ký. Hãy đăng nhập hoặc dùng email khác."
            is FirebaseAuthInvalidUserException ->
                "Không tìm thấy tài khoản với email này."
            is FirebaseAuthInvalidCredentialsException -> if (isRegistration) {
                "Email không hợp lệ. Vui lòng kiểm tra lại."
            } else {
                "Email hoặc mật khẩu không đúng."
            }
            is FirebaseNetworkException ->
                "Không thể kết nối Firebase. Vui lòng kiểm tra Internet."
            is FirebaseTooManyRequestsException ->
                "Bạn đã thử quá nhiều lần. Vui lòng đợi rồi thử lại."
            else -> "Xác thực thất bại. Vui lòng thử lại."
        }
        return AuthException(message, this)
    }
}

private suspend fun Task<*>.awaitCompletion() {
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            val exception = task.exception
            if (task.isSuccessful) {
                continuation.resume(Unit)
            } else {
                continuation.resumeWithException(
                    exception ?: IllegalStateException("Firebase task failed without an exception")
                )
            }
        }
    }
}

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
main
