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
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.LoiCardItemBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.home.mapcontainer.LoiCardAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class LoiCardAdapter : RecyclerView.Adapter<ViewHolder>() {

  private var selectedIndex: Int = -1
  private val itemsList: MutableList<LocationOfInterest> = mutableListOf()
  private lateinit var cardSelectedCallback: (LocationOfInterest?) -> Unit
  private lateinit var collectDataCallback: (LocationOfInterest) -> Unit

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = LoiCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [LocationOfInterest] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(itemsList[position])

    // Add highlight border if selected.
    holder.binding.wrapperView.background =
      ResourcesCompat.getDrawable(
        holder.itemView.context.resources,
        if (selectedIndex == position) {
          R.drawable.border
        } else {
          R.color.colorBackground
        },
        null
      )

    // Handle action buttons.
    holder.binding.loiCard.setOnClickListener { handleItemClicked(position) }
    holder.binding.start.setOnClickListener { handleButtonClicked(position) }
    holder.binding.review.setOnClickListener { TODO() }
    holder.binding.markComplete.setOnClickListener { TODO() }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  fun updateSelectedPosition(newSelectedIndex: Int) {
    if (newSelectedIndex < 0 || newSelectedIndex >= itemCount || selectedIndex == newSelectedIndex)
      return
    handleItemClicked(newSelectedIndex)
  }

  fun updateData(newItemsList: List<LocationOfInterest>) {
    itemsList.clear()
    itemsList.addAll(newItemsList)
    selectedIndex = -1
    notifyDataSetChanged()
    cardSelectedCallback.invoke(null)
  }

  fun setLoiCardSelectedCallback(callback: (LocationOfInterest?) -> Unit) {
    this.cardSelectedCallback = callback
  }

  fun setCollectDataCallback(callback: (LocationOfInterest) -> Unit) {
    this.collectDataCallback = callback
  }

  /** Updates the currently selected item. */
  private fun handleItemClicked(position: Int) {
    selectedIndex = position
    notifyDataSetChanged()
    cardSelectedCallback.invoke(itemsList[position])
  }

  private fun handleButtonClicked(position: Int) {
    collectDataCallback.invoke(itemsList[position])
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class ViewHolder(internal val binding: LoiCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(locationOfInterest: LocationOfInterest) {
      binding.loi = locationOfInterest
    }
  }
}
