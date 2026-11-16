package vn.edu.vlu.milestone2.core.util

import android.text.format.DateUtils
import java.util.Locale

fun Long.toRelativeTime(): CharSequence =
    DateUtils.getRelativeTimeSpanString(this, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)

fun Double?.formatNutrient(): String = when {
    this == null -> "—"
    this % 1.0 == 0.0 -> toInt().toString()
    else -> String.format(Locale.US, "%.1f", this)
}
