package vn.edu.vlu.milestone2.ui.recipe

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.loadImage
import vn.edu.vlu.milestone2.core.ui.clearImage
import vn.edu.vlu.milestone2.core.ui.ratingStyle
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import vn.edu.vlu.milestone2.databinding.FragmentRecipeDetailBinding
import vn.edu.vlu.milestone2.ui.NavArgs

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeDetailViewModel by viewModels { RecipeDetailViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = bundleOf(NavArgs.RECIPE_ID to viewModel.recipeId)
        binding.buttonSave.setOnClickListener { viewModel.toggleSaved() }
        binding.buttonCook.setOnClickListener { findNavController().navigate(R.id.action_recipe_detail_to_cooking, args) }
        binding.buttonEdit.setOnClickListener { findNavController().navigate(R.id.action_recipe_detail_to_edit, args) }
        binding.buttonDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.recipe_delete_title)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
                .show()
        }

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.networkState.showState(state, binding.groupContent, onRetry = viewModel::load)
            binding.buttonCook.isEnabled = state is UiState.Success && state.data.recipe.steps.isNotEmpty()
            if (state is UiState.Success) render(state.data) else hideOptional()
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                is RecipeDetailEvent.SavedChanged ->
                    binding.root.showSnackbar(getString(if (event.saved) R.string.recipe_saved else R.string.recipe_unsaved))
                RecipeDetailEvent.Deleted -> {
                    binding.root.showSnackbar(getString(R.string.recipe_deleted))
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun render(ui: RecipeDetailUi) {
        val recipe = ui.recipe
        val context = requireContext()
        if (recipe.imageUrl != null) {
            binding.imageRecipe.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.imageRecipe.loadImage(recipe.imageUrl)
        } else {
            binding.imageRecipe.scaleType = ImageView.ScaleType.CENTER
            binding.imageRecipe.setImageResource(
                if (recipe.source == RecipeSource.AI) R.drawable.ic_auto_awesome else R.drawable.ic_restaurant
            )
        }
        binding.textTitle.text = recipe.title
        binding.textCategory.text = when (recipe.source) {
            RecipeSource.AI -> getString(R.string.recipes_source_ai)
            RecipeSource.USER -> getString(R.string.recipes_source_user)
            RecipeSource.API -> recipe.category.orEmpty()
        } + if (recipe.basedOnScanIds.isNotEmpty()) {
            " · " + getString(R.string.recipe_based_on_scans, recipe.basedOnScanIds.size)
        } else {
            ""
        }
        binding.buttonSave.setIconResource(if (recipe.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
        binding.buttonSave.contentDescription = getString(if (recipe.isSaved) R.string.recipe_unsave else R.string.recipe_save)

        val compatibility = ui.compatibility
        binding.cardCompatibility.visibility = if (compatibility != null) View.VISIBLE else View.GONE
        if (compatibility != null) {
            val style = ratingStyle(compatibility.rating)
            val fg = ContextCompat.getColor(context, style.foreground)
            binding.cardCompatibility.setCardBackgroundColor(ContextCompat.getColor(context, style.background))
            binding.textCompatibility.setText(
                when (compatibility.rating) {
                    Rating.SUITABLE -> R.string.recipe_compat_suitable
                    Rating.LIMIT -> R.string.recipe_compat_limit
                    Rating.AVOID -> R.string.recipe_compat_avoid
                }
            )
            binding.textCompatibility.setTextColor(fg)
            binding.textCompatibility.setCompoundDrawablesRelativeWithIntrinsicBounds(style.icon, 0, 0, 0)
            binding.textCompatibility.compoundDrawableTintList = ColorStateList.valueOf(fg)
            binding.textCompatibilityReasons.text = compatibility.reasons.joinToString("\n") { "• $it" }
                .ifBlank { getString(R.string.recipe_compat_ok) }
        }

        binding.textFitReason.visibility = if (recipe.fitReason.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.textFitReason.text = recipe.fitReason
        binding.textIngredients.text = recipe.ingredients.joinToString("\n") { "• $it" }
        binding.textSteps.text = recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")

        binding.buttonVideo.visibility = if (recipe.youtubeUrl != null) View.VISIBLE else View.GONE
        binding.buttonVideo.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recipe.youtubeUrl)))
            } catch (error: ActivityNotFoundException) {
                binding.root.showSnackbar(getString(R.string.error_unknown))
            }
        }
        val isUserRecipe = recipe.source == RecipeSource.USER
        binding.buttonEdit.visibility = if (isUserRecipe) View.VISIBLE else View.GONE
        binding.buttonDelete.visibility = if (isUserRecipe) View.VISIBLE else View.GONE
    }

    private fun hideOptional() {
        binding.cardCompatibility.visibility = View.GONE
        binding.textFitReason.visibility = View.GONE
        binding.buttonVideo.visibility = View.GONE
        binding.buttonEdit.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE
    }

    override fun onDestroyView() {
        _binding?.imageRecipe?.clearImage()
        super.onDestroyView()
        _binding = null
    }
}
