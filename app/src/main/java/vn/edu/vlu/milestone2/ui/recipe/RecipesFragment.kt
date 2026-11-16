package vn.edu.vlu.milestone2.ui.recipe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.core.ui.toUserMessage
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.databinding.FragmentRecipesBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** RecipeListFragment (Module 3.4): Dành cho bạn · Từ sản phẩm đã quét · Món của tôi · Khám phá. */
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipesViewModel by viewModels { RecipesViewModel.Factory }
    private var renderedCategories: List<RecipeCategory> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderedCategories = emptyList()
        val openRecipe: (RecipeCardItem) -> Unit = { item ->
            findNavController().navigate(R.id.action_recipes_to_detail, bundleOf(NavArgs.RECIPE_ID to item.id))
        }
        val aiAdapter = RecipeCardAdapter(horizontal = true, onClick = openRecipe)
        val matchesAdapter = RecipeCardAdapter(horizontal = true, onClick = openRecipe)
        val myAdapter = RecipeCardAdapter(horizontal = true, onClick = openRecipe)
        val browseAdapter = RecipeCardAdapter(horizontal = false, onClick = openRecipe)
        binding.recyclerAi.adapter = aiAdapter
        binding.recyclerMatches.adapter = matchesAdapter
        binding.recyclerMy.adapter = myAdapter
        binding.recyclerBrowse.adapter = browseAdapter

        binding.buttonSuggest.setOnClickListener { viewModel.refreshForYou() }
        binding.chipFocus.setOnCloseIconClickListener { viewModel.clearFocus() }
        binding.fabAddRecipe.setOnClickListener { findNavController().navigate(R.id.action_recipes_to_add_edit) }
        binding.layoutSearch.setEndIconOnClickListener { viewModel.search(binding.editSearch.text.toString()) }
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEARCH).also { if (it) viewModel.search(binding.editSearch.text.toString()) }
        }
        binding.scrollRecipes.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) binding.fabAddRecipe.shrink() else binding.fabAddRecipe.extend()
        })

        collectWithLifecycle(viewModel.forYou) { state ->
            binding.progressForYou.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.buttonSuggest.isEnabled = !state.isLoading
            binding.textContext.text = state.query?.let { query ->
                val profile = query.profile
                if (profile == null) {
                    getString(R.string.recipes_context_no_profile)
                } else {
                    val summary = (profile.conditions + profile.allergies.map { "dị ứng $it" } + listOf(profile.dietGoal))
                        .filter { it.isNotBlank() }.joinToString().ifBlank { getString(R.string.home_none) }
                    getString(R.string.recipes_context, summary, query.preferredScans.size, query.excludedIngredients.size)
                }
            }
            val focus = state.query?.focusScan
            binding.chipFocus.visibility = if (focus != null) View.VISIBLE else View.GONE
            binding.chipFocus.text = focus?.let { getString(R.string.recipes_focus, it.productName) }
            binding.chipFocus.isCloseIconVisible = true

            aiAdapter.submitList(state.aiRecipes)
            val message = when {
                state.isLoading -> null
                state.showingCachedAi -> getString(R.string.recipes_ai_offline_cache) + " " +
                    state.aiError?.toUserMessage(requireContext()).orEmpty()
                state.aiError != null -> state.aiError.toUserMessage(requireContext())
                state.aiRecipes.isEmpty() -> getString(R.string.recipes_ai_empty)
                else -> null
            }
            binding.textForYouMessage.visibility = if (message != null) View.VISIBLE else View.GONE
            binding.textForYouMessage.text = message

            matchesAdapter.submitList(state.matches)
            val hasMatches = state.matches.isNotEmpty()
            binding.textMatchesTitle.visibility = if (hasMatches) View.VISIBLE else View.GONE
            binding.recyclerMatches.visibility = if (hasMatches) View.VISIBLE else View.GONE
            binding.textMatchesTitle.text = state.query?.mainIngredient
                ?.let { getString(R.string.recipes_matches_title_with, it) }
                ?: getString(R.string.recipes_matches_title)
        }
        collectWithLifecycle(viewModel.myRecipes) { items ->
            myAdapter.submitList(items)
            binding.textMyEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        collectWithLifecycle(viewModel.categories) { categories -> renderCategories(categories) }
        collectWithLifecycle(viewModel.browse) { state ->
            val items = (state as? UiState.Success)?.data.orEmpty()
            browseAdapter.submitList(items)
            binding.networkState.showState(state, binding.recyclerBrowse, isEmpty = items.isEmpty(), onRetry = viewModel::retryBrowse)
        }
    }

    /** Chip danh mục tạo động từ API (categories.php). */
    private fun renderCategories(categories: List<RecipeCategory>) {
        if (categories == renderedCategories) return
        renderedCategories = categories
        val group = binding.chipGroupCategories
        group.removeAllViews()
        categories.forEach { category ->
            group.addView(
                Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    id = View.generateViewId()
                    text = category.name
                    isCheckable = true
                    setOnClickListener { viewModel.selectCategory(category.name) }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
