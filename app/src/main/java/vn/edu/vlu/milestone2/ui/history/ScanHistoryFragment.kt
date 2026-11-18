package vn.edu.vlu.milestone2.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.databinding.FragmentScanHistoryBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** ScanHistoryFragment: lọc theo đánh giá, vuốt để xóa, nhấn giữ để "Nấu món với sản phẩm này". */
class ScanHistoryFragment : Fragment() {

    private var _binding: FragmentScanHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScanHistoryViewModel by viewModels { ScanHistoryViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ScanHistoryAdapter(
            onClick = { scan ->
                findNavController().navigate(R.id.action_history_to_analysis, bundleOf(NavArgs.SCAN_ID to scan.scanId))
            },
            onLongClick = { scan ->
                findNavController().navigate(R.id.action_history_to_recipes, bundleOf(NavArgs.FOCUS_SCAN_ID to scan.scanId))
            }
        )
        binding.recyclerHistory.adapter = adapter
        attachSwipeToDelete(adapter)

        binding.buttonFavorites.setOnClickListener { findNavController().navigate(R.id.action_history_to_favorites) }
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setFilter(
                when (checkedIds.firstOrNull()) {
                    R.id.chip_suitable -> HistoryFilter.SUITABLE
                    R.id.chip_limit -> HistoryFilter.LIMIT
                    R.id.chip_avoid -> HistoryFilter.AVOID
                    R.id.chip_pending -> HistoryFilter.PENDING
                    else -> HistoryFilter.ALL
                }
            )
        }

        collectWithLifecycle(viewModel.uiState) { state ->
            adapter.submitList(state.items)
            binding.textEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
            binding.textEmpty.setText(if (state.totalCount == 0) R.string.history_empty else R.string.history_empty_filter)
            binding.textCount.text = resources.getQuantityString(R.plurals.history_count, state.totalCount, state.totalCount)
        }
    }

    private fun attachSwipeToDelete(adapter: ScanHistoryAdapter) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position) ?: return
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.analysis_delete_title)
                    .setMessage(R.string.analysis_delete_message)
                    .setNegativeButton(R.string.action_cancel) { _, _ -> adapter.notifyItemChanged(position) }
                    .setOnCancelListener { adapter.notifyItemChanged(position) }
                    .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(item.scanId) }
                    .show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerHistory)
    }

    override fun onDestroyView() {
        _binding?.recyclerHistory?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
