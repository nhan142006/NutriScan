package vn.edu.vlu.milestone2.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow("Hồ sơ sức khỏe cá nhân")
    val profileState: StateFlow<String> = _profileState.asStateFlow()

    fun loadHealthProfile() {
        _profileState.value = "Hồ sơ: Tiểu đường, Dị ứng đậu phộng"
    }
}