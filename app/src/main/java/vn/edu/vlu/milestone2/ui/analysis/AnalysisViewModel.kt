package vn.edu.vlu.milestone2.ui.analysis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnalysisViewModel : ViewModel() {

    private val _analysisState = MutableStateFlow("Sản phẩm an toàn cho người tiểu đường.")
    val analysisState: StateFlow<String> = _analysisState.asStateFlow()

    private val _warningState = MutableStateFlow("Lưu ý: Chứa hàm lượng đường nhỏ.")
    val warningState: StateFlow<String> = _warningState.asStateFlow()

    fun performAnalysis() {
        // Sau này nhóm sẽ gắn logic gọi API hoặc thuật toán AI phân tích tại đây
        _analysisState.value = "Phân tích thành công: Sản phẩm phù hợp với hồ sơ sức khỏe."
        _warningState.value = "Cảnh báo: Không phát hiện đậu phộng (An toàn cho dị ứng)."
    }
}