package vn.edu.vlu.milestone2.ui.recipe

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import vn.edu.vlu.milestone2.data.local.entity.RecipeSource
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
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
    val source: RecipeSource = RecipeSource.API
)

internal class RecipeCardAdapter(
    private val translations: RecipeTranslation,
    private val scope: CoroutineScope,
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
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root) {
        private var titleJob: Job? = null
        private var boundItem: RecipeCardItem? = null
        private var remoteImageBound = false
        @DrawableRes private var iconRes = 0

        init { binding.root.setOnClickListener { boundItem?.let(onClick) } }

        fun bind(item: RecipeCardItem) {
            boundItem = item
            binding.textTitle.text = item.title
            titleJob?.cancel()
            titleJob = scope.launch {
                val title = translations.title(item)
                if (boundItem === item) binding.textTitle.text = title
            }
            binding.textSubtitle.text = item.subtitle
            binding.textSubtitle.visibility = if (item.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            if (item.imageUrl != null) {
                binding.imageRecipe.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.imageRecipe.loadImage(item.imageUrl)
                remoteImageBound = true
                iconRes = 0
            } else {
                // Món không có ảnh (món tự tạo / AI): chỉ hủy Glide khi holder từng tải ảnh mạng,
                // và chỉ đặt lại icon khi đổi loại icon, để giảm việc trên main thread khi cuộn.
                val icon = if (item.source == RecipeSource.AI) R.drawable.ic_auto_awesome else R.drawable.ic_restaurant
                if (remoteImageBound) {
                    binding.imageRecipe.clearImage()
                    remoteImageBound = false
                }
                if (iconRes != icon) {
                    binding.imageRecipe.scaleType = ImageView.ScaleType.CENTER
                    binding.imageRecipe.setImageResource(icon)
                    iconRes = icon
                }
            }
            binding.textBadge.visibility = if (item.badge != null) View.VISIBLE else View.GONE
            item.badge?.let { binding.textBadge.applyRatingPill(ratingStyle(it)) }
        }

        fun recycle() {
            titleJob?.cancel()
            boundItem = null
            if (remoteImageBound) {
                binding.imageRecipe.clearImage()
                remoteImageBound = false
                iconRes = 0
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<RecipeCardItem>() {
        override fun areItemsTheSame(oldItem: RecipeCardItem, newItem: RecipeCardItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecipeCardItem, newItem: RecipeCardItem) = oldItem == newItem
    }
}
