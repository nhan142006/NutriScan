package vn.edu.vlu.milestone2.ui.profile

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.local.NutriScanDatabase
import vn.edu.vlu.milestone2.data.repository.HealthProfile
import vn.edu.vlu.milestone2.data.repository.HealthProfileRepository
import vn.edu.vlu.milestone2.databinding.FragmentHealthProfileBinding

class HealthProfileFragment : Fragment() {
    private var _binding: FragmentHealthProfileBinding? = null
    private val binding get() = _binding!!
    private var isRendering = false

    private val viewModel: ProfileViewModel by viewModels {
        val dao = NutriScanDatabase.getInstance(requireContext()).healthProfileDao()
        ProfileViewModelFactory(HealthProfileRepository(dao))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.inputFullName.doAfterTextChanged {
            if (!isRendering) viewModel.updateFullName(it?.toString().orEmpty())
        }
        binding.inputAge.doAfterTextChanged {
            if (!isRendering) viewModel.updateAge(it?.toString().orEmpty())
        }
        binding.inputCalories.doAfterTextChanged {
            if (!isRendering) viewModel.updateDailyCalorieTarget(it?.toString().orEmpty())
        }
        binding.inputConditions.doAfterTextChanged {
            if (!isRendering) viewModel.updateConditions(it?.toString().orEmpty())
        }
        binding.buttonSave.setOnClickListener { viewModel.saveProfile() }
        binding.buttonNew.setOnClickListener { viewModel.createNewProfile() }
        binding.buttonDelete.setOnClickListener { viewModel.deleteCurrentProfile() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ProfileUiState) = with(binding) {
        isRendering = true
        try {
            inputFullName.setTextIfDifferent(state.form.fullName)
            inputAge.setTextIfDifferent(state.form.age)
            inputCalories.setTextIfDifferent(state.form.dailyCalorieTarget)
            inputConditions.setTextIfDifferent(state.form.conditions)
        } finally {
            isRendering = false
        }

        textFormTitle.text = getString(
            if (state.isEditing) R.string.profile_edit_title else R.string.profile_create_title
        )
        textStatus.text = state.validationError ?: state.message.orEmpty()
        textStatus.setTextColor(
            requireContext().getColor(
                if (state.validationError != null) R.color.status_error else R.color.status_success
            )
        )
        textStatus.visibility =
            if (state.validationError == null && state.message == null) View.GONE else View.VISIBLE

        buttonSave.isEnabled = !state.isSaving
        buttonNew.isEnabled = !state.isSaving
        buttonDelete.isEnabled = state.isEditing && !state.isSaving
        buttonDelete.visibility = if (state.isEditing) View.VISIBLE else View.GONE

        emptyProfiles.visibility = if (state.profiles.isEmpty()) View.VISIBLE else View.GONE
        renderProfiles(state.profiles, state.form.id)
    }

    private fun renderProfiles(profiles: List<HealthProfile>, selectedId: Long) {
        binding.profileListContainer.removeAllViews()
        profiles.forEach { profile ->
            val conditionSummary = profile.conditions
                .ifEmpty { listOf(getString(R.string.no_conditions)) }
                .joinToString(", ")
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = getString(
                    R.string.profile_list_item,
                    profile.fullName,
                    profile.age,
                    profile.dailyCalorieTarget,
                    conditionSummary
                )
                isAllCaps = false
                isCheckable = true
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                isChecked = profile.id == selectedId
                setOnClickListener { viewModel.editProfile(profile) }
            }
            binding.profileListContainer.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.profile_item_spacing)
                }
            )
        }
    }

    private fun EditText.setTextIfDifferent(value: String) {
        if (text?.toString() != value) {
            setText(value)
            setSelection(value.length)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
