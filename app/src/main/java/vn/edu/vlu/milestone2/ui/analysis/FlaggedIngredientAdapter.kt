package vn.edu.vlu.milestone2.ui.analysis

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.model.FlaggedIngredient
import vn.edu.vlu.milestone2.data.model.Severity
import vn.edu.vlu.milestone2.databinding.ItemFlaggedIngredientBinding

class FlaggedIngredientAdapter : ListAdapter<FlaggedIngredient, FlaggedIngredientAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemFlaggedIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(private val binding: ItemFlaggedIngredientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FlaggedIngredient) {
            val context = binding.root.context
            val (label, fg, bg) = when (item.severity) {
                Severity.LOW -> Triple(R.string.severity_low, R.color.rating_pending, R.color.rating_pending_bg)
                Severity.MEDIUM -> Triple(R.string.severity_medium, R.color.rating_limit, R.color.rating_limit_bg)
                Severity.HIGH -> Triple(R.string.severity_high, R.color.rating_avoid, R.color.rating_avoid_bg)
            }
            val fgColor = ContextCompat.getColor(context, fg)
            binding.textName.text = item.name
            binding.textReason.text = item.reason
            binding.textSeverity.setText(label)
            binding.textSeverity.setTextColor(fgColor)
            binding.textSeverity.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bg))
            ImageViewCompat.setImageTintList(binding.imageSeverity, ColorStateList.valueOf(fgColor))
        }
    }

    private object Diff : DiffUtil.ItemCallback<FlaggedIngredient>() {
        override fun areItemsTheSame(oldItem: FlaggedIngredient, newItem: FlaggedIngredient) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: FlaggedIngredient, newItem: FlaggedIngredient) = oldItem == newItem
    }
}
