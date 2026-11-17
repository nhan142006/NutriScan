package vn.edu.vlu.milestone2.ui.recipe

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
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.databinding.ItemRecipeBinding

/** Dữ liệu hiển thị của 1 thẻ món ăn (dùng chung cho AI / TheMealDB / Món của tôi). */
data class RecipeCardItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val subtitle: String?,
    val badge: Rating?,
    val isAi: Boolean = false
)

class RecipeCardAdapter(
    private val horizontal: Boolean,
    private val onClick: (RecipeCardItem) -> Unit
) : ListAdapter<RecipeCardItem, RecipeCardAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (horizontal) {
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = parent.resources.getDimensionPixelSize(R.dimen.recipe_card_width)
            }
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        holder.binding.imageRecipe.clearImage()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecipeCardItem) {
            binding.textTitle.text = item.title
            binding.textSubtitle.text = item.subtitle
            binding.textSubtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            if (item.imageUrl != null) {
                binding.imageRecipe.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                binding.imageRecipe.loadImage(item.imageUrl)
            } else {
                binding.imageRecipe.clearImage()
                binding.imageRecipe.scaleType = android.widget.ImageView.ScaleType.CENTER
                binding.imageRecipe.setImageResource(if (item.isAi) R.drawable.ic_auto_awesome else R.drawable.ic_restaurant)
            }
            binding.textBadge.visibility = if (item.badge != null) View.VISIBLE else View.GONE
            item.badge?.let { binding.textBadge.applyRatingPill(ratingStyle(it)) }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RecipeCardItem>() {
        override fun areItemsTheSame(oldItem: RecipeCardItem, newItem: RecipeCardItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecipeCardItem, newItem: RecipeCardItem) = oldItem == newItem
    }
}
