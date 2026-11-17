package vn.edu.vlu.milestone2.core.network

/**
 * Kết quả trả về từ Repository. Repository không bao giờ ném exception lên ViewModel;
 * mọi lỗi được gói trong [Error].
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class Error(
        val error: NetworkError,
        val httpCode: Int? = null
    ) : NetworkResult<Nothing>
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
}

fun <T> NetworkResult<T>.getOrNull(): T? = (this as? NetworkResult.Success)?.data
