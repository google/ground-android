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
import com.google.android.ground.ui.home.mapcontainer.MapCardAdapter.ViewHolder

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class MapCardAdapter : RecyclerView.Adapter<ViewHolder>() {

  private var focusedIndex: Int = 0
  private val itemsList: MutableList<MapCardUiData> = mutableListOf()
  private lateinit var cardFocusedListener: (MapCardUiData?) -> Unit
  private lateinit var collectDataListener: (MapCardUiData) -> Unit

  /** Creates a new [ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = LoiCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  /** Binds [LocationOfInterest] data to [ViewHolder]. */
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val uiData: MapCardUiData = itemsList[position]

    when (uiData) {
      is MapCardUiData.LoiCardUiData -> holder.bind(uiData.loi)
    }

    // TODO(#1483): Selected card color should match job color
    // Add highlight border if selected.
    val borderDrawable =
      if (focusedIndex == position) {
        R.drawable.loi_card_selected_background
      } else {
        R.drawable.loi_card_default_background
      }
    holder.binding.loiCard.background =
      ResourcesCompat.getDrawable(holder.itemView.context.resources, borderDrawable, null)

    holder.binding.loiCard.setOnClickListener { collectDataListener.invoke(uiData) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  /** Updates the currently focused item. */
  fun focusItemAtIndex(newIndex: Int) {
    if (newIndex < 0 || newIndex >= itemCount || focusedIndex == newIndex) return

    focusedIndex = newIndex
    notifyDataSetChanged()

    cardFocusedListener.invoke(itemsList[newIndex])
  }

  /** Overwrites existing cards. */
  fun updateData(newItemsList: List<MapCardUiData>) {
    itemsList.clear()
    itemsList.addAll(newItemsList)
    focusedIndex = 0
    notifyDataSetChanged()
    cardFocusedListener.invoke(null)
  }

  fun setLoiCardFocusedListener(listener: (MapCardUiData?) -> Unit) {
    this.cardFocusedListener = listener
  }

  fun setCollectDataListener(listener: (MapCardUiData) -> Unit) {
    this.collectDataListener = listener
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class ViewHolder(internal val binding: LoiCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(loi: LocationOfInterest) {
      binding.viewModel = LoiCardViewModel(loi)
    }
  }
}
