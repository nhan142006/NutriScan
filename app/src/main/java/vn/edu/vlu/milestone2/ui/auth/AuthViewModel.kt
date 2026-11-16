package vn.edu.vlu.milestone2.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.repository.AuthError
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthResult
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

/** Lỗi validate theo từng ô nhập (TextInputLayout.error) – lưu id chuỗi, không giữ Context. */
data class AuthFormState(
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    val isLoading: Boolean = false
)

sealed interface AuthEvent {
    data object LoggedIn : AuthEvent
    data object Registered : AuthEvent
    data class ShowError(@StringRes val message: Int) : AuthEvent
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: HealthProfileRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    /** Validate real-time khi người dùng gõ (gọi từ doAfterTextChanged). */
    fun onEmailChanged(email: String) = _form.update {
        it.copy(emailError = if (email.isBlank() || isValidEmail(email)) null else R.string.auth_validation_email)
    }

    fun onPasswordChanged(password: String) = _form.update {
        it.copy(passwordError = if (password.isEmpty() || password.length >= MIN_PASSWORD) null else R.string.auth_validation_password)
    }

    fun onConfirmChanged(password: String, confirm: String) = _form.update {
        it.copy(confirmError = if (confirm.isEmpty() || confirm == password) null else R.string.auth_validation_confirm)
    }

    fun signIn(email: String, password: String) {
        if (_form.value.isLoading) return
        val emailError = if (isValidEmail(email)) null else R.string.auth_validation_email
        val passwordError = if (password.length >= MIN_PASSWORD) null else R.string.auth_validation_password
        _form.value = AuthFormState(emailError = emailError, passwordError = passwordError)
        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    profileRepository.restoreFromCloudIfMissing()
                    _events.send(AuthEvent.LoggedIn)
                }
                is AuthResult.Failure -> _events.send(AuthEvent.ShowError(result.error.toMessage()))
            }
            _form.update { it.copy(isLoading = false) }
        }
    }

    fun register(name: String, email: String, password: String, confirm: String) {
        if (_form.value.isLoading) return
        val state = AuthFormState(
            nameError = if (name.isNotBlank()) null else R.string.auth_validation_name,
            emailError = if (isValidEmail(email)) null else R.string.auth_validation_email,
            passwordError = if (password.length >= MIN_PASSWORD) null else R.string.auth_validation_password,
            confirmError = if (confirm == password) null else R.string.auth_validation_confirm
        )
        _form.value = state
        if (listOf(state.nameError, state.emailError, state.passwordError, state.confirmError).any { it != null }) return

        viewModelScope.launch {
            _form.update { it.copy(isLoading = true) }
            when (val result = authRepository.register(name, email, password)) {
                is AuthResult.Success -> _events.send(AuthEvent.Registered)
                is AuthResult.Failure -> _events.send(AuthEvent.ShowError(result.error.toMessage()))
            }
            _form.update { it.copy(isLoading = false) }
        }
    }

    private fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    private fun AuthError.toMessage(): Int = when (this) {
        AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
        AuthError.USER_NOT_FOUND -> R.string.auth_error_user_not_found
        AuthError.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
        AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
        AuthError.INVALID_EMAIL -> R.string.auth_error_invalid_email
        AuthError.NETWORK -> R.string.auth_error_network
        AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
        AuthError.UNKNOWN -> R.string.auth_error_unknown
    }

    companion object {
        const val MIN_PASSWORD = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(ServiceLocator.authRepository, ServiceLocator.healthProfileRepository) }
        }
    }
}
