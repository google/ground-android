package com.google.android.ground.ui.datacollection

import androidx.recyclerview.widget.RecyclerView

/**
 * Abstract class extending RecyclerView.Adapter, handling the selection states of items selected in
 * the RecyclerView.
 */
abstract class SelectionAdapter<V : RecyclerView.ViewHolder> : RecyclerView.Adapter<V>() {
  abstract fun getPosition(key: Long): Int

  abstract fun handleItemStateChanged(position: Int, selected: Boolean)
}
