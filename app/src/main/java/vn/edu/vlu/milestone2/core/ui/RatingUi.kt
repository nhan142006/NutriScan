package vn.edu.vlu.milestone2.core.ui

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import vn.edu.vlu.milestone2.R
import vn.edu.vlu.milestone2.data.local.entity.Rating
import vn.edu.vlu.milestone2.data.local.entity.ScanStatus

/** Bộ màu/nhãn/biểu tượng cho 1 đánh giá – dùng chung mọi màn hình. */
data class RatingStyle(
    @StringRes val label: Int,
    @ColorRes val foreground: Int,
    @ColorRes val background: Int,
    @DrawableRes val icon: Int
)

fun ratingStyle(rating: Rating?, status: ScanStatus = ScanStatus.DONE): RatingStyle = when {
    status == ScanStatus.PENDING -> RatingStyle(R.string.status_pending, R.color.rating_pending, R.color.rating_pending_bg, R.drawable.ic_schedule)
    status == ScanStatus.FAILED -> RatingStyle(R.string.status_failed, R.color.rating_avoid, R.color.rating_avoid_bg, R.drawable.ic_warning)
    rating == Rating.SUITABLE -> RatingStyle(R.string.rating_suitable, R.color.rating_suitable, R.color.rating_suitable_bg, R.drawable.ic_check_circle)
    rating == Rating.LIMIT -> RatingStyle(R.string.rating_limit, R.color.rating_limit, R.color.rating_limit_bg, R.drawable.ic_warning)
    rating == Rating.AVOID -> RatingStyle(R.string.rating_avoid, R.color.rating_avoid, R.color.rating_avoid_bg, R.drawable.ic_block)
    else -> RatingStyle(R.string.status_pending, R.color.rating_pending, R.color.rating_pending_bg, R.drawable.ic_schedule)
}

/** Tô 1 TextView dạng "pill" (nền bo tròn bg_rating_pill) theo đánh giá. */
fun TextView.applyRatingPill(style: RatingStyle) {
    val ctx = this.context
    setText(style.label)
    setTextColor(ContextCompat.getColor(ctx, style.foreground))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, style.background))
    setCompoundDrawablesRelativeWithIntrinsicBounds(style.icon, 0, 0, 0)
    compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, style.foreground))
}
