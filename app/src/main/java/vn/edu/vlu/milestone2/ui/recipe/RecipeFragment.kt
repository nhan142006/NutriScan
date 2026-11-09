package vn.edu.vlu.milestone2.ui.recipe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.core.ui.toUserMessage
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.data.model.RecipeDetail
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.databinding.FragmentRecipeRecommendationBinding

class RecipeFragment : Fragment() {

    private var _binding: FragmentRecipeRecommendationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeViewModel by viewModels { RecipeViewModel.Factory }

    private val recipeAdapter = RecipeAdapter { recipe -> viewModel.openRecipe(recipe.id) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerRecipes.adapter = recipeAdapter
        binding.inputRecipeSearch.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEARCH).also { isSearch ->
                if (isSearch) viewModel.search(binding.inputRecipeSearch.text?.toString().orEmpty())
            }
        }
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { group.findViewById<Chip>(it) } ?: return@setOnCheckedStateChangeListener
            viewModel.selectCategory(chip.tag as String)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.categories.collect { renderCategories(it) } }
                launch { viewModel.selectedCategory.collect { renderSelectedCategory(it) } }
                launch { viewModel.recipesState.collect { renderRecipes(it) } }
                launch { viewModel.detailState.collect { renderDetail(it) } }
            }
        }
    }

    private fun renderCategories(categories: List<RecipeCategory>) {
        val group = binding.chipGroupCategories
        val existing = (0 until group.childCount).map { (group.getChildAt(it).tag as String) }
        if (existing == categories.map { it.name }) return

        group.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                tag = category.name
                text = category.name
                isCheckable = true
            }
            group.addView(chip)
        }
        renderSelectedCategory(viewModel.selectedCategory.value)
    }

    private fun renderSelectedCategory(category: String?) {
        val group = binding.chipGroupCategories
        if (category == null) {
            group.clearCheck()
            return
        }
        val chip = (0 until group.childCount)
            .map { group.getChildAt(it) as Chip }
            .firstOrNull { it.tag == category } ?: return
        if (!chip.isChecked) group.check(chip.id)
    }

    private fun renderRecipes(state: UiState<List<RecipeSummary>>) {
        binding.stateView.showState(state, content = binding.recyclerRecipes, onRetry = viewModel::retry)
        val isEmpty = state is UiState.Success && state.data.isEmpty()
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (state is UiState.Success) recipeAdapter.submitList(state.data)
    }

    private fun renderDetail(state: UiState<RecipeDetail>) {
        binding.progressDetail.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
        when (state) {
            is UiState.Success -> {
                showRecipeDialog(state.data)
                viewModel.onDetailConsumed()
            }
            is UiState.Error -> {
                Snackbar.make(
                    binding.root,
                    state.error.toUserMessage(requireContext(), state.httpCode),
                    Snackbar.LENGTH_LONG
                ).show()
                viewModel.onDetailConsumed()
            }
            UiState.Idle, UiState.Loading -> Unit
        }
    }

    private fun showRecipeDialog(detail: RecipeDetail) {
        val meta = listOfNotNull(detail.category, detail.area).joinToString(" · ")
        val message = buildString {
            if (meta.isNotEmpty()) append(meta).append("\n\n")
            append(getString(R.string.recipe_ingredients)).append(":\n")
            detail.ingredients.forEach { append("• ").append(it).append('\n') }
            append('\n').append(getString(R.string.recipe_instructions)).append(":\n")
            append(detail.instructions)
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(detail.name)
            .setMessage(message)
            .setPositiveButton(R.string.recipe_close, null)
        detail.youtubeUrl?.let { url ->
            builder.setNeutralButton(R.string.recipe_watch_video) { _, _ ->
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
        }
        builder.show()
    }

    override fun onDestroyView() {
        binding.recyclerRecipes.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
