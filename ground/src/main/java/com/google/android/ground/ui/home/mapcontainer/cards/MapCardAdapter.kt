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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.AddLoiItemBinding
import com.google.android.ground.databinding.LoiCardItemBinding
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.theme.AppTheme

/**
 * An implementation of [RecyclerView.Adapter] that associates [LocationOfInterest] data with the
 * [ViewHolder] views.
 */
class MapCardAdapter(
  private val updateSubmissionCount: (loi: LocationOfInterest, view: TextView) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  private var canUserSubmitData: Boolean = false
  private var activeLoi: MapUiData.LoiUiData? = null
  private val newLoiJobs: MutableList<MapUiData.AddLoiUiData> = mutableListOf()
  private var cardFocusedListener: ((MapUiData?) -> Unit)? = null
  private lateinit var collectDataListener: (MapUiData) -> Unit

  /** Creates a new [RecyclerView.ViewHolder] item without any data. */
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)

    return if (viewType == R.layout.loi_card_item) {
      LoiViewHolder(
        LoiCardItemBinding.inflate(layoutInflater, parent, false),
        updateSubmissionCount,
      )
    } else {
      AddLoiCardViewHolder(AddLoiItemBinding.inflate(layoutInflater, parent, false))
    }
  }

  override fun getItemViewType(position: Int): Int =
    if (activeLoi != null) {
      R.layout.loi_card_item
    } else {
      // Assume we don't render add LOI option unless we know the job allows it.
      R.layout.add_loi_item
    }

  /** Binds [LocationOfInterest] data to [LoiViewHolder] or [AddLoiCardViewHolder]. */
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val loi = activeLoi
    if (loi != null) {
      val cardHolder = bindLoiCardViewHolder(loi, holder)
      cardHolder.setOnClickListener { collectDataListener(loi) }
    } else {
      bindAddLoiCardViewHolder(newLoiJobs, holder) { job -> collectDataListener(job) }
    }
  }

  /** Returns the size of the list. */
  override fun getItemCount() = if (activeLoi != null || newLoiJobs.isNotEmpty()) 1 else 0

  /** Overwrites existing cards. */
  fun updateData(
    canUserSubmitData: Boolean,
    loiUi: MapUiData.LoiUiData?,
    jobUi: List<MapUiData.AddLoiUiData>,
  ) {
    this.canUserSubmitData = canUserSubmitData
    activeLoi = loiUi
    newLoiJobs.clear()
    newLoiJobs.addAll(jobUi)
    notifyDataSetChanged()
  }

  fun setLoiCardFocusedListener(listener: (MapUiData?) -> Unit) {
    this.cardFocusedListener = listener
  }

  fun setCollectDataListener(listener: (MapUiData) -> Unit) {
    this.collectDataListener = listener
  }

  private fun bindLoiCardViewHolder(
    loiData: MapUiData.LoiUiData,
    holder: RecyclerView.ViewHolder,
  ): LoiViewHolder = (holder as LoiViewHolder).apply { bind(canUserSubmitData, loiData.loi) }

  private fun bindAddLoiCardViewHolder(
    addLoiJobData: List<MapUiData.AddLoiUiData>,
    holder: RecyclerView.ViewHolder,
    callback: (MapUiData.AddLoiUiData) -> Unit,
  ): AddLoiCardViewHolder = (holder as AddLoiCardViewHolder).apply { bind(addLoiJobData, callback) }

  abstract class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

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
        // NOTE(#2539): The DataCollectionFragment will crash if there are no non-LOI tasks.
        collectData.visibility =
          if (canUserSubmitData && loi.job.hasNonLoiTasks()) View.VISIBLE else View.GONE
        updateSubmissionCount(loi, submissions)
      }
    }

    fun setOnClickListener(callback: () -> Unit) {
      binding.collectData.setOnClickListener { callback() }
    }
  }

  /** View item representing the Add Loi Job data in the list. */
  class AddLoiCardViewHolder(internal val binding: AddLoiItemBinding) :
    CardViewHolder(binding.root) {
    private val jobDialogOpened = mutableStateOf(false)

    fun bind(
      jobs: List<MapUiData.AddLoiUiData>,
      callback: (MapUiData.AddLoiUiData) -> Unit,
    ) {
      with(binding) {
        loiUi.setOnClickListener {
          jobDialogOpened.value = true
          (root as ViewGroup).addView(
            ComposeView(root.context).apply {
              setContent { AppTheme { ShowJobSelectionDialog(jobs, callback, jobDialogOpened) } }
            }
          )
        }
      }
    }

    @Composable
    fun ShowJobSelectionDialog(
      jobs: List<MapUiData.AddLoiUiData>,
      callback: (MapUiData.AddLoiUiData) -> Unit,
      jobDialogOpened: MutableState<Boolean>,
    ) {
      var selectedJobId by rememberSaveable { mutableStateOf(jobs[0].job.id) }
      var openJobsDialog by rememberSaveable { jobDialogOpened }
      if (openJobsDialog) {
        JobSelectionDialog(
          selectedJobId = selectedJobId,
          jobs = jobs,
          onJobSelection = { selectedJobId = it.job.id },
          onConfirmRequest = { callback(it) },
          onDismissRequest = { openJobsDialog = false },
        )
      }
    }
  }
}
