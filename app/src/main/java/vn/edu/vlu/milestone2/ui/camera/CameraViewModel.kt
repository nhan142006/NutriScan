package vn.edu.vlu.milestone2.ui.camera

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {

    private val _cameraState = MutableStateFlow("Sẵn sàng chụp ảnh nhãn thành phần.")
    val cameraState: StateFlow<String> = _cameraState.asStateFlow()

    fun onImageCaptured(imagePath: String) {
        _cameraState.value = "Đã chụp và lưu ảnh tại: $imagePath"
    }
}