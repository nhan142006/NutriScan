package vn.edu.vlu.milestone2.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.core.network.NetworkResult
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.data.repository.HealthProfile
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.di.ServiceLocator
import vn.edu.vlu.milestone2.domain.AnalysisResult
import vn.edu.vlu.milestone2.domain.NutritionAnalyzer

data class ProductAnalysis(
    val product: Product,
    val profile: HealthProfile?,
    val result: AnalysisResult
)

/**
 * ProductRepository (Retrofit/Open Food Facts) + HealthProfileRepository (Room)
 * -> AnalysisViewModel -> StateFlow<UiState<ProductAnalysis>> -> AnalysisFragment.
 */
class AnalysisViewModel(
    private val productRepository: ProductRepository,
    private val healthProfileRepository: HealthProfileRepository,
    private val analyzer: NutritionAnalyzer
) : ViewModel() {

    private val _analysisState = MutableStateFlow<UiState<ProductAnalysis>>(UiState.Idle)
    val analysisState: StateFlow<UiState<ProductAnalysis>> = _analysisState.asStateFlow()

    private var currentBarcode: String? = null
    private var analysisJob: Job? = null

    /** Gọi từ Fragment; không tải lại nếu cùng mã vạch (ví dụ khi xoay màn hình). */
    fun analyze(barcode: String) {
        val normalized = barcode.trim()
        if (normalized.isEmpty()) {
            _analysisState.value = UiState.Idle
            return
        }
        if (normalized == currentBarcode && _analysisState.value !is UiState.Error) return
        currentBarcode = normalized
        load(normalized)
    }

    fun retry() {
        currentBarcode?.let(::load)
    }

    private fun load(barcode: String) {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _analysisState.value = UiState.Loading
            _analysisState.value = when (val result = productRepository.getProduct(barcode)) {
                is NetworkResult.Error -> UiState.Error(result.error, result.httpCode)
                is NetworkResult.Success -> {
                    // Hồ sơ cập nhật gần nhất (Room sắp xếp theo updated_at DESC).
                    val profile = healthProfileRepository.observeProfiles().first().firstOrNull()
                    UiState.Success(
                        ProductAnalysis(
                            product = result.data,
                            profile = profile,
                            result = analyzer.analyze(result.data, profile)
                        )
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnalysisViewModel(
                    productRepository = ServiceLocator.productRepository,
                    healthProfileRepository = ServiceLocator.healthProfileRepository,
                    analyzer = ServiceLocator.nutritionAnalyzer
                )
            }
        }
    }
}
