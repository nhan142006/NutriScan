package vn.edu.vlu.milestone2.ui.scanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.databinding.FragmentBarcodeScannerBinding
import vn.edu.vlu.milestone2.ui.analysis.AnalysisFragment
import vn.edu.vlu.milestone2.ui.detail.DetailViewModel

class ScannerFragment : Fragment() {

    private var _binding: FragmentBarcodeScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScannerViewModel by viewModels { ScannerViewModel.Factory }

    private val productAdapter = ProductAdapter(::openAnalysis)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerProducts.adapter = productAdapter

        binding.buttonLookup.setOnClickListener { lookupBarcode() }
        binding.buttonOpenDetail.setOnClickListener {
            readBarcode()?.let { barcode ->
                findNavController().navigate(
                    R.id.action_scanner_to_detail,
                    bundleOf(DetailViewModel.ARG_BARCODE to barcode)
                )
            }
        }
        binding.inputBarcode.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_GO).also { if (it) lookupBarcode() }
        }
        binding.buttonSearch.setOnClickListener { submitSearch() }
        binding.inputSearch.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEARCH).also { if (it) submitSearch() }
        }

        viewModel.loadInitial(getString(R.string.scanner_default_query))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchState.collect(::renderSearch)
            }
        }
    }

    private fun renderSearch(state: UiState<List<ProductSummary>>) {
        binding.stateView.showState(state, content = binding.recyclerProducts, onRetry = viewModel::retry)
        val isEmpty = state is UiState.Success && state.data.isEmpty()
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (state is UiState.Success) productAdapter.submitList(state.data)
    }

    private fun lookupBarcode() {
        readBarcode()?.let(::navigateToAnalysis)
    }

    private fun readBarcode(): String? {
        val barcode = binding.inputBarcode.text?.toString().orEmpty().trim()
        if (!ScannerViewModel.isValidBarcode(barcode)) {
            binding.layoutBarcode.error = getString(R.string.scanner_barcode_invalid)
            return null
        }
        binding.layoutBarcode.error = null
        return barcode
    }

    private fun submitSearch() {
        val query = binding.inputSearch.text?.toString().orEmpty()
        if (query.isBlank()) {
            binding.layoutSearch.error = getString(R.string.scanner_search_empty)
            return
        }
        binding.layoutSearch.error = null
        viewModel.search(query)
    }

    private fun openAnalysis(product: ProductSummary) = navigateToAnalysis(product.barcode)

    private fun navigateToAnalysis(barcode: String) {
        findNavController().navigate(
            R.id.action_scanner_to_analysis,
            bundleOf(AnalysisFragment.ARG_BARCODE to barcode)
        )
    }

    override fun onDestroyView() {
        binding.recyclerProducts.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
