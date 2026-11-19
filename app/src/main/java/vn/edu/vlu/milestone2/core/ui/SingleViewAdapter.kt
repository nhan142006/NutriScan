package vn.edu.vlu.milestone2.core.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter 1 item bọc một view đã inflate sẵn (ví dụ phần đầu màn hình), dùng với ConcatAdapter
 * để phần đầu cuộn chung với danh sách mà danh sách vẫn được RecyclerView tái sử dụng view.
 * View được Fragment giữ qua ViewBinding nên dữ liệu được cập nhật trực tiếp, không qua onBind.
 */
class SingleViewAdapter(private val view: View) : RecyclerView.Adapter<SingleViewAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        (view.parent as? ViewGroup)?.removeView(view)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = Unit
}
