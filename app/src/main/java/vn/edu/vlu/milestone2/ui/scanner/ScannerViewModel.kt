package vn.edu.vlu.milestone2.ui.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScannerViewModel : ViewModel() {

    private val _scanResultState = MutableStateFlow("Đang chờ quét mã vạch...")
    val scanResultState: StateFlow<String> = _scanResultState.asStateFlow()

    fun onBarcodeScanned(barcode: String) {
        // Xử lý khi quét được mã vạch
        _scanResultState.value = "Đã quét thành công mã: $barcode"
    }
}