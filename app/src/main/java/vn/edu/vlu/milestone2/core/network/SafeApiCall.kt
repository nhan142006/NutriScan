package vn.edu.vlu.milestone2.core.network

import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Chạy [block] trên [dispatcher] (mặc định Dispatchers.IO, tách khỏi Main Thread)
 * và chuyển mọi exception thành [NetworkResult.Error].
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> T
): NetworkResult<T> = withContext(dispatcher) {
    try {
        NetworkResult.Success(block())
    } catch (cancellation: CancellationException) {
        // Không nuốt CancellationException để viewModelScope hủy coroutine đúng cách.
        throw cancellation
    } catch (throwable: Throwable) {
        val httpCode = (throwable as? HttpException)?.code()
        NetworkResult.Error(error = throwable.toNetworkError(), httpCode = httpCode)
    }
}

/** Ánh xạ exception (OkHttp / Retrofit / Gson) sang [NetworkError]. */
fun Throwable.toNetworkError(): NetworkError = when (this) {
    is NoConnectivityException,
    is UnknownHostException,
    is ConnectException -> NetworkError.NO_INTERNET

    is SocketTimeoutException -> NetworkError.TIMEOUT
    is HttpException -> httpCodeToError(code())
    is JsonParseException,
    is NumberFormatException -> NetworkError.PARSE

    is InterruptedIOException -> NetworkError.TIMEOUT
    is IOException -> NetworkError.NO_INTERNET
    else -> NetworkError.UNKNOWN
}

fun httpCodeToError(code: Int): NetworkError = when (code) {
    400 -> NetworkError.BAD_REQUEST
    401, 403 -> NetworkError.UNAUTHORIZED
    404 -> NetworkError.NOT_FOUND
    429 -> NetworkError.TOO_MANY_REQUESTS
    in 500..599 -> NetworkError.SERVER
    else -> NetworkError.UNKNOWN
}
