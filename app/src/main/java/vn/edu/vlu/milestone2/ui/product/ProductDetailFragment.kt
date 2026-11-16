package vn.edu.vlu.milestone2.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.network.NetworkError
import vn.edu.vlu.milestone2.core.ui.UiState
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.loadImage
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.core.ui.showState
import vn.edu.vlu.milestone2.core.ui.toUserMessage
import vn.edu.vlu.milestone2.core.util.formatNutrient
import vn.edu.vlu.milestone2.data.model.ProductLookup
import vn.edu.vlu.milestone2.databinding.FragmentProductDetailBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** ProductDetailFragment (Module 3.3) – kèm luồng fallback chụp nhãn khi không có trong Open Food Facts. */
class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailViewModel by viewModels { ProductDetailViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonAnalyze.setOnClickListener { viewModel.analyze() }
        binding.buttonRecipes.setOnClickListener { viewModel.findRecipes() }
        binding.buttonFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.buttonTakeLabelPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_product_to_label_camera)
        }

        collectWithLifecycle(viewModel.product) { state ->
            val notFound = state is UiState.Error && state.error == NetworkError.NOT_FOUND
            binding.cardNotFound.visibility = if (notFound) View.VISIBLE else View.GONE
            // NOT_FOUND không phải lỗi mạng → ẩn banner lỗi, hiển thị thẻ fallback chụp nhãn.
            binding.networkState.showState(
                state = if (notFound) UiState.Idle else state,
                content = binding.groupContent,
                onRetry = { viewModel.load(forceRefresh = true) }
            )
            val lookup = (state as? UiState.Success)?.data
            if (lookup != null) render(lookup) else {
                binding.textAllergens.visibility = View.GONE
                binding.textCached.visibility = View.GONE
            }
        }
        collectWithLifecycle(viewModel.isFavorite) { favorite ->
            binding.buttonFavorite.setIconResource(if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            binding.buttonFavorite.contentDescription =
                getString(if (favorite) R.string.favorites_remove else R.string.favorites_add)
        }
        collectWithLifecycle(viewModel.isAnalyzing) { analyzing ->
            binding.progressAnalyze.visibility = if (analyzing) View.VISIBLE else View.GONE
            binding.buttonAnalyze.isEnabled = !analyzing
            binding.buttonRecipes.isEnabled = !analyzing
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                is ProductEvent.OpenAnalysis -> {
                    if (event.aiError != null) {
                        binding.root.showSnackbar(
                            getString(R.string.product_ai_fallback, event.aiError.toUserMessage(requireContext())),
                            long = true
                        )
                    }
                    findNavController().navigate(
                        R.id.action_product_to_analysis,
                        bundleOf(NavArgs.SCAN_ID to event.scanId)
                    )
                }
                is ProductEvent.OpenRecipes -> findNavController().navigate(
                    R.id.action_product_to_recipes,
                    bundleOf(NavArgs.FOCUS_SCAN_ID to event.scanId)
                )
                is ProductEvent.FavoriteChanged -> binding.root.showSnackbar(
                    getString(if (event.added) R.string.favorites_added else R.string.favorites_removed)
                )
            }
        }
    }

    private fun render(lookup: ProductLookup) {
        val product = lookup.product
        val n = product.nutrition
        binding.imageProduct.loadImage(product.imageUrl, centerCrop = false)
        binding.textCached.visibility = if (lookup.fromCache) View.VISIBLE else View.GONE
        binding.textName.text = product.name
        binding.textBrand.text = listOfNotNull(product.brand, product.quantity, product.barcode).joinToString(" · ")
        binding.chipNutriscore.text = product.nutriScore?.let { getString(R.string.nutriscore_label, it) }
            ?: getString(R.string.nutriscore_unknown)
        binding.chipNova.visibility = if (product.novaGroup != null) View.VISIBLE else View.GONE
        product.novaGroup?.let { binding.chipNova.text = getString(R.string.product_nova, it) }

        binding.textEnergy.text = getString(R.string.nutrient_energy, n.energyKcal.formatNutrient())
        binding.textSugars.text = getString(R.string.nutrient_sugars, n.sugars.formatNutrient())
        binding.textFat.text = getString(R.string.nutrient_fat, n.fat.formatNutrient())
        binding.textSatFat.text = getString(R.string.nutrient_sat_fat, n.saturatedFat.formatNutrient())
        binding.textSalt.text = getString(R.string.nutrient_salt, n.salt.formatNutrient())
        binding.textProteins.text = getString(R.string.nutrient_proteins, n.proteins.formatNutrient())
        binding.textCarbs.text = getString(R.string.nutrient_carbs, n.carbohydrates.formatNutrient())
        binding.textFiber.text = getString(R.string.nutrient_fiber, n.fiber.formatNutrient())

        binding.textIngredients.text = product.ingredients ?: getString(R.string.product_ingredients_unknown)
        binding.textAllergens.visibility = if (product.allergens.isEmpty()) View.GONE else View.VISIBLE
        binding.textAllergens.text = getString(R.string.product_allergens, product.allergens.joinToString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
