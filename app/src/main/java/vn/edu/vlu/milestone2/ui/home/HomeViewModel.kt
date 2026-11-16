package vn.edu.vlu.milestone2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

data class HomeUiState(
    val displayName: String? = null,
    val profile: HealthProfile? = null,
    val recentScans: List<ScanResult> = emptyList(),
    val pendingCount: Int = 0,
    val isAiConfigured: Boolean = false
)

class HomeViewModel(
    authRepository: AuthRepository,
    profileRepository: HealthProfileRepository,
    scanRepository: ScanRepository,
    isAiConfigured: Boolean
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.observeAuthState(),
        profileRepository.observeCurrentProfile(),
        scanRepository.observeRecent(RECENT_LIMIT),
        scanRepository.observePendingCount()
    ) { user, profile, recent, pending ->
        HomeUiState(
            displayName = profile?.fullName?.takeIf { it.isNotBlank() } ?: user?.displayName ?: user?.email,
            profile = profile,
            recentScans = recent,
            pendingCount = pending,
            isAiConfigured = isAiConfigured
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isAiConfigured = isAiConfigured))

    companion object {
        private const val RECENT_LIMIT = 3

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    ServiceLocator.authRepository,
                    ServiceLocator.healthProfileRepository,
                    ServiceLocator.scanRepository,
                    ServiceLocator.isAiConfigured
                )
            }
        }
    }
}
