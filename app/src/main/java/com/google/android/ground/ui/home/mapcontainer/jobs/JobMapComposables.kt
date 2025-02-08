/*
 * Copyright 2025 Google LLC
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

package com.google.android.ground.ui.home.mapcontainer.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.ground.R
import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Manages a set of [Composable] components that renders [LocationOfInterest] cards and dialogs. */
class JobMapComposables {
  private var collectDataListener: MutableState<(DataCollectionEntryPointData) -> Unit> =
    mutableStateOf({})
  private var canUserSubmitData = mutableStateOf(false)
  private var activeLoi: MutableState<SelectedLoiSheetData?> = mutableStateOf(null)
  private val newLoiJobs: MutableList<AdHocDataCollectionButtonData> = mutableStateListOf()
  private var selectedFeatureListener: ((String?) -> Unit) = {}
  private val jobModalOpened = mutableStateOf(false)
  private val jobCardOpened = mutableStateOf(false)
  private val submissionCount = mutableIntStateOf(-1)

  @Composable
  fun Render(onOpen: () -> Unit, onDismiss: () -> Unit) {
    InitializeJobCard()
    InitializeAddLoiButton {
      if (newLoiJobs.size == 1) {
        // If there's only one job, start data collection on it without showing the
        // job modal.
        collectDataListener.value(newLoiJobs.first())
      } else {
        jobModalOpened.value = true
      }
    }
    InitializeJobSelectionModal(onOpen, onDismiss)
  }

  /** Overwrites existing cards. */
  fun updateData(
    canUserSubmitData: Boolean,
    selectedLoi: SelectedLoiSheetData?,
    addLoiJobs: List<AdHocDataCollectionButtonData>,
  ) {
    this.canUserSubmitData.value = canUserSubmitData
    activeLoi.value = selectedLoi
    newLoiJobs.clear()
    newLoiJobs.addAll(addLoiJobs)
    if (selectedLoi != null) {
      submissionCount.intValue = selectedLoi.submissionCount
      jobCardOpened.value = true
      selectedFeatureListener(selectedLoi.loi.id)
    }
  }

  fun setSelectedFeature(listener: (String?) -> Unit) {
    selectedFeatureListener = listener
  }

  fun setCollectDataListener(listener: (DataCollectionEntryPointData) -> Unit) {
    collectDataListener.value = listener
  }

  private fun closeJobCard() {
    jobCardOpened.value = false
    activeLoi.value = null
    selectedFeatureListener(null)
  }

  @Composable
  private fun InitializeAddLoiButton(callback: () -> Unit) {
    val jobs = remember { newLoiJobs }
    val jobModalOpened by remember { jobModalOpened }
    val canUserSubmitData by remember { canUserSubmitData }
    if (jobs.size == 0 || jobModalOpened || !canUserSubmitData) {
      return
    }
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ActionButton(
        icon = Icons.Filled.Add,
        contentDescription = stringResource(id = R.string.add_site),
        onClick = callback,
      )
    }
  }

  @Composable
  private fun InitializeJobSelectionModal(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val buttonDataList = remember { newLoiJobs }
    var openJobsModal by remember { jobModalOpened }
    val collectDataCallback by remember { collectDataListener }
    if (openJobsModal) {
      onOpen()
      JobSelectionModal(
        jobs = buttonDataList.map { it.job },
        onJobClicked = { job ->
          collectDataCallback(AdHocDataCollectionButtonData(job))
          openJobsModal = false
        },
        onDismiss = { openJobsModal = false },
      )
    } else {
      onDismiss()
    }
  }

  @Composable
  private fun InitializeJobCard() {
    val collectDataCallback by remember { collectDataListener }
    val loi by remember { activeLoi }
    val showJobCard by remember { jobCardOpened }

    if (!showJobCard) {
      return
    }

    loi?.let { loiData ->
      LoiJobSheet(
        loi = loiData.loi,
        canUserSubmitDataState = canUserSubmitData,
        submissionCountState = submissionCount,
        onCollectClicked = { collectDataCallback(loiData) },
        onDismiss = { closeJobCard() },
      )
    }
  }
}
