package vn.edu.vlu.milestone2.ui.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Chống trùng: ML Kit trả về cùng 1 mã nhiều lần/giây → chỉ phát sự kiện điều hướng 1 lần. */
class BarcodeScannerViewModel : ViewModel() {

    private var handled = false

    private val _detected = Channel<String>(Channel.CONFLATED)
    val detected: Flow<String> = _detected.receiveAsFlow()

    fun onBarcodeDetected(value: String) {
        if (handled) return
        handled = true
        _detected.trySend(value)
    }
}
