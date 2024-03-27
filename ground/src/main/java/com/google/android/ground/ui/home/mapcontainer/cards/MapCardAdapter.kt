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

package com.google.android.ground.ui.home.mapcontainer.cards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.AddLoiCardItemBinding
import com.google.android.ground.databinding.LoiCardItemBinding
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.common.LocationOfInterestHelper

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class MapCardAdapter(
  private val updateSubmissionCount: (loi: LocationOfInterest, view: TextView) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var canUserSubmitData: Boolean = false
  private var focusedIndex: Int = 0
  private var indexOfLastLoi: Int = -1
  private val itemsList: MutableList<MapCardUiData> = mutableListOf()
  private var cardFocusedListener: ((MapCardUiData?) -> Unit)? = null
  private lateinit var collectDataListener: (MapCardUiData) -> Unit

  /** Creates a new [RecyclerView.ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)

    return if (viewType == R.layout.loi_card_item) {
      LoiViewHolder(
        LoiCardItemBinding.inflate(layoutInflater, parent, false),
        updateSubmissionCount,
      )
    } else {
      AddLoiCardViewHolder(AddLoiCardItemBinding.inflate(layoutInflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (position <= indexOfLastLoi) {
      R.layout.loi_card_item
    } else {
      R.layout.add_loi_card_item
    }

  /** Binds [LocationOfInterest] data to [LoiViewHolder] or [AddLoiCardViewHolder]. */
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val uiData = itemsList[position]
    val cardHolder = bindViewHolder(uiData, holder)
    if (focusedIndex == position) {
      cardFocusedListener?.invoke(uiData)
    }
    cardHolder.setOnClickListener { collectDataListener(uiData) }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = itemsList.size

  /** Updates the currently focused item. */
  fun focusItemAtIndex(newIndex: Int) {
    if (newIndex < 0 || newIndex >= itemCount || focusedIndex == newIndex) return

    focusedIndex = newIndex
    notifyDataSetChanged()
  }

  /** Overwrites existing cards. */
  fun updateData(
    canUserSubmitData: Boolean,
    newItemsList: List<MapCardUiData>,
    indexOfLastLoi: Int,
  ) {
    this.canUserSubmitData = canUserSubmitData
    this.indexOfLastLoi = indexOfLastLoi
    itemsList.clear()
    itemsList.addAll(newItemsList)
    focusedIndex = 0
    notifyDataSetChanged()
  }

  fun setLoiCardFocusedListener(listener: (MapCardUiData?) -> Unit) {
    this.cardFocusedListener = listener
  }

  fun setCollectDataListener(listener: (MapCardUiData) -> Unit) {
    this.collectDataListener = listener
  }

  private fun bindViewHolder(
    uiData: MapCardUiData,
    holder: RecyclerView.ViewHolder,
  ): CardViewHolder =
    when (uiData) {
      is MapCardUiData.LoiCardUiData -> {
        (holder as LoiViewHolder).apply { bind(canUserSubmitData, uiData.loi) }
      }

      is MapCardUiData.AddLoiCardUiData -> {
        (holder as AddLoiCardViewHolder).apply { bind(canUserSubmitData, uiData.job) }
      }
    }

  /** Returns index of job card with the given [LocationOfInterest]. */
  fun getIndex(loi: LocationOfInterest): Int {
    for ((index, item) in itemsList.withIndex()) {
      if (item is MapCardUiData.LoiCardUiData && item.loi == loi) {
        return index
      }
    }
    return -1
  }

  abstract class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun setOnClickListener(callback: () -> Unit)
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class LoiViewHolder(
    internal val binding: LoiCardItemBinding,
    private val updateSubmissionCount: (loi: LocationOfInterest, view: TextView) -> Unit,
  ) : CardViewHolder(binding.root) {
    private val loiHelper = LocationOfInterestHelper(itemView.resources)

    fun bind(canUserSubmitData: Boolean, loi: LocationOfInterest) {
      with(binding) {
        loiName.text = loiHelper.getDisplayLoiName(loi)
        jobName.text = loiHelper.getJobName(loi)
        collectData.visibility =
          if (canUserSubmitData && loi.job.hasTasks()) View.VISIBLE else View.GONE
        updateSubmissionCount(loi, submissions)
      }
    }

    override fun setOnClickListener(callback: () -> Unit) {
      binding.collectData.setOnClickListener { callback() }
    }
  }

  /** View item representing the Add Loi Job data in the list. */
  class AddLoiCardViewHolder(internal val binding: AddLoiCardItemBinding) :
    CardViewHolder(binding.root) {

    fun bind(canUserSubmitData: Boolean, job: Job) {
      with(binding) {
        jobName.text = job.name
        collectData.visibility =
          if (canUserSubmitData && job.hasTasks()) View.VISIBLE else View.GONE
      }
    }

    override fun setOnClickListener(callback: () -> Unit) {
      binding.collectData.setOnClickListener { callback() }
    }
  }
}
