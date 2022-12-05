package com.google.android.ground.ui.datacollection

import androidx.recyclerview.widget.RecyclerView

abstract class SelectionAdapter<V : RecyclerView.ViewHolder> : RecyclerView.Adapter<V>() {
  abstract fun getPosition(key: Long): Int

  abstract fun handleItemStateChanged(position: Int, selected: Boolean)
}