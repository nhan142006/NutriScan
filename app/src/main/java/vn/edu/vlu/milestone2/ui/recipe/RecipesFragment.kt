package vn.edu.vlu.milestone2.ui.recipe

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.SingleViewAdapter
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.core.ui.toUserMessage
import vn.edu.vlu.milestone2.data.model.RecipeCategory
import vn.edu.vlu.milestone2.databinding.FragmentRecipesBinding
import vn.edu.vlu.milestone2.databinding.LayoutRecipesHeaderBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** RecipeListFragment (Module 3.4): Dành cho bạn · Từ sản phẩm đã quét · Món của tôi · Khám phá. */
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private var _header: LayoutRecipesHeaderBinding? = null
    private val header get() = _header!!

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

        // Phần đầu là item 0 của RecyclerView chính; lưới "Khám phá" là các item sau và được tái
        // sử dụng view khi cuộn (trước đây cả lưới nằm trong NestedScrollView nên bị đo/vẽ toàn bộ).
        val grid = GridLayoutManager(requireContext(), BROWSE_SPAN).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) BROWSE_SPAN else 1
            }
        }
        binding.recyclerRecipes.layoutManager = grid
        binding.recyclerRecipes.setHasFixedSize(true)
        binding.recyclerRecipes.addItemDecoration(
            BrowseGridInset(resources.getDimensionPixelSize(R.dimen.spacing_medium))
        )
        // LayoutManager phải gắn trước khi inflate để header nhận GridLayoutManager.LayoutParams.
        _header = LayoutRecipesHeaderBinding.inflate(layoutInflater, binding.recyclerRecipes, false)
        listOf(header.recyclerAi to aiAdapter, header.recyclerMatches to matchesAdapter, header.recyclerMy to myAdapter)
            .forEach { (list, adapter) ->
                list.adapter = adapter
                (list.layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = NESTED_PREFETCH
            }
        binding.recyclerRecipes.adapter = ConcatAdapter(SingleViewAdapter(header.root), browseAdapter)

        header.buttonSuggest.setOnClickListener { viewModel.refreshForYou() }
        header.chipFocus.setOnCloseIconClickListener { viewModel.clearFocus() }
        binding.fabAddRecipe.setOnClickListener { findNavController().navigate(R.id.action_recipes_to_add_edit) }
        header.layoutSearch.setEndIconOnClickListener { viewModel.search(header.editSearch.text.toString()) }
        header.editSearch.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEARCH).also { if (it) viewModel.search(header.editSearch.text.toString()) }
        }
        binding.recyclerRecipes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val fab = binding.fabAddRecipe
                if (dy > 0 && fab.isExtended) fab.shrink() else if (dy < 0 && !fab.isExtended) fab.extend()
            }
        })

        collectWithLifecycle(viewModel.forYou) { state ->
            header.progressForYou.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            header.buttonSuggest.isEnabled = !state.isLoading
            header.textContext.text = state.query?.let { query ->
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
            header.chipFocus.visibility = if (focus != null) View.VISIBLE else View.GONE
            header.chipFocus.text = focus?.let { getString(R.string.recipes_focus, it.productName) }
            header.chipFocus.isCloseIconVisible = true

            aiAdapter.submitList(state.aiRecipes)
            val message = when {
                state.isLoading -> null
                state.showingCachedAi -> getString(R.string.recipes_ai_offline_cache) + " " +
                    state.aiError?.toUserMessage(requireContext()).orEmpty()
                state.aiError != null -> state.aiError.toUserMessage(requireContext())
                state.aiRecipes.isEmpty() -> getString(R.string.recipes_ai_empty)
                else -> null
            }
            header.textForYouMessage.visibility = if (message != null) View.VISIBLE else View.GONE
            header.textForYouMessage.text = message

            matchesAdapter.submitList(state.matches)
            val hasMatches = state.matches.isNotEmpty()
            header.textMatchesTitle.visibility = if (hasMatches) View.VISIBLE else View.GONE
            header.recyclerMatches.visibility = if (hasMatches) View.VISIBLE else View.GONE
            header.textMatchesTitle.text = state.query?.mainIngredient
                ?.let { getString(R.string.recipes_matches_title_with, it) }
                ?: getString(R.string.recipes_matches_title)
        }
        collectWithLifecycle(viewModel.myRecipes) { items ->
            myAdapter.submitList(items)
            header.textMyEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        collectWithLifecycle(viewModel.categories) { categories -> renderCategories(categories) }
        collectWithLifecycle(viewModel.browse) { state ->
            val items = (state as? UiState.Success)?.data.orEmpty()
            browseAdapter.submitList(items)
            header.networkState.showState(state, null, isEmpty = items.isEmpty(), onRetry = viewModel::retryBrowse)
        }
    }

    /** Chip danh mục tạo động từ API (categories.php). */
    private fun renderCategories(categories: List<RecipeCategory>) {
        if (categories == renderedCategories) return
        renderedCategories = categories
        val group = header.chipGroupCategories
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
        _header?.let {
            it.recyclerAi.adapter = null
            it.recyclerMatches.adapter = null
            it.recyclerMy.adapter = null
        }
        _binding?.let {
            it.recyclerRecipes.clearOnScrollListeners()
            it.recyclerRecipes.adapter = null
        }
        renderedCategories = emptyList()
        super.onDestroyView()
        _header = null
        _binding = null
    }

    /** Lề 2 bên cho lưới "Khám phá" (trước đây là paddingHorizontal của RecyclerView lồng). */
    private class BrowseGridInset(private val inset: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val params = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
            if (params.spanSize == BROWSE_SPAN) return
            if (params.spanIndex == 0) outRect.left = inset else outRect.right = inset
        }
    }

    private companion object {
        const val BROWSE_SPAN = 2
        const val NESTED_PREFETCH = 3
    }
}
