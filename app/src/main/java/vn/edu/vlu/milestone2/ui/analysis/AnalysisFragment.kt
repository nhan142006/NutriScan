package vn.edu.vlu.milestone2.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.loadRemoteImage
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.data.model.Nutrition
import vn.edu.vlu.milestone2.databinding.FragmentAnalysisResultBinding
import vn.edu.vlu.milestone2.domain.Verdict
import vn.edu.vlu.milestone2.ui.detail.DetailViewModel
import java.util.Locale

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalysisViewModel by viewModels { AnalysisViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonGoScanner.setOnClickListener {
            findNavController().navigate(R.id.scannerFragment)
        }

        val barcode = arguments?.getString(ARG_BARCODE).orEmpty()
        viewModel.analyze(barcode)
        binding.buttonOpenDetail.setOnClickListener {
            findNavController().navigate(
                R.id.action_analysis_to_detail,
                bundleOf(DetailViewModel.ARG_BARCODE to barcode)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.analysisState.collect(::render)
            }
        }
    }

    private fun render(state: UiState<ProductAnalysis>) = with(binding) {
        layoutIdle.visibility = if (state is UiState.Idle) View.VISIBLE else View.GONE
        stateView.showState(state, content = layoutContent, onRetry = viewModel::retry)
        if (state is UiState.Success) bindAnalysis(state.data)
    }

    private fun bindAnalysis(analysis: ProductAnalysis) = with(binding) {
        val product = analysis.product
        imageProduct.loadRemoteImage(product.imageUrl)
        textProductName.text = product.name
        textProductMeta.text = listOfNotNull(product.brand, product.quantity).joinToString(" · ")
        textNutriScore.text = product.nutriScore?.let { getString(R.string.nutriscore_label, it) }
            ?: getString(R.string.nutriscore_unknown)
        textBarcode.text = getString(R.string.analysis_barcode, product.barcode)

        val (verdictText, verdictColor, verdictBackground) = when (analysis.result.verdict) {
            Verdict.SAFE -> Triple(R.string.analysis_verdict_safe, R.color.status_success, R.color.verdict_safe_bg)
            Verdict.CAUTION -> Triple(R.string.analysis_verdict_caution, R.color.status_warning, R.color.verdict_caution_bg)
            Verdict.AVOID -> Triple(R.string.analysis_verdict_avoid, R.color.status_error, R.color.verdict_avoid_bg)
        }
        textVerdict.setText(verdictText)
        textVerdict.setTextColor(requireContext().getColor(verdictColor))
        cardVerdict.setCardBackgroundColor(requireContext().getColor(verdictBackground))

        textProfileUsed.text = analysis.profile?.let { getString(R.string.analysis_profile_used, it.fullName) }
            ?: getString(R.string.analysis_no_profile)
        textWarnings.text = analysis.result.warnings
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "• ${it.message}" }
            ?: getString(R.string.analysis_no_warnings)

        textNutrition.text = product.nutrition.toDisplayText()
        textAllergens.visibility = if (product.allergens.isEmpty()) View.GONE else View.VISIBLE
        textAllergens.text = getString(R.string.analysis_allergens, product.allergens.joinToString(", "))
        textIngredients.text = product.ingredients ?: getString(R.string.value_not_available)
    }

    private fun Nutrition.toDisplayText(): String {
        fun Double?.show(): String = this?.let { String.format(Locale.US, "%.1f", it) }
            ?: getString(R.string.value_not_available)
        return getString(
            R.string.analysis_nutrition_table,
            energyKcal.show(),
            sugars.show(),
            fat.show(),
            saturatedFat.show(),
            salt.show(),
            proteins.show(),
            carbohydrates.show(),
            fiber.show()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Trùng với tên <argument> trong nav_graph.xml. */
        const val ARG_BARCODE = "barcode"
    }
}
