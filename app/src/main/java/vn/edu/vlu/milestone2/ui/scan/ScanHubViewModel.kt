package vn.edu.vlu.milestone2.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

class ScanHubViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private val _results = MutableStateFlow<UiState<List<ProductSummary>>>(UiState.Idle)
    val results: StateFlow<UiState<List<ProductSummary>>> = _results.asStateFlow()

    private val _barcodeError = MutableStateFlow(false)
    val barcodeError: StateFlow<Boolean> = _barcodeError.asStateFlow()

    private val _openProduct = Channel<String>(Channel.BUFFERED)
    val openProduct: Flow<String> = _openProduct.receiveAsFlow()

    private var lastQuery: String? = null
    private var searchJob: Job? = null

    fun lookupBarcode(input: String) {
        val barcode = input.trim()
        val valid = barcode.matches(BARCODE_REGEX)
        _barcodeError.value = !valid
        if (valid) _openProduct.trySend(barcode)
    }

    fun search(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        lastQuery = text
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _results.value = UiState.Loading
            _results.value = when (val result = productRepository.searchProducts(text)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
            }
        }
    }

    fun retry() {
        lastQuery?.let(::search)
    }

    companion object {
        private val BARCODE_REGEX = Regex("^\\d{8,14}$")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ScanHubViewModel(ServiceLocator.productRepository) }
        }
    }
}
