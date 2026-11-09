package vn.edu.vlu.milestone2.ui.scanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.clearRemoteImage
import vn.edu.vlu.milestone2.core.ui.loadRemoteImage
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.databinding.ItemProductBinding

/** RecyclerView.Adapter hiển thị kết quả tìm kiếm; ảnh được Glide tải bất đồng bộ. */
class ProductAdapter(
    private val onProductClick: (ProductSummary) -> Unit
) : ListAdapter<ProductSummary, ProductAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ProductViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (ProductSummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductSummary) = with(binding) {
            val context = root.context
            textProductName.text = item.name
            textProductBrand.text = item.brand.orEmpty()
            textProductBrand.visibility = if (item.brand.isNullOrBlank()) View.GONE else View.VISIBLE
            textNutriScore.text = item.nutriScore?.let { context.getString(R.string.nutriscore_label, it) }
                ?: context.getString(R.string.nutriscore_unknown)
            imageProduct.loadRemoteImage(item.imageUrl)
            root.setOnClickListener { onProductClick(item) }
        }

        fun unbind() {
            binding.imageProduct.clearRemoteImage()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductSummary>() {
        override fun areItemsTheSame(oldItem: ProductSummary, newItem: ProductSummary) =
            oldItem.barcode == newItem.barcode

        override fun areContentsTheSame(oldItem: ProductSummary, newItem: ProductSummary) =
            oldItem == newItem
    }
}
