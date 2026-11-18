package vn.edu.vlu.milestone2.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentCookingModeBinding

/** Cooking Mode (Proposal 3.4 – CookingModeActivity được hiện thực dưới dạng Fragment trong NavGraph). */
class CookingModeFragment : Fragment() {

    private var _binding: FragmentCookingModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CookingViewModel by viewModels { CookingViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCookingModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = IngredientCheckAdapter(viewModel::toggleIngredient)
        binding.recyclerChecklist.adapter = adapter
        binding.buttonPrevious.setOnClickListener { viewModel.previous() }
        binding.buttonNext.setOnClickListener {
            if (viewModel.uiState.value.isLastStep) {
                binding.root.showSnackbar(getString(R.string.cooking_done))
                findNavController().navigateUp()
            } else {
                viewModel.next()
            }
        }

        collectWithLifecycle(viewModel.uiState) { state ->
            val recipe = state.recipe ?: return@collectWithLifecycle
            binding.textRecipeTitle.text = recipe.title
            val total = state.totalSteps
            if (total == 0) {
                binding.textStep.setText(R.string.cooking_no_steps)
                binding.textStepCounter.text = null
                binding.buttonNext.isEnabled = false
            } else {
                binding.textStep.text = recipe.steps[state.stepIndex]
                binding.textStepCounter.text = getString(R.string.cooking_step_counter, state.stepIndex + 1, total)
                binding.progressSteps.max = total
                binding.progressSteps.setProgressCompat(state.stepIndex + 1, true)
                binding.buttonNext.isEnabled = true
            }
            binding.buttonPrevious.isEnabled = state.stepIndex > 0
            binding.buttonNext.setText(if (state.isLastStep) R.string.cooking_finish else R.string.cooking_next)

            adapter.submitList(recipe.ingredients.mapIndexed { index, text -> IngredientCheckItem(index, text, index in state.checked) })
            binding.textChecklistTitle.text = getString(R.string.cooking_checklist, state.checked.size, recipe.ingredients.size)
        }
    }

    override fun onDestroyView() {
        _binding?.recyclerChecklist?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
