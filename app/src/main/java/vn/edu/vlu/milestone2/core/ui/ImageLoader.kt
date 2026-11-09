package vn.edu.vlu.milestone2.core.ui

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import vn.edu.vlu.milestone2.R

/**
 * Tải ảnh từ URL bằng Glide (bất đồng bộ, ngoài Main Thread) với:
 * - placeholder trong lúc tải,
 * - error fallback khi URL hỏng / mất mạng,
 * - memory cache + disk cache (mặc định của Glide, được bật tường minh ở đây).
 */
fun ImageView.loadRemoteImage(url: String?) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_error)
        .fallback(R.drawable.ic_image_error)
        .skipMemoryCache(false)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .centerCrop()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(this)
}

/** Hủy request khi ViewHolder bị tái sử dụng để tránh hiển thị ảnh sai vị trí. */
fun ImageView.clearRemoteImage() {
    Glide.with(this).clear(this)
}
