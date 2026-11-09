package vn.edu.vlu.milestone2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthUser
import vn.edu.vlu.milestone2.di.ServiceLocator

/**
 * Quản lý phiên đăng nhập cho MainActivity.
 * Firebase Auth tự lưu token trên thiết bị, nên [isLoggedIn] vẫn true sau khi tắt/mở lại app.
 */
class SessionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: Boolean
        get() = authRepository.currentUser != null

    val currentUser: StateFlow<AuthUser?> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = authRepository.currentUser
        )

    fun signOut() {
        authRepository.signOut()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SessionViewModel(ServiceLocator.authRepository) }
        }
    }
}
