package vn.edu.vlu.milestone2.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.repository.ScanRepository
import vn.edu.vlu.milestone2.data.storage.ScanImageStorage
import vn.edu.vlu.milestone2.di.ServiceLocator
import java.io.File

sealed interface CaptureEvent {
    data class Saved(val scanId: Long) : CaptureEvent
    data object Failed : CaptureEvent
}

/**
 * ViewModel không giữ Context: file tạm lấy từ [ScanImageStorage] (tầng data),
 * việc nén + lưu file URI + xếp hàng WorkManager do [ScanRepository] thực hiện.
 */
class LabelCameraViewModel(
    private val scanRepository: ScanRepository,
    private val imageStorage: ScanImageStorage
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = Channel<CaptureEvent>(Channel.BUFFERED)
    val events: Flow<CaptureEvent> = _events.receiveAsFlow()

    fun newCaptureFile(): File = imageStorage.createTempCaptureFile()

    fun onCaptureStarted() {
        _isSaving.value = true
    }

    fun onPhotoCaptured(file: File) {
        viewModelScope.launch {
            val result = scanRepository.createLabelScan(file)
            _isSaving.value = false
            _events.send(result.fold({ CaptureEvent.Saved(it) }, { CaptureEvent.Failed }))
        }
    }

    fun onCaptureFailed(file: File) {
        file.delete()
        _isSaving.value = false
        _events.trySend(CaptureEvent.Failed)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LabelCameraViewModel(ServiceLocator.scanRepository, ServiceLocator.imageStorage) }
        }
    }
}
