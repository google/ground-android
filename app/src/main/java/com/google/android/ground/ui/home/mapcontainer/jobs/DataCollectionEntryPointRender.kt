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
import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Manages a set of [Composable] components that renders [LocationOfInterest] cards and dialogs. */
@Composable
fun DataCollectionEntryPointRender(
  state: DataCollectionEntryPointState,
  onEvent: (DataCollectionEntryPointEvent) -> Unit,
  onFeatureSelected: (String?) -> Unit,
  onJobSelectionModalShown: () -> Unit,
  onJobSelectionModalDismissed: () -> Unit,
  onCollectData: (DataCollectionEntryPointData) -> Unit,
) {

  fun startDataCollection(data: DataCollectionEntryPointData) {
    onEvent(DataCollectionEntryPointEvent.StartDataCollection(data))
    onCollectData(data)
  }

  if (state.showLoiSheet) {
    state.selectedLoiSheetData?.let { loiData ->
      onFeatureSelected(loiData.loi.id)
      LoiJobSheet(
        loi = loiData.loi,
        canUserSubmitData = loiData.canCollectData,
        submissionCount = loiData.submissionCount,
        onCollectClicked = { startDataCollection(loiData) },
        onDismiss = {
          onFeatureSelected(null)
          onEvent(DataCollectionEntryPointEvent.DismissSelectedLoiJobSheet)
        },
      )
    }
  }

  if (
    !state.showNewLoiJobSelectionModal &&
      state.newLoiJobCardDataList.isNotEmpty() &&
      state.newLoiJobCardDataList.all { it.canCollectData }
  ) {
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
            // If there's only one job, start data collection on it without showing the job modal.
            startDataCollection(state.newLoiJobCardDataList.first())
          } else {
            onEvent(DataCollectionEntryPointEvent.ShowNewLoiJobSelectionModal)
          }
        },
      )
    }
  }

  if (state.showNewLoiJobSelectionModal) {
    onJobSelectionModalShown()
    JobSelectionModal(
      jobs = state.newLoiJobCardDataList.map { it.job },
      onJobClicked = { job ->
        startDataCollection(state.newLoiJobCardDataList.first { it.job == job })
      },
      onDismiss = { onEvent(DataCollectionEntryPointEvent.DismissNewLoiJobSelectionModal) },
    )
  } else {
    onJobSelectionModalDismissed()
  }
}
