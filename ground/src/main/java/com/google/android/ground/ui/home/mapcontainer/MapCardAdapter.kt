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
import com.google.android.ground.databinding.SuggestLoiCardItemBinding
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.material.card.MaterialCardView

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class MapCardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var focusedIndex: Int = 0
  private var indexOfLastLoi: Int = -1
  private val itemsList: MutableList<MapCardUiData> = mutableListOf()
  private lateinit var cardFocusedListener: (MapCardUiData?) -> Unit
  private lateinit var collectDataListener: (MapCardUiData) -> Unit

  /** Creates a new [RecyclerView.ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
    if (viewType == R.layout.loi_card_item) {
      LoiViewHolder(LoiCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    } else {
      SuggestLoiViewHolder(
        SuggestLoiCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      )
    }

  override fun getItemViewType(position: Int): Int =
    if (position <= indexOfLastLoi) {
      R.layout.loi_card_item
    } else {
      R.layout.suggest_loi_card_item
    }

  /** Binds [LocationOfInterest] data to [LoiViewHolder] or [SuggestLoiViewHolder]. */
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val uiData: MapCardUiData = itemsList[position]

    var loiCard: MaterialCardView? = null
    when (uiData) {
      is MapCardUiData.LoiCardUiData -> {
        with(holder as LoiViewHolder) {
          bind(uiData.loi)
          loiCard = binding.loiCard
        }
      }
      is MapCardUiData.SuggestLoiCardUiData ->
        with(holder as SuggestLoiViewHolder) {
          bind(uiData.job)
          loiCard = binding.loiCard
        }
    }

    // TODO(#1483): Selected card color should match job color
    // Add highlight border if selected.
    val borderDrawable =
      if (focusedIndex == position) {
        R.drawable.loi_card_selected_background
      } else {
        R.drawable.loi_card_default_background
      }
    loiCard?.background =
      ResourcesCompat.getDrawable(holder.itemView.context.resources, borderDrawable, null)

    loiCard?.setOnClickListener { collectDataListener.invoke(uiData) }
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
  fun updateData(newItemsList: List<MapCardUiData>, indexOfLastLoi: Int) {
    this.indexOfLastLoi = indexOfLastLoi
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
  class LoiViewHolder(internal val binding: LoiCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(loi: LocationOfInterest) {
      binding.viewModel = LoiCardViewModel(loi)
    }
  }

  /** View item representing the Suggestion Loi Job data in the list. */
  class SuggestLoiViewHolder(internal val binding: SuggestLoiCardItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(job: Job) {
      binding.viewModel = SuggestLoiCardViewModel(job)
    }
  }
}
