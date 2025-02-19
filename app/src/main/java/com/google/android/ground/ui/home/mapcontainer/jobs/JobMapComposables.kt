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
import androidx.compose.runtime.getValue
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
  private var loiJobCardDataState = mutableStateOf<SelectedLoiSheetData?>(null)
  private val newLoiJobCardDataListState = mutableStateListOf<AdHocDataCollectionButtonData>()
  private var selectedFeatureListener: ((String?) -> Unit) = {}
  private val showNewLoiJobSelectionModalState = mutableStateOf(false)
  private val showLoiJobCardState = mutableStateOf(false)

  @Composable
  fun Render(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onCollectData: (DataCollectionEntryPointData) -> Unit,
  ) {
    InitializeJobCard(onCollectClicked = onCollectData)
    InitializeAddLoiButton(onCollectData = onCollectData)
    InitializeJobSelectionModal(onOpen, onDismiss, onCollectData)
  }

  /** Overwrites existing cards. */
  fun updateData(
    selectedLoi: SelectedLoiSheetData?,
    addLoiJobs: List<AdHocDataCollectionButtonData>,
  ) {
    loiJobCardDataState.value = selectedLoi
    newLoiJobCardDataListState.clear()
    newLoiJobCardDataListState.addAll(addLoiJobs)
    if (selectedLoi != null) {
      showLoiJobCardState.value = true
      selectedFeatureListener(selectedLoi.loi.id)
    }
  }

  fun setSelectedFeature(listener: (String?) -> Unit) {
    selectedFeatureListener = listener
  }

  private fun closeJobCard() {
    showLoiJobCardState.value = false
    loiJobCardDataState.value = null
    selectedFeatureListener(null)
  }

  @Composable
  private fun InitializeAddLoiButton(onCollectData: (AdHocDataCollectionButtonData) -> Unit) {
    val jobs = remember { newLoiJobCardDataListState }
    var jobModalOpened by remember { showNewLoiJobSelectionModalState }
    if (jobs.size == 0 || jobModalOpened || !jobs.all { it.canCollectData }) {
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
        onClick = {
          if (jobs.size == 1) {
            // If there's only one job, start data collection on it without showing the job modal.
            onCollectData(jobs.first())
          } else {
            jobModalOpened = true
          }
        },
      )
    }
  }

  @Composable
  private fun InitializeJobSelectionModal(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onCollectData: (DataCollectionEntryPointData) -> Unit,
  ) {
    val buttonDataList = remember { newLoiJobCardDataListState }
    var openJobsModal by remember { showNewLoiJobSelectionModalState }
    if (openJobsModal) {
      onOpen()
      JobSelectionModal(
        jobs = buttonDataList.map { it.job },
        onJobClicked = { job ->
          onCollectData(buttonDataList.first { it.job == job })
          openJobsModal = false
        },
        onDismiss = { openJobsModal = false },
      )
    } else {
      onDismiss()
    }
  }

  @Composable
  private fun InitializeJobCard(onCollectClicked: (SelectedLoiSheetData) -> Unit) {
    val loi by remember { loiJobCardDataState }
    val showJobCard by remember { showLoiJobCardState }

    if (!showJobCard) {
      return
    }

    loi?.let { loiData ->
      LoiJobSheet(
        loi = loiData.loi,
        canUserSubmitData = loiData.canCollectData,
        submissionCount = loiData.submissionCount,
        onCollectClicked = { onCollectClicked(loiData) },
        onDismiss = { closeJobCard() },
      )
    }
  }
}
