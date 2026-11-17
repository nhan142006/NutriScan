package vn.edu.vlu.milestone2.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.data.repository.ProductRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

/** Ví dụ dùng LiveData (Milestone 2 cho phép LiveData hoặc StateFlow) – quan sát bằng viewLifecycleOwner. */
class FavoritesViewModel(private val productRepository: ProductRepository) : ViewModel() {

    val favorites: LiveData<List<ProductSummary>> =
        productRepository.observeFavorites().map { items -> items.map { it.product } }.asLiveData()

    fun remove(barcode: String) {
        viewModelScope.launch { productRepository.removeFavorite(barcode) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { FavoritesViewModel(ServiceLocator.productRepository) }
        }
    }
}
