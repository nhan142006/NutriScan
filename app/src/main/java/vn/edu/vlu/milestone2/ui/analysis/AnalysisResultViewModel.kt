package vn.edu.vlu.milestone2.ui.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkMonitor
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.ui.NavArgs

data class AnalysisUiState(
    val isLoading: Boolean = true,
    val scan: ScanResult? = null,
    val isOnline: Boolean = true
)

sealed interface AnalysisEvent {
    data object NoteSaved : AnalysisEvent
    data object Deleted : AnalysisEvent
}

/**
 * AnalysisViewModel: quan sát ScanResult trong Room bằng Flow → khi WorkManager cập nhật
 * PENDING → DONE, màn hình tự đổi sang kết quả mà không cần tải lại.
 */
class AnalysisResultViewModel(
    savedStateHandle: SavedStateHandle,
    private val scanRepository: ScanRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val scanId: Long = checkNotNull(savedStateHandle.get<Long>(NavArgs.SCAN_ID))

    val uiState: StateFlow<AnalysisUiState> = combine(
        scanRepository.observeScan(scanId),
        networkMonitor.isOnlineFlow
    ) { scan, online -> AnalysisUiState(isLoading = false, scan = scan, isOnline = online) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisUiState())

    private val _events = Channel<AnalysisEvent>(Channel.BUFFERED)
    val events: Flow<AnalysisEvent> = _events.receiveAsFlow()

    fun retry() {
        viewModelScope.launch { scanRepository.retryScan(scanId) }
    }

    fun saveNote(note: String) {
        viewModelScope.launch {
            scanRepository.updateNote(scanId, note)
            _events.send(AnalysisEvent.NoteSaved)
        }
    }

    fun delete() {
        viewModelScope.launch {
            scanRepository.deleteScan(scanId)
            _events.send(AnalysisEvent.Deleted)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnalysisResultViewModel(createSavedStateHandle(), ServiceLocator.scanRepository, ServiceLocator.networkMonitor)
            }
        }
    }
}
