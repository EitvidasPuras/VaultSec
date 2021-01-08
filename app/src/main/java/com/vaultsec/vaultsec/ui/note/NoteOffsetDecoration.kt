package com.vaultsec.vaultsec.ui.note

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class NoteOffsetDecoration(private val space: Int, private val spanCount: Int) :
    RecyclerView.ItemDecoration() {
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

        if (spanIndex == 0) {
            outRect.left = space
        } else {
            outRect.right = space
            outRect.left = space
        }
        outRect.bottom = space * 2


//        if ((parent.getChildLayoutPosition(view) % spanCount) == 0) {
//            outRect.left = space
//        } else if ((parent.getChildLayoutPosition(view) % spanCount) == 1) {
//            outRect.right = space
//
//        }
    }
}