package com.vaultsec.vaultsec.ui.password

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class PasswordOffsetDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildLayoutPosition(view)
        val lp: StaggeredGridLayoutManager.LayoutParams =
            view.layoutParams as StaggeredGridLayoutManager.LayoutParams
        val spanIndex = lp.spanIndex

        if (position == 0 || position == 1) {
            outRect.top = space + 8
        }
        if (spanIndex == 0) {
            outRect.left = space
        } else {
            outRect.right = space
            outRect.left = space
        }
        outRect.bottom = space
    }
}