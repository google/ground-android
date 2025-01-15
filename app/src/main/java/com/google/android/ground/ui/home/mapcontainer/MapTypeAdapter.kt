/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.ground.ui.home.mapcontainer

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTypeDialogItemBinding
import com.google.android.ground.ui.home.mapcontainer.MapTypeAdapter.ViewHolder
import com.google.android.ground.ui.map.MapType
import com.google.android.material.color.MaterialColors

/**
 * An implementation of [RecyclerView.Adapter] that associates [MapType] data with the [ViewHolder]
 * views.
 */
class MapTypeAdapter(
  private val context: Context,
  private val itemsList: List<MapType>,
  private var selectedIndex: Int,
  private val callback: (Int) -> Unit,
) : RecyclerView.Adapter<ViewHolder>() {

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
      MapTypeDialogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [MapType] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val itemsViewModel = itemsList[position]
    holder.binding.imageView.setImageResource(itemsViewModel.imageId)
    holder.binding.textView.text = context.getString(itemsViewModel.labelId)

    val isItemSelected = selectedIndex == position
    holder.binding.textView.setTextColor(
      MaterialColors.getColor(
        holder.binding.textView,
        if (isItemSelected) {
          R.attr.colorPrimary
        } else {
          R.attr.colorOnSurface
        },
      )
    )
    holder.binding.card.apply {
      strokeWidth = if (isItemSelected) 5 else 0
      elevation = if (isItemSelected) 7.0f else 0.0f
    }
    holder.itemView.setOnClickListener { handleItemClicked(holder.adapterPosition) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  /** Updates the currently selected item and invokes the attached [callback]. */
  private fun handleItemClicked(position: Int) {
    if (position == -1) {
      return
    }
    callback.invoke(position)
    selectedIndex = position
    notifyDataSetChanged()
  }

  /** View item representing the [MapType] data in the list. */
  class ViewHolder(internal val binding: MapTypeDialogItemBinding) :
    RecyclerView.ViewHolder(binding.root)
}
