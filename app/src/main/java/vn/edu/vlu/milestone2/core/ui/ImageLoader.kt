package vn.edu.vlu.milestone2.core.ui

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import vn.edu.vlu.milestone2.R

/**
 * Tải ảnh bằng Glide (bất đồng bộ) với placeholder, error fallback, memory + disk cache.
 * [source] có thể là URL https hoặc file URI (ảnh nhãn đã nén trong filesDir).
 */
fun ImageView.loadImage(source: String?, centerCrop: Boolean = true) {
    var request = Glide.with(this)
        .load(source)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_error)
        .fallback(R.drawable.ic_image_placeholder)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .transition(DrawableTransitionOptions.withCrossFade())
    if (centerCrop) request = request.centerCrop()
    request.into(this)
}

/** Hủy request khi ViewHolder bị tái sử dụng để tránh hiển thị ảnh sai vị trí. */
fun ImageView.clearImage() {
    Glide.with(this).clear(this)
}
