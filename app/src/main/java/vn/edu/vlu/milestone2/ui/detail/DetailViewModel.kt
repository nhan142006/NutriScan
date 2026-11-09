package vn.edu.vlu.milestone2.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.repository.DetailRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.ui.scanner.ScannerViewModel

internal class DetailViewModel(
    private val barcode: String,
    private val repository: DetailRepository
) : ViewModel() {

    private val _productState = MutableStateFlow<UiState<Product>>(UiState.Idle)
    val productState = _productState.asStateFlow()

    private val _savedState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val savedState = _savedState.asStateFlow()

    private var lastSavedRequest: suspend (String) -> NetworkResult<Boolean> = repository::isSaved

    init {
        loadProduct()
    }

    fun loadProduct() {
        if (_productState.value is UiState.Loading) return
        if (!ScannerViewModel.isValidBarcode(barcode)) {
            _productState.value = UiState.Error(NetworkError.BAD_REQUEST)
            return
        }
        _productState.value = UiState.Loading
        viewModelScope.launch {
            val result = repository.getProduct(barcode)
            _productState.value = result.toUiState()
            if (result is NetworkResult.Success) updateSavedState(repository::isSaved)
        }
    }

    fun save() {
        if (_savedState.value == UiState.Success(false)) updateSavedState(repository::save)
    }

    fun delete() {
        if (_savedState.value == UiState.Success(true)) updateSavedState(repository::delete)
    }

    fun retrySavedRequest() {
        if (_savedState.value is UiState.Error) updateSavedState(lastSavedRequest)
    }

    private fun updateSavedState(request: suspend (String) -> NetworkResult<Boolean>) {
        if (_savedState.value is UiState.Loading) return
        lastSavedRequest = request
        _savedState.value = UiState.Loading
        viewModelScope.launch {
            _savedState.value = request(barcode).toUiState()
        }
    }

    private fun <T> NetworkResult<T>.toUiState(): UiState<T> = when (this) {
        is NetworkResult.Success -> UiState.Success(data)
        is NetworkResult.Error -> UiState.Error(error, httpCode)
    }

    companion object {
        const val ARG_BARCODE = "barcode"

        val Factory = viewModelFactory {
            initializer {
                val barcode = createSavedStateHandle().get<String>(ARG_BARCODE).orEmpty()
                DetailViewModel(barcode, ServiceLocator.detailRepository)
            }
        }
    }
}
