package vn.edu.vlu.milestone2.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.databinding.FragmentScanHubBinding
import vn.edu.vlu.milestone2.ui.NavArgs
import vn.edu.vlu.milestone2.ui.product.ProductAdapter

class ScanHubFragment : Fragment() {

    private var _binding: FragmentScanHubBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanHubViewModel by viewModels { ScanHubViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ProductAdapter(onClick = { openProduct(it.barcode) })
        binding.recyclerResults.adapter = adapter

        binding.buttonOpenBarcode.setOnClickListener { findNavController().navigate(R.id.action_scan_hub_to_barcode) }
        binding.buttonOpenLabel.setOnClickListener { findNavController().navigate(R.id.action_scan_hub_to_label_camera) }
        binding.buttonFavorites.setOnClickListener { findNavController().navigate(R.id.favoritesFragment) }
        binding.buttonLookup.setOnClickListener { viewModel.lookupBarcode(binding.editBarcode.text.toString()) }
        binding.editBarcode.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_GO).also { if (it) viewModel.lookupBarcode(binding.editBarcode.text.toString()) }
        }
        binding.layoutSearch.setEndIconOnClickListener { viewModel.search(binding.editSearch.text.toString()) }
        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_SEARCH).also { if (it) viewModel.search(binding.editSearch.text.toString()) }
        }

        collectWithLifecycle(viewModel.barcodeError) { hasError ->
            binding.layoutBarcode.error = if (hasError) getString(R.string.scan_hub_barcode_invalid) else null
        }
        collectWithLifecycle(viewModel.openProduct) { openProduct(it) }
        collectWithLifecycle(viewModel.results) { state ->
            val items = (state as? UiState.Success)?.data.orEmpty()
            binding.networkState.textEmpty.setText(R.string.scan_hub_search_empty)
            binding.networkState.showState(state, binding.recyclerResults, isEmpty = items.isEmpty(), onRetry = viewModel::retry)
            adapter.submitList(items)
        }
    }

    private fun openProduct(barcode: String) {
        findNavController().navigate(R.id.action_scan_hub_to_product_detail, bundleOf(NavArgs.BARCODE to barcode))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
