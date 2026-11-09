package vn.edu.vlu.milestone2.core.network

/**
 * Loại lỗi mạng đã được chuẩn hóa để tầng UI hiển thị thông báo thân thiện,
 * thay vì để exception thô làm crash ứng dụng.
 */
enum class NetworkError {
    /** Không có Internet / đang bật Chế độ máy bay / không phân giải được DNS. */
    NO_INTERNET,

    /** Kết nối hoặc đọc dữ liệu quá thời gian cho phép. */
    TIMEOUT,

    /** HTTP 400 - yêu cầu không hợp lệ. */
    BAD_REQUEST,

    /** HTTP 401 / 403 - không có quyền truy cập. */
    UNAUTHORIZED,

    /** HTTP 404 - không tìm thấy tài nguyên (ví dụ: mã vạch không tồn tại). */
    NOT_FOUND,

    /** HTTP 429 - gọi API quá nhiều lần. */
    TOO_MANY_REQUESTS,

    /** HTTP 5xx - máy chủ gặp sự cố. */
    SERVER,

    /** JSON trả về sai định dạng, không parse được. */
    PARSE,

    /** Các lỗi khác. */
    UNKNOWN
}
