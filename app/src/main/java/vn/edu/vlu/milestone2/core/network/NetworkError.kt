package vn.edu.vlu.milestone2.core.network

/**
 * Loại lỗi đã chuẩn hóa để tầng UI hiển thị thông báo thân thiện,
 * thay vì để exception thô làm crash ứng dụng.
 */
enum class NetworkError {
    NO_INTERNET,
    TIMEOUT,
    BAD_REQUEST,
    UNAUTHORIZED,
    NOT_FOUND,
    TOO_MANY_REQUESTS,
    SERVER,
    PARSE,
    /** Chưa khai báo GEMINI_API_KEY trong local.properties. */
    AI_NOT_CONFIGURED,
    /** Gemini chặn nội dung hoặc trả JSON không khớp responseSchema. */
    AI_INVALID_RESPONSE,
    UNKNOWN
}
