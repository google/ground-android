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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.ground.R
import com.google.android.ground.model.job.Job

/** Renders the entry points for data collection, managing the display of cards and dialogs. */
@Composable
fun DataCollectionEntryPointRender(
  state: DataCollectionEntryPointState,
  onEvent: (DataCollectionEntryPointEvent) -> Unit,
  onJobSelectionModalShown: () -> Unit,
  onJobSelectionModalDismissed: () -> Unit,
  onDataCollected: (DataCollectionEntryPointData) -> Unit,
) {

  /**
   * Starts the data collection process for the given [DataCollectionEntryPointData].
   *
   * @param data The data associated with the location of interest.
   */
  fun startDataCollection(data: DataCollectionEntryPointData) {
    onEvent(DataCollectionEntryPointEvent.StartDataCollection(data))
    onDataCollected(data)
  }

  // Display the LoiJobSheet if it should be shown.
  if (state.showLoiSheet) {
    state.selectedLoiSheetData?.let { loiData ->
      LoiJobSheet(
        loi = loiData.loi,
        canUserSubmitData = loiData.canCollectData,
        submissionCount = loiData.submissionCount,
        onCollectClicked = { startDataCollection(loiData) },
        onDismiss = { onEvent(DataCollectionEntryPointEvent.DismissSelectedLoiJobSheet) },
      )
    }
  }

  // Display the "Add Site" button if the conditions are met.
  if (state.shouldShowAddSiteButton()) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ActionButton(
        icon = Icons.Filled.Add,
        contentDescription = stringResource(id = R.string.add_site),
        onClick = {
          if (state.newLoiJobCardDataList.size == 1) {
            // If there's only one job, start data collection on it directly.
            startDataCollection(state.newLoiJobCardDataList.first())
          } else {
            onEvent(DataCollectionEntryPointEvent.ShowNewLoiJobSelectionModal)
          }
        },
      )
    }
  }

  // Display the JobSelectionModal if it should be shown.
  if (state.showNewLoiJobSelectionModal) {
    onJobSelectionModalShown()
    JobSelectionModal(
      jobs = state.newLoiJobCardDataList.map { it.job },
      onJobClicked = { job -> state.findNewLoiJob(job)?.let { startDataCollection(it) } },
      onDismiss = { onEvent(DataCollectionEntryPointEvent.DismissNewLoiJobSelectionModal) },
    )
  } else {
    onJobSelectionModalDismissed()
  }
}

private fun DataCollectionEntryPointState.findNewLoiJob(job: Job): AdHocDataCollectionButtonData? =
  newLoiJobCardDataList.firstOrNull { it.job == job }

private fun DataCollectionEntryPointState.shouldShowAddSiteButton(): Boolean =
  !showNewLoiJobSelectionModal &&
    newLoiJobCardDataList.isNotEmpty() &&
    newLoiJobCardDataList.all { it.canCollectData }
