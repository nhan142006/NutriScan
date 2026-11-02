package vn.edu.vlu.milestone2.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.repository.HealthProfile
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository

data class ProfileFormState(
    val id: Long = 0,
    val fullName: String = "",
    val age: String = "",
    val dailyCalorieTarget: String = "2000",
    val conditions: String = ""
)

data class ProfileUiState(
    val profiles: List<HealthProfile> = emptyList(),
    val form: ProfileFormState = ProfileFormState(),
    val validationError: String? = null,
    val message: String? = null,
    val isSaving: Boolean = false
) {
    val isEditing: Boolean get() = form.id != 0L
}

class ProfileViewModel(
    private val repository: HealthProfileRepository
) : ViewModel() {
    private val form = MutableStateFlow(ProfileFormState())
    private val validationError = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeProfiles(),
        form,
        validationError,
        message,
        isSaving
    ) { profiles, currentForm, currentError, currentMessage, saving ->
        ProfileUiState(
            profiles = profiles,
            form = currentForm,
            validationError = currentError,
            message = currentMessage,
            isSaving = saving
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun updateFullName(value: String) = updateForm { copy(fullName = value) }

    fun updateAge(value: String) = updateForm { copy(age = value) }

    fun updateDailyCalorieTarget(value: String) =
        updateForm { copy(dailyCalorieTarget = value) }

    fun updateConditions(value: String) = updateForm { copy(conditions = value) }

    fun editProfile(profile: HealthProfile) {
        form.value = ProfileFormState(
            id = profile.id,
            fullName = profile.fullName,
            age = profile.age.toString(),
            dailyCalorieTarget = profile.dailyCalorieTarget.toString(),
            conditions = profile.conditions.joinToString(", ")
        )
        validationError.value = null
        message.value = null
    }

    fun createNewProfile() {
        form.value = ProfileFormState()
        validationError.value = null
        message.value = null
    }

    fun saveProfile() {
        val currentForm = form.value
        val name = currentForm.fullName.trim()
        val age = currentForm.age.toIntOrNull()
        val calorieTarget = currentForm.dailyCalorieTarget.toIntOrNull()

        val error = when {
            name.isEmpty() -> "Vui lòng nhập họ và tên."
            age == null || age !in 1..120 -> "Tuổi phải là số từ 1 đến 120."
            calorieTarget == null || calorieTarget !in 500..10_000 ->
                "Mục tiêu năng lượng phải từ 500 đến 10.000 kcal."
            else -> null
        }
        if (error != null) {
            validationError.value = error
            message.value = null
            return
        }
        val validatedAge = requireNotNull(age)
        val validatedCalorieTarget = requireNotNull(calorieTarget)

        viewModelScope.launch {
            isSaving.value = true
            validationError.value = null
            runCatching {
                repository.saveProfile(
                    id = currentForm.id,
                    fullName = name,
                    age = validatedAge,
                    dailyCalorieTarget = validatedCalorieTarget,
                    conditions = currentForm.conditions.split(',', ';', '\n')
                )
            }.onSuccess {
                form.value = ProfileFormState()
                message.value = if (currentForm.id == 0L) {
                    "Đã lưu hồ sơ vào bộ nhớ offline."
                } else {
                    "Đã cập nhật hồ sơ offline."
                }
            }.onFailure { throwable ->
                validationError.value = throwable.message ?: "Không thể lưu hồ sơ."
            }
            isSaving.value = false
        }
    }

    fun deleteCurrentProfile() {
        val profileId = form.value.id
        if (profileId == 0L) return

        viewModelScope.launch {
            isSaving.value = true
            runCatching { repository.deleteProfile(profileId) }
                .onSuccess { deleted ->
                    if (deleted) {
                        form.value = ProfileFormState()
                        validationError.value = null
                        message.value = "Đã xóa hồ sơ và các tình trạng liên quan."
                    } else {
                        validationError.value = "Hồ sơ không còn tồn tại."
                    }
                }
                .onFailure { throwable ->
                    validationError.value = throwable.message ?: "Không thể xóa hồ sơ."
                }
            isSaving.value = false
        }
    }

    private inline fun updateForm(transform: ProfileFormState.() -> ProfileFormState) {
        form.value = form.value.transform()
        validationError.value = null
        message.value = null
    }
}

class ProfileViewModelFactory(
    private val repository: HealthProfileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            "ViewModel không được hỗ trợ: ${modelClass.name}"
        }
        return ProfileViewModel(repository) as T
    }
}
