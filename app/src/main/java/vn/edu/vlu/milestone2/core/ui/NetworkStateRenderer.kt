package vn.edu.vlu.milestone2.core.ui

import android.view.View
import vn.edu.vlu.milestone2.databinding.ViewNetworkStateBinding

/**
 * Hiển thị đúng 1 trong 3 trạng thái cho khối include `view_network_state.xml`.
 * [content] là view chứa dữ liệu thật (thường là RecyclerView).
 */
fun ViewNetworkStateBinding.showState(
    state: UiState<*>,
    content: View,
    isEmpty: Boolean = false,
    onRetry: (() -> Unit)? = null
) {
    val context = root.context
    progressBar.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
    errorBanner.visibility = if (state is UiState.Error) View.VISIBLE else View.GONE
    content.visibility = if (state is UiState.Success<*> && !isEmpty) View.VISIBLE else View.GONE
    textEmpty.visibility = if (state is UiState.Success<*> && isEmpty) View.VISIBLE else View.GONE

    if (state is UiState.Error) {
        textErrorMessage.text = state.error.toUserMessage(context, state.httpCode)
        buttonRetry.visibility = if (onRetry != null) View.VISIBLE else View.GONE
        buttonRetry.setOnClickListener { onRetry?.invoke() }
    }
}
