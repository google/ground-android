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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
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
    val view =
      LayoutInflater.from(parent.context).inflate(R.layout.map_type_dialog_item, parent, false)
    return ViewHolder(view)
  }

  /** Binds [MapType] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val itemsViewModel = itemsList[position]
    holder.imageView.setImageResource(itemsViewModel.imageId)
    holder.textView.text = context.getString(itemsViewModel.labelId)
    val color = if (selectedIndex == position) R.color.colorGrey300 else R.color.colorBackground
    holder.itemView.setBackgroundColor(ContextCompat.getColor(context, color))
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
  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.findViewById(R.id.imageview)
    val textView: TextView = view.findViewById(R.id.textView)
  }
}
