package vn.edu.vlu.milestone2.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

/**
 * ProductRepository (Retrofit) -> ScannerViewModel -> StateFlow<UiState> -> ScannerFragment.
 * Coroutine chạy trong viewModelScope; Repository tự chuyển sang Dispatchers.IO.
 */
class ScannerViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<UiState<List<ProductSummary>>>(UiState.Idle)
    val searchState: StateFlow<UiState<List<ProductSummary>>> = _searchState.asStateFlow()

    private var lastQuery: String? = null
    private var searchJob: Job? = null

    /** Tải danh sách mặc định 1 lần (không gọi lại khi xoay màn hình). */
    fun loadInitial(defaultQuery: String) {
        if (lastQuery == null) search(defaultQuery)
    }

    fun search(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        lastQuery = normalized

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchState.value = UiState.Loading
            _searchState.value = when (val result = productRepository.searchProducts(normalized)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
            }
        }
    }

    fun retry() {
        lastQuery?.let(::search)
    }

    companion object {
        private val BARCODE_PATTERN = Regex("^\\d{8,14}$")

        fun isValidBarcode(value: String): Boolean = BARCODE_PATTERN.matches(value.trim())

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ScannerViewModel(ServiceLocator.productRepository) }
        }
    }
}
