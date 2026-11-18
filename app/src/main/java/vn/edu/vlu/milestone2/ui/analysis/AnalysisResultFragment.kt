package vn.edu.vlu.milestone2.ui.analysis

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.collectWithLifecycle
import vn.edu.vlu.milestone2.core.ui.loadImage
import vn.edu.vlu.milestone2.core.ui.ratingStyle
import vn.edu.vlu.milestone2.core.ui.scanErrorMessage
import vn.edu.vlu.milestone2.core.ui.showSnackbar
import vn.edu.vlu.milestone2.core.util.toRelativeTime
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.databinding.FragmentAnalysisResultBinding
import vn.edu.vlu.milestone2.ui.NavArgs

/** AnalysisResultFragment (Module 3.2): rating, thành phần bị gắn cờ, ghi chú, "Nấu món với sản phẩm này". */
class AnalysisResultFragment : Fragment() {

    private var _binding: FragmentAnalysisResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalysisResultViewModel by viewModels { AnalysisResultViewModel.Factory }
    private var noteInitialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalysisResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteInitialized = false
        val flaggedAdapter = FlaggedIngredientAdapter()
        binding.recyclerFlagged.adapter = flaggedAdapter

        binding.buttonRetry.setOnClickListener { viewModel.retry() }
        binding.layoutNote.setEndIconOnClickListener { viewModel.saveNote(binding.editNote.text.toString()) }
        binding.editNote.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { if (it) viewModel.saveNote(binding.editNote.text.toString()) }
        }
        binding.buttonFindRecipes.setOnClickListener {
            findNavController().navigate(
                R.id.action_analysis_to_recipes,
                bundleOf(NavArgs.FOCUS_SCAN_ID to viewModel.scanId)
            )
        }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        collectWithLifecycle(viewModel.uiState) { state ->
            val scan = state.scan
            if (!state.isLoading && scan == null) {
                binding.textProductName.setText(R.string.analysis_not_found)
                return@collectWithLifecycle
            }
            if (scan != null) render(scan, state.isOnline, flaggedAdapter)
        }
        collectWithLifecycle(viewModel.events) { event ->
            when (event) {
                AnalysisEvent.NoteSaved -> binding.root.showSnackbar(getString(R.string.analysis_note_saved))
                AnalysisEvent.Deleted -> {
                    binding.root.showSnackbar(getString(R.string.analysis_deleted))
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun render(scan: ScanResult, isOnline: Boolean, adapter: FlaggedIngredientAdapter) {
        val context = requireContext()
        val style = ratingStyle(scan.rating, scan.status)
        val fg = ContextCompat.getColor(context, style.foreground)

        binding.textProductName.text = scan.productName
        val source = getString(if (scan.source == ScanSource.BARCODE) R.string.scan_source_barcode else R.string.scan_source_label)
        binding.textMeta.text = getString(R.string.scan_meta, source, scan.createdAt.toRelativeTime())
        if (scan.imagePath != null) {
            binding.imageLabel.loadImage(scan.imagePath)
        } else {
            binding.imageLabel.setImageResource(R.drawable.ic_barcode)
        }

        binding.cardRating.setCardBackgroundColor(ContextCompat.getColor(context, style.background))
        binding.imageRating.setImageResource(style.icon)
        ImageViewCompat.setImageTintList(binding.imageRating, ColorStateList.valueOf(fg))
        binding.textRating.setText(style.label)
        binding.textRating.setTextColor(fg)

        val isDone = scan.status == ScanStatus.DONE
        binding.progressPending.visibility = if (scan.status == ScanStatus.PENDING) View.VISIBLE else View.GONE
        binding.buttonRetry.visibility = if (scan.status == ScanStatus.FAILED) View.VISIBLE else View.GONE
        binding.textExplanation.text = when (scan.status) {
            ScanStatus.DONE -> scan.analysis?.explanation
            ScanStatus.PENDING -> getString(if (isOnline) R.string.analysis_pending_online else R.string.analysis_pending_offline)
            ScanStatus.FAILED -> scanErrorMessage(context, scan.errorMessage)
        }

        val flagged = scan.analysis?.flaggedIngredients.orEmpty()
        adapter.submitList(flagged)
        binding.textFlaggedTitle.visibility = if (isDone) View.VISIBLE else View.GONE
        binding.textNoFlags.visibility = if (isDone && flagged.isEmpty()) View.VISIBLE else View.GONE

        binding.textIngredientsTitle.visibility = if (scan.ingredients.isEmpty()) View.GONE else View.VISIBLE
        binding.chipGroupIngredients.removeAllViews()
        scan.ingredients.forEach { ingredient ->
            binding.chipGroupIngredients.addView(
                Chip(context).apply {
                    text = ingredient
                    isClickable = false
                    isCheckable = false
                }
            )
        }

        if (!noteInitialized) {
            binding.editNote.setText(scan.note.orEmpty())
            noteInitialized = true
        }
        binding.buttonFindRecipes.isEnabled = isDone
        binding.buttonViewProduct.visibility = if (scan.productBarcode != null) View.VISIBLE else View.INVISIBLE
        binding.buttonViewProduct.setOnClickListener {
            scan.productBarcode?.let { barcode ->
                findNavController().navigate(R.id.action_analysis_to_product_detail, bundleOf(NavArgs.BARCODE to barcode))
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.analysis_delete_title)
            .setMessage(R.string.analysis_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .show()
    }

    override fun onDestroyView() {
        _binding?.recyclerFlagged?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}
