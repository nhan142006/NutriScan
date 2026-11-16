package vn.edu.vlu.milestone2.ui.recipe

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.databinding.ItemIngredientCheckBinding

data class IngredientCheckItem(val index: Int, val text: String, val checked: Boolean)

class IngredientCheckAdapter(
    private val onToggle: (Int, Boolean) -> Unit
) : ListAdapter<IngredientCheckItem, IngredientCheckAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemIngredientCheckBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemIngredientCheckBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IngredientCheckItem) {
            binding.checkIngredient.setOnCheckedChangeListener(null)
            binding.checkIngredient.text = item.text
            binding.checkIngredient.isChecked = item.checked
            binding.checkIngredient.paintFlags = if (item.checked) {
                binding.checkIngredient.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.checkIngredient.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.checkIngredient.setOnCheckedChangeListener { _, isChecked -> onToggle(item.index, isChecked) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<IngredientCheckItem>() {
        override fun areItemsTheSame(oldItem: IngredientCheckItem, newItem: IngredientCheckItem) = oldItem.index == newItem.index
        override fun areContentsTheSame(oldItem: IngredientCheckItem, newItem: IngredientCheckItem) = oldItem == newItem
    }
}
