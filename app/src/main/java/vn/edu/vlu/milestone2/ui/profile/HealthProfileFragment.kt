package vn.edu.vlu.milestone2.ui.profile

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.data.model.HealthProfile
import vn.edu.vlu.milestone2.databinding.FragmentHealthProfileBinding

/** HealthProfileFragment (Module 3.1): Create / Read / Update / Delete hồ sơ sức khỏe. */
class HealthProfileFragment : Fragment() {

    private var _binding: FragmentHealthProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels { ProfileViewModel.Factory }
    private var fieldsFilled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHealthProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fieldsFilled = savedInstanceState?.getBoolean(KEY_FIELDS_FILLED) ?: false
        binding.layoutCustomCondition.setEndIconOnClickListener {
            viewModel.addCustomCondition(binding.editCustomCondition.text.toString())
            binding.editCustomCondition.text?.clear()
        }
        binding.layoutCustomAllergy.setEndIconOnClickListener {
            viewModel.addCustomAllergy(binding.editCustomAllergy.text.toString())
            binding.editCustomAllergy.text?.clear()
        }
        binding.buttonSave.setOnClickListener {
            viewModel.save(
                name = binding.editName.text.toString(),
                ageText = binding.editAge.text.toString(),
                calorieText = binding.editCalories.text.toString(),
                dietGoal = binding.autoDietGoal.text.toString()
            )
        }
        binding.buttonDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_delete_title)
                .setMessage(R.string.profile_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
                .show()
        }
        binding.buttonLogout.setOnClickListener { viewModel.signOut() }

        collectWithLifecycle(viewModel.accountEmail) { email ->
            binding.textAccount.text = email
        }
        collectWithLifecycle(viewModel.profile) { state ->
            binding.buttonSave.isEnabled = !state.isLoading && !viewModel.form.value.isSaving
            binding.editName.isEnabled = !state.isLoading
            binding.editAge.isEnabled = !state.isLoading
            binding.editCalories.isEnabled = !state.isLoading
            binding.autoDietGoal.isEnabled = !state.isLoading
            if (!state.isLoading) renderProfile(state.profile)
        }
        collectWithLifecycle(viewModel.form) { form ->
            if (binding.autoDietGoal.adapter == null) {
                binding.autoDietGoal.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, form.dietGoalOptions)
                )
            }
            binding.layoutName.error = form.nameError?.let { getString(it) }
            binding.layoutAge.error = form.ageError?.let { getString(it) }
            binding.layoutCalories.error = form.calorieError?.let { getString(it) }
            binding.buttonSave.isEnabled = !form.isSaving && !viewModel.profile.value.isLoading
            renderChips(binding.chipGroupConditions, form.conditionOptions, form.conditions, viewModel::toggleCondition)
            renderChips(binding.chipGroupAllergies, form.allergyOptions, form.allergies, viewModel::toggleAllergy)
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                ProfileEvent.Saved -> binding.root.showSnackbar(getString(R.string.profile_saved))
                ProfileEvent.Deleted -> {
                    fieldsFilled = false
                    binding.editName.text?.clear()
                    binding.editAge.text?.clear()
                    binding.editCalories.setText(viewModel.form.value.defaultCalorieTarget.toString())
                    binding.autoDietGoal.setText(viewModel.form.value.defaultDietGoal, false)
                    binding.root.showSnackbar(getString(R.string.profile_deleted))
                }
            }
        }
    }

    private fun renderProfile(profile: HealthProfile?) {
        binding.buttonDelete.visibility = if (profile != null) View.VISIBLE else View.GONE
        binding.textUpdated.text = profile?.let {
            getString(R.string.profile_updated_at, DateUtils.getRelativeTimeSpanString(it.updatedAt))
        } ?: getString(R.string.profile_not_created)
        binding.buttonSave.setText(if (profile == null) R.string.profile_create else R.string.profile_update)
        if (fieldsFilled) return
        fieldsFilled = true
        binding.editName.setText(profile?.fullName.orEmpty())
        binding.editAge.setText(profile?.age?.toString().orEmpty())
        binding.editCalories.setText((profile?.dailyCalorieTarget ?: viewModel.form.value.defaultCalorieTarget).toString())
        binding.autoDietGoal.setText(profile?.dietGoal?.ifBlank { null } ?: viewModel.form.value.defaultDietGoal, false)
    }

    /** Dựng lại chip chỉ khi danh sách thay đổi; trạng thái chọn lấy từ ViewModel (nguồn sự thật). */
    private fun renderChips(
        group: ChipGroup,
        options: List<String>,
        selected: Set<String>,
        onToggle: (String, Boolean) -> Unit
    ) {
        val current = (0 until group.childCount).map { (group.getChildAt(it) as Chip).text.toString() }
        if (current != options) {
            group.removeAllViews()
            options.forEach { option ->
                group.addView(
                    Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                        id = View.generateViewId()
                        text = option
                        isCheckable = true
                    }
                )
            }
        }
        for (index in 0 until group.childCount) {
            val chip = group.getChildAt(index) as Chip
            chip.setOnCheckedChangeListener(null)
            chip.isChecked = chip.text.toString() in selected
            chip.setOnCheckedChangeListener { _, isChecked -> onToggle(chip.text.toString(), isChecked) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FIELDS_FILLED, fieldsFilled)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val KEY_FIELDS_FILLED = "profile_fields_filled"
    }
}
