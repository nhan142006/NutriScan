package vn.edu.vlu.milestone2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.repository.DetailRepository

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val data: String) : DetailUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : DetailUiState()
}

class DetailViewModel(private val repository: DetailRepository = DetailRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            repository.fetchDetail(id)
                .onSuccess { data ->
                    _uiState.value = DetailUiState.Success(data)
                }
                .onFailure { error ->
                    val userMsg = when (error.message) {
                        "OFFLINE_ERROR" -> "No internet connection! Please check your network or airplane mode."
                        "TIMEOUT_ERROR" -> "Connection timed out. Please try again."
                        "HTTP_404" -> "Data not found (Error 404)."
                        "HTTP_500" -> "Server internal error (Error 500)."
                        else -> "An unexpected network error occurred."
                    }
                    _uiState.value = DetailUiState.Error(userMsg)
                }
        }
    }

    fun deleteItem(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteItem(id)
                .onSuccess { onSuccess() }
                .onFailure { onError("Deletion failed. Please check your network connection!") }
        }
    }
}