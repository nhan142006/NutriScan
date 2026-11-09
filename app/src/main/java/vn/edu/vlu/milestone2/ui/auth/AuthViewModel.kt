package vn.edu.vlu.milestone2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.repository.AuthRepository

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = repository.isSignedIn,
            userEmail = repository.currentUserEmail
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(emailInput: String, password: String) {
        if (_uiState.value.isLoading) return
        val email = emailInput.trim()
        if (!validate(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.signIn(email, password) }
                .onSuccess {
                    _uiState.value = AuthUiState(
                        isAuthenticated = true,
                        userEmail = repository.currentUserEmail ?: email
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Đăng nhập thất bại."
                    )
                }
        }
    }

    fun register(emailInput: String, password: String, confirmPassword: String) {
        if (_uiState.value.isLoading) return
        val email = emailInput.trim()
        val isValid = validate(email, password)
        val confirmError = if (confirmPassword != password) "Mật khẩu xác nhận không khớp." else null
        if (!isValid || confirmError != null) {
            _uiState.value = _uiState.value.copy(confirmPasswordError = confirmError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.register(email, password) }
                .onSuccess {
                    _uiState.value = AuthUiState(
                        isAuthenticated = true,
                        userEmail = repository.currentUserEmail ?: email
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Đăng ký thất bại."
                    )
                }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearErrors() {
        val state = _uiState.value
        if (
            state.emailError != null || state.passwordError != null ||
            state.confirmPasswordError != null || state.errorMessage != null
        ) {
            _uiState.value = state.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                errorMessage = null
            )
        }
    }

    private fun validate(email: String, password: String): Boolean {
        val emailError = when {
            email.isBlank() -> "Vui lòng nhập email."
            !EMAIL_PATTERN.matches(email) -> "Email không hợp lệ."
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "Vui lòng nhập mật khẩu."
            password.length < 6 -> "Mật khẩu cần có ít nhất 6 ký tự."
            else -> null
        }
        _uiState.value = _uiState.value.copy(
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = null,
            errorMessage = null
        )
        return emailError == null && passwordError == null
    }

    companion object {
        private val EMAIL_PATTERN = Regex(
            pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            option = RegexOption.IGNORE_CASE
        )
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        return AuthViewModel(repository) as T
    }
}
