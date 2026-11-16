package vn.edu.vlu.milestone2.ui.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.core.ui.clearImage
import vn.edu.vlu.milestone2.core.ui.loadImage
import vn.edu.vlu.milestone2.data.model.ProductSummary
import vn.edu.vlu.milestone2.databinding.ItemProductBinding

/** Danh sách sản phẩm (kết quả tìm kiếm / yêu thích). [onRemove] != null → hiện nút bỏ yêu thích. */
class ProductAdapter(
    private val onClick: (ProductSummary) -> Unit,
    private val onRemove: ((ProductSummary) -> Unit)? = null
) : ListAdapter<ProductSummary, ProductAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        holder.binding.imageProduct.clearImage()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductSummary) {
            val context = binding.root.context
            binding.textName.text = item.name
            binding.textBrand.text = item.brand?.let { context.getString(R.string.product_brand_barcode, it, item.barcode) }
                ?: item.barcode
            binding.chipNutriscore.text = item.nutriScore?.let { context.getString(R.string.nutriscore_label, it) }
                ?: context.getString(R.string.nutriscore_unknown)
            binding.imageProduct.loadImage(item.imageUrl)
            binding.root.setOnClickListener { onClick(item) }
            binding.buttonAction.visibility = if (onRemove != null) View.VISIBLE else View.GONE
            binding.buttonAction.setOnClickListener { onRemove?.invoke(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ProductSummary>() {
        override fun areItemsTheSame(oldItem: ProductSummary, newItem: ProductSummary) = oldItem.barcode == newItem.barcode
        override fun areContentsTheSame(oldItem: ProductSummary, newItem: ProductSummary) = oldItem == newItem
    }
}
