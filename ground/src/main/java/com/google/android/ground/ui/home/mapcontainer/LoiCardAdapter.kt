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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.home.mapcontainer.LoiCardAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class LoiCardAdapter : RecyclerView.Adapter<ViewHolder>() {

  private var selectedIndex: Int = -1
  private val itemsList: MutableList<LocationOfInterest> = mutableListOf()
  private var callback: ((LocationOfInterest) -> Unit)? = null

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.loi_card_item, parent, false)
    return ViewHolder(view)
  }

  /** Binds [LocationOfInterest] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val locationOfInterest = itemsList[position]
    holder.loiName.text = locationOfInterest.caption ?: "empty caption"
    holder.jobName.text = locationOfInterest.job.name ?: "empty name"
    holder.status.text = "Completed"
    holder.wrapperView.background =
      ResourcesCompat.getDrawable(
        holder.itemView.resources,
        if (selectedIndex == position) {
          R.drawable.border
        } else {
          R.color.colorBackground
        },
        null
      )
    holder.itemView.setOnClickListener { handleItemClicked(position) }
    holder.button.setOnClickListener { handleButtonClicked(position) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  fun updateSelectedPosition(newSelectedIndex: Int) {
    if (newSelectedIndex < 0 || newSelectedIndex >= itemCount || selectedIndex == newSelectedIndex)
      return

    val lastSelectedIndex = selectedIndex
    selectedIndex = newSelectedIndex

    notifyItemChanged(lastSelectedIndex)
    notifyItemChanged(selectedIndex)
  }

  fun updateData(newItemsList: List<LocationOfInterest>) {
    itemsList.clear()
    itemsList.addAll(newItemsList)
    selectedIndex = -1
    notifyDataSetChanged()
  }

  fun setItemClickCallback(callback: (LocationOfInterest) -> Unit) {
    this.callback = callback
  }

  /** Updates the currently selected item. */
  private fun handleItemClicked(position: Int) {
    selectedIndex = position
    notifyDataSetChanged()
  }

  private fun handleButtonClicked(position: Int) {
    callback?.invoke(itemsList[position])
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val status: TextView = view.findViewById(R.id.status)
    val loiName: TextView = view.findViewById(R.id.loiName)
    val jobName: TextView = view.findViewById(R.id.jobName)
    val button: Button = view.findViewById(R.id.button)
    val wrapperView: View = view.findViewById(R.id.wrapper_view)
  }
}
