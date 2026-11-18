package vn.edu.vlu.milestone2.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.applyRatingPill
import vn.edu.vlu.milestone2.core.ui.clearImage
import vn.edu.vlu.milestone2.core.ui.loadImage
import vn.edu.vlu.milestone2.core.ui.ratingStyle
import vn.edu.vlu.milestone2.core.util.toRelativeTime
import vn.edu.vlu.milestone2.data.local.entity.ScanSource
import vn.edu.vlu.milestone2.data.model.ScanResult
import vn.edu.vlu.milestone2.databinding.ItemScanHistoryBinding

/** ListAdapter + DiffUtil (không dùng notifyDataSetChanged) – Proposal 6.1. */
class ScanHistoryAdapter(
    private val onClick: (ScanResult) -> Unit,
    private val onLongClick: ((ScanResult) -> Unit)? = null
) : ListAdapter<ScanResult, ScanHistoryAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemScanHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(val binding: ItemScanHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private var boundItem: ScanResult? = null

        init {
            binding.root.setOnClickListener { boundItem?.let(onClick) }
            binding.root.setOnLongClickListener {
                val item = boundItem
                if (item != null && onLongClick != null) {
                    onLongClick.invoke(item)
                    true
                } else false
            }
        }

        fun bind(item: ScanResult) {
            boundItem = item
            val context = binding.root.context
            binding.textName.text = item.productName
            val source = context.getString(
                if (item.source == ScanSource.BARCODE) R.string.scan_source_barcode else R.string.scan_source_label
            )
            binding.textMeta.text = context.getString(R.string.scan_meta, source, item.createdAt.toRelativeTime())
            binding.textRating.applyRatingPill(ratingStyle(item.rating, item.status))
            binding.textNote.visibility = if (item.note.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.textNote.text = item.note

            if (item.imagePath != null) {
                binding.imageThumb.setPadding(0, 0, 0, 0)
                binding.imageThumb.loadImage(item.imagePath)
            } else {
                binding.imageThumb.clearImage()
                val pad = context.resources.getDimensionPixelSize(R.dimen.spacing_medium)
                binding.imageThumb.setPadding(pad, pad, pad, pad)
                binding.imageThumb.setImageResource(
                    if (item.source == ScanSource.BARCODE) R.drawable.ic_barcode else R.drawable.ic_camera
                )
            }
        }

        fun recycle() {
            boundItem = null
            binding.imageThumb.clearImage()
        }
    }

    private object Diff : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult) = oldItem.scanId == newItem.scanId
        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult) = oldItem == newItem
    }
}
