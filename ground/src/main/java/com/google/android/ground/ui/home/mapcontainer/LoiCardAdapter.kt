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

  private var focusedIndex: Int = -1
  private val itemsList: MutableList<LocationOfInterest> = mutableListOf()
  private lateinit var cardFocusedCallback: (LocationOfInterest?) -> Unit
  private lateinit var collectDataCallback: (LocationOfInterest) -> Unit

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = LoiCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [LocationOfInterest] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val loi: LocationOfInterest = itemsList[position]

    holder.bind(loi)

    // Add highlight border if selected.
    holder.binding.wrapperView.background =
      ResourcesCompat.getDrawable(
        holder.itemView.context.resources,
        if (focusedIndex == position) {
          R.drawable.border
        } else {
          R.color.colorBackground
        },
        null
      )

    holder.binding.loiCard.setOnClickListener { collectDataCallback.invoke(loi) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  /** Updates the currently focused item. */
  fun focusItemAtIndex(newIndex: Int) {
    if (newIndex < 0 || newIndex >= itemCount || focusedIndex == newIndex) return

    focusedIndex = newIndex
    notifyDataSetChanged()

    cardFocusedCallback.invoke(itemsList[newIndex])
  }

  /** Overwrites existing cards. */
  fun updateData(newItemsList: List<LocationOfInterest>) {
    itemsList.clear()
    itemsList.addAll(newItemsList)
    focusedIndex = -1
    notifyDataSetChanged()
    cardFocusedCallback.invoke(null)
  }

  fun setLoiCardFocusedCallback(callback: (LocationOfInterest?) -> Unit) {
    this.cardFocusedCallback = callback
  }

  fun setCollectDataCallback(callback: (LocationOfInterest) -> Unit) {
    this.collectDataCallback = callback
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class ViewHolder(internal val binding: LoiCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(locationOfInterest: LocationOfInterest) {
      binding.loi = locationOfInterest
    }
  }
}
