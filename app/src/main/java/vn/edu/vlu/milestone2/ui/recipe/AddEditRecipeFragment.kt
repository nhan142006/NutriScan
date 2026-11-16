package vn.edu.vlu.milestone2.ui.recipe

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.ratingStyle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentAddEditRecipeBinding

class AddEditRecipeFragment : Fragment() {

    private var _binding: FragmentAddEditRecipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditRecipeViewModel by viewModels { AddEditRecipeViewModel.Factory }
    private var formFilled = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        formFilled = savedInstanceState != null
        binding.editIngredients.doAfterTextChanged { viewModel.onIngredientsChanged(it.toString()) }
        binding.buttonSave.setOnClickListener {
            viewModel.save(
                title = binding.editTitle.text.toString(),
                category = binding.editCategory.text.toString(),
                ingredientsText = binding.editIngredients.text.toString(),
                stepsText = binding.editSteps.text.toString()
            )
        }

        collectWithLifecycle(viewModel.existing) { recipe ->
            if (recipe != null && !formFilled) {
                formFilled = true
                binding.editTitle.setText(recipe.title)
                binding.editCategory.setText(recipe.category.orEmpty())
                binding.editIngredients.setText(recipe.ingredients.joinToString("\n"))
                binding.editSteps.setText(recipe.steps.joinToString("\n"))
            }
        }
        collectWithLifecycle(viewModel.errors) { errors ->
            binding.layoutTitle.error = errors.title?.let { getString(it) }
            binding.layoutIngredients.error = errors.ingredients?.let { getString(it) }
            binding.layoutSteps.error = errors.steps?.let { getString(it) }
        }
        collectWithLifecycle(viewModel.compatibility) { compatibility ->
            binding.textCompatibility.visibility = if (compatibility == null) View.GONE else View.VISIBLE
            if (compatibility != null) {
                val style = ratingStyle(compatibility.rating)
                val context = requireContext()
                binding.textCompatibility.text = listOf(getString(style.label))
                    .plus(compatibility.reasons).joinToString(" · ")
                binding.textCompatibility.setTextColor(ContextCompat.getColor(context, style.foreground))
                binding.textCompatibility.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, style.background))
            }
        }
        collectWithLifecycle(viewModel.saved) {
            binding.root.showSnackbar(getString(R.string.add_recipe_saved))
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
