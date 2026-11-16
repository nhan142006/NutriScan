package vn.edu.vlu.milestone2.core.ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Thu thập Flow theo vòng đời VIEW của Fragment (viewLifecycleOwner + repeatOnLifecycle(STARTED))
 * → tự dừng khi Fragment vào back stack, tránh memory leak (yêu cầu Milestone 2).
 */
fun <T> Fragment.collectWithLifecycle(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

fun View.showSnackbar(message: CharSequence, long: Boolean = false) {
    Snackbar.make(this, message, if (long) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
}
