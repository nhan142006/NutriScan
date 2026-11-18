package vn.edu.vlu.milestone2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkMonitor
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.AuthUser
import vn.edu.vlu.milestone2.di.ServiceLocator

/** Trạng thái phiên đăng nhập + online/offline dùng chung cho MainActivity. */
class SessionViewModel(
    private val authRepository: AuthRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isLoggedIn: Boolean get() = authRepository.currentUser != null

    val currentUser: StateFlow<AuthUser?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUser)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SessionViewModel(ServiceLocator.authRepository, ServiceLocator.networkMonitor) }
        }
    }
}
