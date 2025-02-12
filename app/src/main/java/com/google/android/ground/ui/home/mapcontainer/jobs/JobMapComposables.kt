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
  private var loiSheetDataState = mutableStateOf<SelectedLoiSheetData?>(null)
  private val newLoiJobCardDataListState = mutableStateListOf<AdHocDataCollectionButtonData>()
  private var selectedFeatureListener: ((String?) -> Unit) = {}
  private val showNewLoiJobSelectionModalState = mutableStateOf(false)
  private val showLoiSheetState = mutableStateOf(false)

  @Composable
  fun Render(
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onCollectData: (DataCollectionEntryPointData) -> Unit,
  ) {
    var loiSheetData by remember { loiSheetDataState }
    var showLoiSheet by remember { showLoiSheetState }
    val newLoiJobCardDataList = remember { newLoiJobCardDataListState }
    var showNewLoiJobSelectionModal by remember { showNewLoiJobSelectionModalState }

    if (showLoiSheet) {
      loiSheetData?.let { loiData ->
        LoiJobSheet(
          loi = loiData.loi,
          canUserSubmitData = loiData.canCollectData,
          submissionCount = loiData.submissionCount,
          onCollectClicked = { onCollectData(loiData) },
          onDismiss = {
            showLoiSheet = false
            loiSheetData = null
            selectedFeatureListener(null)
          },
        )
      }
    }

    if (
      newLoiJobCardDataList.size != 0 &&
        !showNewLoiJobSelectionModal &&
        newLoiJobCardDataList.all { it.canCollectData }
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
            if (newLoiJobCardDataList.size == 1) {
              // If there's only one job, start data collection on it without showing the job modal.
              onCollectData(newLoiJobCardDataList.first())
            } else {
              showNewLoiJobSelectionModal = true
            }
          },
        )
      }
    }

    if (showNewLoiJobSelectionModal) {
      onOpen()
      JobSelectionModal(
        jobs = newLoiJobCardDataList.map { it.job },
        onJobClicked = { job ->
          onCollectData(newLoiJobCardDataList.first { it.job == job })
          showNewLoiJobSelectionModal = false
        },
        onDismiss = { showNewLoiJobSelectionModal = false },
      )
    } else {
      onDismiss()
    }
  }

  /** Overwrites existing cards. */
  fun updateData(
    selectedLoi: SelectedLoiSheetData?,
    addLoiJobs: List<AdHocDataCollectionButtonData>,
  ) {
    loiSheetDataState.value = selectedLoi
    newLoiJobCardDataListState.clear()
    newLoiJobCardDataListState.addAll(addLoiJobs)
    if (selectedLoi != null) {
      showLoiSheetState.value = true
      selectedFeatureListener(selectedLoi.loi.id)
    }
  }

  fun setSelectedFeature(listener: (String?) -> Unit) {
    selectedFeatureListener = listener
  }
}
