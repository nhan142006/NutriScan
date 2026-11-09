package vn.edu.vlu.milestone2.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.repository.AuthError
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthResult
import vn.edu.vlu.milestone2.di.ServiceLocator

data class AuthUiState(
    val isLoading: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    val authError: AuthError? = null,
    val isAuthenticated: Boolean = false
)

/**
 * ViewModel cho Đăng nhập / Đăng ký.
 * UI -> AuthViewModel -> AuthRepository -> Firebase Authentication.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val emailValidator: (String) -> Boolean = { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        _uiState.update {
            it.copy(emailError = emailError, passwordError = passwordError, authError = null)
        }
        if (emailError != null || passwordError != null) return

        authenticate { authRepository.signIn(email, password) }
    }

    fun register(displayName: String, email: String, password: String, confirmPassword: String) {
        val nameError = if (displayName.isBlank()) R.string.auth_validation_name else null
        val emailError = validateEmail(email)
        val passwordError = validatePassword(password)
        val confirmError = if (password != confirmPassword) R.string.auth_validation_confirm else null
        _uiState.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                authError = null
            )
        }
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) return

        authenticate { authRepository.register(displayName, email, password) }
    }

    private fun authenticate(request: suspend () -> AuthResult) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authError = null) }
            when (val result = request()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = true)
                }
                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, authError = result.error)
                }
            }
        }
    }

    private fun validateEmail(email: String): Int? =
        if (emailValidator(email.trim())) null else R.string.auth_validation_email

    private fun validatePassword(password: String): Int? =
        if (password.length >= MIN_PASSWORD_LENGTH) null else R.string.auth_validation_password

    companion object {
        const val MIN_PASSWORD_LENGTH = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(ServiceLocator.authRepository) }
        }
    }
}

@StringRes
fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.USER_NOT_FOUND -> R.string.auth_error_user_not_found
    AuthError.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
