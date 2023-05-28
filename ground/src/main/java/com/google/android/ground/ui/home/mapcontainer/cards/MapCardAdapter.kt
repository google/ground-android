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
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.LoiCardItemBinding
import com.google.android.ground.databinding.SuggestLoiCardItemBinding
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class MapCardAdapter(
  private val submissionRepository: SubmissionRepository,
  private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var focusedIndex: Int = 0
  private var indexOfLastLoi: Int = -1
  private val itemsList: MutableList<MapCardUiData> = mutableListOf()
  private var cardFocusedListener: ((MapCardUiData?) -> Unit)? = null
  private lateinit var collectDataListener: (MapCardUiData) -> Unit

  /** Creates a new [RecyclerView.ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)

    return if (viewType == R.layout.loi_card_item) {
      LoiViewHolder(LoiCardItemBinding.inflate(layoutInflater, parent, false))
    } else {
      SuggestLoiViewHolder(SuggestLoiCardItemBinding.inflate(layoutInflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (position <= indexOfLastLoi) {
      R.layout.loi_card_item
    } else {
      R.layout.suggest_loi_card_item
    }

  /** Binds [LocationOfInterest] data to [LoiViewHolder] or [SuggestLoiViewHolder]. */
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val uiData = itemsList[position]
    val cardHolder = bindViewHolder(submissionRepository, lifecycleScope, uiData, holder)

    val shouldFocus = focusedIndex == position
    if (shouldFocus) {
      cardFocusedListener?.invoke(uiData)
    }
    cardHolder.setCardBackground(shouldFocus)
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
  fun updateData(newItemsList: List<MapCardUiData>, indexOfLastLoi: Int) {
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
    submissionRepository: SubmissionRepository,
    lifecycleScope: LifecycleCoroutineScope,
    uiData: MapCardUiData,
    holder: RecyclerView.ViewHolder
  ): CardViewHolder =
    when (uiData) {
      is MapCardUiData.LoiCardUiData -> {
        (holder as LoiViewHolder).apply { bind(submissionRepository, lifecycleScope, uiData.loi) }
      }
      is MapCardUiData.SuggestLoiCardUiData -> {
        (holder as SuggestLoiViewHolder).apply { bind(uiData.job) }
      }
    }

  abstract class CardViewHolder(itemView: View, private val cardView: MaterialCardView) :
    RecyclerView.ViewHolder(itemView) {

    // TODO(#1483): Selected card color should match job color.
    fun setCardBackground(shouldFocus: Boolean) =
      with(cardView) {
        background =
          ResourcesCompat.getDrawable(
            resources,
            if (shouldFocus) {
              R.drawable.loi_card_selected_background
            } else {
              R.drawable.loi_card_default_background
            },
            null
          )
      }

    fun setOnClickListener(callback: () -> Unit) {
      cardView.setOnClickListener { callback() }
    }
  }

  /** View item representing the [LocationOfInterest] data in the list. */
  class LoiViewHolder(internal val binding: LoiCardItemBinding) :
    CardViewHolder(binding.root, binding.loiCard) {

    fun bind(
      submissionRepository: SubmissionRepository,
      lifecycleScope: LifecycleCoroutineScope,
      loi: LocationOfInterest
    ) {
      with(binding) {
        loiName.text = LoiCardUtil.getDisplayLoiName(loi)
        jobName.text = LoiCardUtil.getJobName(loi)

        lifecycleScope.launch {
          val count = submissionRepository.getSubmissions(loi).size
          submissions.text = LoiCardUtil.getSubmissionsText(count)
        }
      }
    }
  }

  /** View item representing the Suggestion Loi Job data in the list. */
  class SuggestLoiViewHolder(internal val binding: SuggestLoiCardItemBinding) :
    CardViewHolder(binding.root, binding.loiCard) {

    fun bind(job: Job) {
      with(binding) { jobName.text = job.name }
    }
  }
}
