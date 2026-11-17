package vn.edu.vlu.milestone2.core.ui

import android.content.Context
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.network.NetworkError

/** Chuyển [NetworkError] thành thông báo tiếng Việt thân thiện. */
fun NetworkError.toUserMessage(context: Context, httpCode: Int? = null): String {
    val message = context.getString(
        when (this) {
            NetworkError.NO_INTERNET -> R.string.error_no_internet
            NetworkError.TIMEOUT -> R.string.error_timeout
            NetworkError.BAD_REQUEST -> R.string.error_bad_request
            NetworkError.UNAUTHORIZED -> R.string.error_unauthorized
            NetworkError.NOT_FOUND -> R.string.error_not_found
            NetworkError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
            NetworkError.SERVER -> R.string.error_server
            NetworkError.PARSE -> R.string.error_parse
            NetworkError.AI_NOT_CONFIGURED -> R.string.error_ai_not_configured
            NetworkError.AI_INVALID_RESPONSE -> R.string.error_ai_invalid
            NetworkError.UNKNOWN -> R.string.error_unknown
        }
    )
    return if (httpCode != null) context.getString(R.string.error_with_http_code, message, httpCode) else message
}

/** Lỗi lưu trong ScanResult.errorMessage là tên enum → đổi sang câu tiếng Việt. */
fun scanErrorMessage(context: Context, raw: String?): String {
    val error = NetworkError.entries.firstOrNull { it.name == raw }
    return error?.toUserMessage(context) ?: raw ?: context.getString(R.string.error_unknown)
}
