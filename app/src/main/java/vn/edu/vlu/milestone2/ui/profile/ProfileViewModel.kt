package vn.edu.vlu.milestone2.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.data.repository.AuthRepository
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.di.ServiceLocator

data class ProfileFormState(
    val conditions: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    @StringRes val nameError: Int? = null,
    @StringRes val ageError: Int? = null,
    @StringRes val calorieError: Int? = null,
    val isSaving: Boolean = false
) {
    /** Chip hiển thị = danh sách gợi ý sẵn + mục người dùng tự thêm. */
    val conditionOptions: List<String> get() = (PRESET_CONDITIONS + conditions).distinct()
    val allergyOptions: List<String> get() = (PRESET_ALLERGIES + allergies).distinct()
    val dietGoalOptions: List<String> get() = DIET_GOALS
    val defaultDietGoal: String get() = dietGoalOptions.first()
    val defaultCalorieTarget: Int get() = DEFAULT_CALORIE_TARGET

    companion object {
        private const val DEFAULT_CALORIE_TARGET = 2000
        val PRESET_CONDITIONS = listOf("Tiểu đường", "Cao huyết áp", "Mỡ máu cao", "Bệnh thận", "Gout")
        val PRESET_ALLERGIES = listOf("Đậu phộng", "Sữa / Lactose", "Gluten", "Hải sản", "Trứng", "Đậu nành", "Hạt", "Mè")
        val DIET_GOALS = listOf("Giữ cân", "Giảm cân", "Tăng cơ", "Ăn chay", "Ít đường", "Ít muối")
    }
}

/** Không dùng null để đại diện đồng thời cho đang tải và chưa có hồ sơ. */
data class ProfileUiState(
    val profile: HealthProfile? = null,
    val isLoading: Boolean = true
)

sealed interface ProfileEvent {
    data object Saved : ProfileEvent
    data object Deleted : ProfileEvent
}

/** ProfileViewModel – CRUD Hồ sơ sức khỏe (Room + đồng bộ Firestore). */
class ProfileViewModel(
    private val profileRepository: HealthProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _form = MutableStateFlow(ProfileFormState())
    val form: StateFlow<ProfileFormState> = _form.asStateFlow()

    val profile: StateFlow<ProfileUiState> = profileRepository.observeCurrentProfile()
        .map { saved ->
            onProfileLoaded(saved)
            ProfileUiState(profile = saved, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    val accountEmail: StateFlow<String?> = authRepository.observeAuthState()
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), authRepository.currentUser?.email)

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    private var loadedFromDb = false

    /** Nạp lựa chọn từ hồ sơ đã lưu đúng 1 lần (không ghi đè khi người dùng đang sửa). */
    private fun onProfileLoaded(profile: HealthProfile?) {
        if (loadedFromDb || profile == null) return
        loadedFromDb = true
        _form.update { it.copy(conditions = profile.conditions.toSet(), allergies = profile.allergies.toSet()) }
    }

    fun toggleCondition(name: String, checked: Boolean) = _form.update {
        it.copy(conditions = if (checked) it.conditions + name else it.conditions - name)
    }

    fun toggleAllergy(name: String, checked: Boolean) = _form.update {
        it.copy(allergies = if (checked) it.allergies + name else it.allergies - name)
    }

    fun addCustomCondition(text: String) {
        val value = text.trim()
        if (value.isNotEmpty()) toggleCondition(value, true)
    }

    fun addCustomAllergy(text: String) {
        val value = text.trim()
        if (value.isNotEmpty()) toggleAllergy(value, true)
    }

    fun save(name: String, ageText: String, calorieText: String, dietGoal: String) {
        val age = ageText.trim().toIntOrNull()
        val calories = calorieText.trim().toIntOrNull()
        val nameError = if (name.isBlank()) R.string.profile_error_name else null
        val ageError = if (age == null || age !in 1..120) R.string.profile_error_age else null
        val calorieError = if (calories == null || calories !in 800..5000) R.string.profile_error_calories else null
        _form.update { it.copy(nameError = nameError, ageError = ageError, calorieError = calorieError) }
        if (nameError != null || ageError != null || calorieError != null) return

        val state = _form.value
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            profileRepository.saveProfile(
                fullName = name,
                age = age!!,
                dietGoal = dietGoal,
                dailyCalorieTarget = calories!!,
                conditions = state.conditions.toList(),
                allergies = state.allergies.toList()
            )
            _form.update { it.copy(isSaving = false) }
            _events.send(ProfileEvent.Saved)
        }
    }

    fun delete() {
        viewModelScope.launch {
            profileRepository.deleteProfile()
            loadedFromDb = false
            _form.value = ProfileFormState()
            _events.send(ProfileEvent.Deleted)
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ProfileViewModel(ServiceLocator.healthProfileRepository, ServiceLocator.authRepository) }
        }
    }
}
