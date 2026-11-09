package vn.edu.vlu.milestone2.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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
