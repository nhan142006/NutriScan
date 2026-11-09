package vn.edu.vlu.milestone2.core.network

import java.io.IOException

/** Ném ra bởi [ConnectivityInterceptor] khi thiết bị không có mạng (ví dụ: Chế độ máy bay). */
class NoConnectivityException : IOException("No network connection available")
