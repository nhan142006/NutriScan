package vn.edu.vlu.milestone2.ui.recipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.core.ui.clearRemoteImage
import vn.edu.vlu.milestone2.core.ui.loadRemoteImage
import vn.edu.vlu.milestone2.data.model.RecipeSummary
import vn.edu.vlu.milestone2.databinding.ItemRecipeBinding

/** RecyclerView.Adapter dạng lưới; ảnh món ăn được Glide tải bất đồng bộ + cache. */
class RecipeAdapter(
    private val onRecipeClick: (RecipeSummary) -> Unit
) : ListAdapter<RecipeSummary, RecipeAdapter.RecipeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onRecipeClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: RecipeViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    class RecipeViewHolder(
        private val binding: ItemRecipeBinding,
        private val onRecipeClick: (RecipeSummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecipeSummary) = with(binding) {
            textRecipeName.text = item.name
            imageRecipe.loadRemoteImage(item.thumbnailUrl)
            root.setOnClickListener { onRecipeClick(item) }
        }

        fun unbind() {
            binding.imageRecipe.clearRemoteImage()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecipeSummary>() {
        override fun areItemsTheSame(oldItem: RecipeSummary, newItem: RecipeSummary) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RecipeSummary, newItem: RecipeSummary) = oldItem == newItem
    }
}
