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
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.MapTypeDialogItemBinding
import com.google.android.ground.ui.home.mapcontainer.MapTypeAdapter.ViewHolder
import com.google.android.ground.ui.map.MapType

/**
 * An implementation of [RecyclerView.Adapter] that associates [MapType] data with the [ViewHolder]
 * views.
 */
class MapTypeAdapter(
  private val context: Context,
  private val itemsList: Array<MapType>,
  private var selectedIndex: Int,
  private val callback: (Int) -> Unit
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
    holder.binding.imageview.setImageResource(itemsViewModel.imageId)
    holder.binding.textView.text = context.getString(itemsViewModel.labelId)
    val textColor =
      if (selectedIndex == position) {
        R.color.colorAccent
      } else {
        R.color.colorForeground
      }
    holder.binding.textView.setTextColor(context.resources.getColor(textColor, null))
    val borderDrawable =
      if (selectedIndex == position) {
        R.drawable.map_type_item_selected_background
      } else {
        R.drawable.map_type_item_default_background
      }
    holder.binding.container.background =
      ResourcesCompat.getDrawable(context.resources, borderDrawable, null)
    holder.itemView.setOnClickListener { handleItemClicked(holder.adapterPosition) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  /** Updates the currently selected item and invokes the attached [callback]. */
  private fun handleItemClicked(position: Int) {
    callback.invoke(position)
    selectedIndex = position
    notifyDataSetChanged()
  }

  /** View item representing the [MapType] data in the list. */
  class ViewHolder(internal val binding: MapTypeDialogItemBinding) :
    RecyclerView.ViewHolder(binding.root)
}
