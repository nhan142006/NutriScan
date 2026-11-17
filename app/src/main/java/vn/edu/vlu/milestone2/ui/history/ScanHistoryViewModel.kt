package vn.edu.vlu.milestone2.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

enum class HistoryFilter { ALL, SUITABLE, LIMIT, AVOID, PENDING }

data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val items: List<ScanResult> = emptyList(),
    val totalCount: Int = 0
)

/** ScanHistoryViewModel – Read/Update(note)/Delete lịch sử quét từ Room. */
class ScanHistoryViewModel(private val scanRepository: ScanRepository) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(scanRepository.observeHistory(), filter) { all, current ->
        HistoryUiState(
            filter = current,
            items = all.filter { it.matches(current) },
            totalCount = all.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun delete(scanId: Long) {
        viewModelScope.launch { scanRepository.deleteScan(scanId) }
    }

    private fun ScanResult.matches(filter: HistoryFilter): Boolean = when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.SUITABLE -> status == ScanStatus.DONE && rating == Rating.SUITABLE
        HistoryFilter.LIMIT -> status == ScanStatus.DONE && rating == Rating.LIMIT
        HistoryFilter.AVOID -> status == ScanStatus.DONE && rating == Rating.AVOID
        HistoryFilter.PENDING -> status != ScanStatus.DONE
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ScanHistoryViewModel(ServiceLocator.scanRepository) }
        }
    }
}
