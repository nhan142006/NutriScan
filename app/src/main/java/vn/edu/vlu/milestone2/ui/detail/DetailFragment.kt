package vn.edu.vlu.milestone2.ui.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.loadRemoteImage
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.data.model.Product
import vn.edu.vlu.milestone2.databinding.FragmentProductDetailBinding

class DetailFragment : Fragment(R.layout.fragment_product_detail) {

    private val viewModel: DetailViewModel by viewModels { DetailViewModel.Factory }
    private var deleteDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentProductDetailBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.productState.collect { renderProduct(binding, it) } }
                launch { viewModel.savedState.collect { renderSavedState(binding, it) } }
            }
        }
    }

    private fun renderProduct(binding: FragmentProductDetailBinding, state: UiState<Product>) = with(binding) {
        productState.showState(state, productContent, viewModel::loadProduct)
        if (state is UiState.Success) {
            val product = state.data
            imageProduct.loadRemoteImage(product.imageUrl)
            textProductName.text = product.name
            textBarcode.text = getString(R.string.analysis_barcode, product.barcode)
            textProductMeta.text = listOfNotNull(product.brand, product.quantity).joinToString(" · ")
            textNutriScore.text = product.nutriScore?.let { getString(R.string.nutriscore_label, it) }
                ?: getString(R.string.nutriscore_unknown)
            textIngredients.text = getString(
                R.string.detail_ingredients,
                product.ingredients ?: getString(R.string.value_not_available)
            )
        }
    }

    private fun renderSavedState(binding: FragmentProductDetailBinding, state: UiState<Boolean>) = with(binding) {
        savedState.showState(state, buttonSavedProduct, viewModel::retrySavedRequest)
        if (state is UiState.Success) {
            val saved = state.data
            buttonSavedProduct.setText(if (saved) R.string.detail_delete else R.string.detail_save)
            buttonSavedProduct.setOnClickListener {
                if (saved) confirmDelete() else viewModel.save()
            }
        }
    }

    private fun confirmDelete() {
        if (deleteDialog?.isShowing == true) return
        deleteDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_delete)
            .setMessage(R.string.detail_delete_confirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.detail_delete_confirm) { _, _ -> viewModel.delete() }
            .show()
    }

    override fun onDestroyView() {
        deleteDialog?.dismiss()
        deleteDialog = null
        super.onDestroyView()
    }
}
