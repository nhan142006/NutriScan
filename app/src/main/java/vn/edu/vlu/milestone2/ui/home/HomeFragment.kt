package vn.edu.vlu.milestone2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.navigateToTab
import vn.edu.vlu.milestone2.databinding.FragmentHomeBinding
import vn.edu.vlu.milestone2.ui.NavArgs
import vn.edu.vlu.milestone2.ui.history.ScanHistoryAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ScanHistoryAdapter(onClick = { scan ->
            findNavController().navigate(R.id.action_home_to_analysis, bundleOf(NavArgs.SCAN_ID to scan.scanId))
        })
        binding.recyclerRecent.adapter = adapter

        binding.cardScanBarcode.setOnClickListener { findNavController().navigate(R.id.action_home_to_barcode) }
        binding.cardScanLabel.setOnClickListener { findNavController().navigate(R.id.action_home_to_label_camera) }
        binding.cardProfile.setOnClickListener { navigateToTab(R.id.profileFragment) }
        binding.buttonSeeAll.setOnClickListener { navigateToTab(R.id.historyFragment) }

        collectWithLifecycle(viewModel.uiState) { state ->
            binding.textGreeting.text = state.displayName?.let { getString(R.string.home_greeting, it) }
                ?: getString(R.string.home_greeting_default)

            val profile = state.profile
            binding.textProfileSummary.text = if (profile == null) {
                getString(R.string.home_profile_missing)
            } else {
                val none = getString(R.string.home_none)
                getString(
                    R.string.home_profile_summary,
                    profile.conditions.joinToString().ifBlank { none },
                    profile.allergies.joinToString().ifBlank { none },
                    profile.dietGoal.ifBlank { none }
                )
            }

            binding.cardPending.visibility = if (state.pendingCount > 0) View.VISIBLE else View.GONE
            binding.textPending.text = resources.getQuantityString(R.plurals.home_pending, state.pendingCount, state.pendingCount)
            binding.textAiStatus.setText(if (state.isAiConfigured) R.string.home_ai_ready else R.string.home_ai_missing)

            adapter.submitList(state.recentScans)
            binding.textRecentEmpty.visibility = if (state.recentScans.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
