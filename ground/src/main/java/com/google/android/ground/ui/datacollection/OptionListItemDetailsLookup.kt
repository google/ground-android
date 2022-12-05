package com.google.android.ground.ui.datacollection

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.widget.RecyclerView

/** Provides [ItemDetails] for the Multiple Choice option items. */
class OptionListItemDetailsLookup(
  val recyclerView: RecyclerView,
) : ItemDetailsLookup<Long>() {
  override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
    val view: View = recyclerView.findChildViewUnder(e.x, e.y) ?: return null

    val holder: RecyclerView.ViewHolder = recyclerView.getChildViewHolder(view)

    val position = holder.adapterPosition
    return object : ItemDetails<Long>() {
      override fun getPosition(): Int = position

      override fun getSelectionKey(): Long = position.toLong()
    }
  }
}
