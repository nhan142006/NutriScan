package vn.edu.vlu.milestone2.core.ui

import vn.edu.vlu.milestone2.core.network.NetworkError

/** 3 trạng thái bắt buộc cho mọi màn hình gọi mạng (Milestone 04): Loading / Success / Error. */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: NetworkError, val httpCode: Int? = null) : UiState<Nothing>
}
