package vn.edu.vlu.milestone2.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.ProductLookup
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.ui.NavArgs

sealed interface ProductEvent {
    /** [aiError] != null → đã dùng phân tích offline thay cho Gemini. */
    data class OpenAnalysis(val scanId: Long, val aiError: NetworkError?) : ProductEvent
    data class OpenRecipes(val scanId: Long) : ProductEvent
    data class FavoriteChanged(val added: Boolean) : ProductEvent
}

class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    val barcode: String = checkNotNull(savedStateHandle.get<String>(NavArgs.BARCODE))

    private val _product = MutableStateFlow<UiState<ProductLookup>>(UiState.Loading)
    val product: StateFlow<UiState<ProductLookup>> = _product.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    val isFavorite: StateFlow<Boolean> = productRepository.observeIsFavorite(barcode)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _events = Channel<ProductEvent>(Channel.BUFFERED)
    val events: Flow<ProductEvent> = _events.receiveAsFlow()

    /** Lần phân tích gần nhất trong phiên này – tái dùng khi bấm "Tìm món". */
    private var lastScanId: Long? = null

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _product.value = UiState.Loading
            _product.value = when (val result = productRepository.getProduct(barcode, forceRefresh)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
            }
        }
    }

    fun analyze() = runAnalysis { scanId, aiError -> ProductEvent.OpenAnalysis(scanId, aiError) }

    /** "Cook With What You Scanned": phân tích (nếu chưa) rồi mở module Công thức với scanId này. */
    fun findRecipes() {
        val existing = lastScanId
        if (existing != null) {
            _events.trySend(ProductEvent.OpenRecipes(existing))
        } else {
            runAnalysis { scanId, _ -> ProductEvent.OpenRecipes(scanId) }
        }
    }

    fun toggleFavorite() {
        val lookup = (_product.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val add = !isFavorite.value
            if (add) productRepository.addFavorite(lookup.product) else productRepository.removeFavorite(barcode)
            _events.send(ProductEvent.FavoriteChanged(add))
        }
    }

    private fun runAnalysis(toEvent: (Long, NetworkError?) -> ProductEvent) {
        val lookup = (_product.value as? UiState.Success)?.data ?: return
        if (_isAnalyzing.value) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val analysis = scanRepository.analyzeProduct(lookup.product)
            lastScanId = analysis.scanId
            _isAnalyzing.value = false
            _events.send(toEvent(analysis.scanId, analysis.aiError))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProductDetailViewModel(
                    createSavedStateHandle(),
                    ServiceLocator.productRepository,
                    ServiceLocator.scanRepository
                )
            }
        }
    }
}
