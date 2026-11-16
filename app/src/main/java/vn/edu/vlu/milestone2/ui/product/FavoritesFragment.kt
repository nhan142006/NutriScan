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
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.databinding.FragmentFavoritesBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** FavoritesFragment (RecyclerView + DiffUtil) – dữ liệu từ Room, xem được khi offline. */
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels { FavoritesViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ProductAdapter(
            onClick = { product ->
                findNavController().navigate(
                    R.id.action_favorites_to_product_detail,
                    bundleOf(NavArgs.BARCODE to product.barcode)
                )
            },
            onRemove = { product ->
                viewModel.remove(product.barcode)
                binding.root.showSnackbar(getString(R.string.favorites_removed))
            }
        )
        binding.recyclerFavorites.adapter = adapter

        // observe(viewLifecycleOwner) – lifecycle-aware theo yêu cầu Milestone 2.
        viewModel.favorites.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
