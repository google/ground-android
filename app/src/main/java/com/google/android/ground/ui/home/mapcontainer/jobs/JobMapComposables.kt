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

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.R
import com.google.android.ground.model.job.getDefaultColor
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.theme.AppTheme
import com.google.android.ground.util.createComposeView
import kotlinx.coroutines.launch

/** Manages a set of [Composable] components that renders [LocationOfInterest] cards and dialogs. */
class JobMapComposables(private val getSubmissionCount: suspend (loi: LocationOfInterest) -> Int) {
  private var collectDataListener: MutableState<(DataCollectionEntryPointData) -> Unit> =
    mutableStateOf({})
  private var canUserSubmitData = mutableStateOf(false)
  private var activeLoi: MutableState<SelectedLoiSheetData?> = mutableStateOf(null)
  private val newLoiJobs: MutableList<AdHocDataCollectionButtonData> = mutableStateListOf()
  private var selectedFeatureListener: ((String?) -> Unit) = {}
  private val jobModalOpened = mutableStateOf(false)
  private val jobCardOpened = mutableStateOf(false)
  private val submissionCount = mutableIntStateOf(-1)

  fun render(view: ViewGroup, onOpen: () -> Unit, onDismiss: () -> Unit) {
    initializeJobCard(view)
    initializeAddLoiButton(view, onOpen, onDismiss)
  }

  /** Overwrites existing cards. */
  suspend fun updateData(
    canUserSubmitData: Boolean,
    selectedLoi: SelectedLoiSheetData?,
    addLoiJobs: List<AdHocDataCollectionButtonData>,
  ) {
    this.canUserSubmitData.value = canUserSubmitData
    activeLoi.value = selectedLoi
    newLoiJobs.clear()
    newLoiJobs.addAll(addLoiJobs)
    if (selectedLoi != null) {
      submissionCount.intValue = getSubmissionCount(selectedLoi.loi)
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

  private fun initializeAddLoiButton(view: ViewGroup, onOpen: () -> Unit, onDismiss: () -> Unit) {
    view.addView(
      view.createComposeView {
        AppTheme {
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
      }
    )
  }

  private fun initializeJobCard(view: ViewGroup) {
    view.addView(view.createComposeView { AppTheme { InitializeJobCard() } })
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
        callback = callback,
      )
    }
  }

  @Composable
  private fun InitializeJobSelectionModal(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val jobs = remember { newLoiJobs }
    var openJobsModal by remember { jobModalOpened }
    val collectDataCallback by remember { collectDataListener }
    if (openJobsModal) {
      onOpen()
      Modal(onDismiss = { openJobsModal = false }) {
        jobs.forEach { job ->
          JobSelectionRow(job) {
            collectDataCallback(job)
            openJobsModal = false
          }
          Spacer(Modifier.height(16.dp))
        }
      }
    } else {
      onDismiss()
    }
  }

  @Suppress("CognitiveComplexMethod", "LongMethod")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun InitializeJobCard() {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val showJobCard by remember { jobCardOpened }
    val loi by remember { activeLoi }
    val submissionCount by remember { submissionCount }
    val canUserSubmitData by remember { canUserSubmitData }
    val collectDataCallback by remember { collectDataListener }
    val loiHelper = LocationOfInterestHelper(LocalContext.current.resources)

    if (!showJobCard) {
      return
    }
    loi?.let { loiData ->
      ModalBottomSheet(
        onDismissRequest = { closeJobCard() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(width = 32.dp) },
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
          Text(
            loiHelper.getJobName(loiData.loi) ?: "",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
          )
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_ring_marker),
              contentDescription = stringResource(R.string.job_site_icon),
              modifier = Modifier.size(32.dp),
              tint = Color(loiData.loi.job.getDefaultColor()),
            )
            Spacer(modifier = Modifier.size(18.dp))
            Text(
              loiHelper.getDisplayLoiName(loiData.loi),
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 28.sp,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              if (submissionCount <= 0) stringResource(R.string.no_submissions)
              else
                pluralStringResource(R.plurals.submission_count, submissionCount, submissionCount),
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 16.sp,
            )
            // NOTE(#2539): The DataCollectionFragment will crash if there are no non-LOI tasks.
            if (canUserSubmitData && loiData.loi.job.hasNonLoiTasks()) {
              Button(
                onClick = {
                  scope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion {
                      if (!sheetState.isVisible) {
                        closeJobCard()
                      }
                      collectDataCallback(loiData)
                    }
                }
              ) {
                Text(
                  stringResource(R.string.add_data),
                  modifier = Modifier.padding(4.dp),
                  fontSize = 18.sp,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ActionButton(icon: ImageVector, contentDescription: String, callback: () -> Unit) {
  Button(
    onClick = callback,
    modifier = Modifier.size(width = 100.dp, height = 100.dp),
    colors =
      ButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = Color.Black,
        disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
        disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor,
      ),
    shape =
      RoundedCornerShape(
        topStartPercent = 25,
        topEndPercent = 25,
        bottomStartPercent = 25,
        bottomEndPercent = 25,
      ),
  ) {
    Icon(imageVector = icon, contentDescription = contentDescription, Modifier.size(65.dp))
  }
}

@Composable
fun Modal(onDismiss: () -> Unit, content: @Composable () -> Unit) {
  Column(
    Modifier.fillMaxSize()
      .background(
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0.0f to Color.Black.copy(alpha = 0.75F),
              0.9f to Color.DarkGray.copy(alpha = 0.6F),
              1f to Color.Transparent,
            )
        )
      )
      .pointerInput(Unit) { detectTapGestures {} }
      .clickable(onClick = onDismiss),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.weight(1F))
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      content()
    }
    Box(modifier = Modifier.weight(1F).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
      ActionButton(icon = Icons.Filled.Clear, contentDescription = stringResource(R.string.close)) {
        onDismiss()
      }
    }
  }
}

@Composable
fun JobSelectionRow(job: AdHocDataCollectionButtonData, onJobSelected: () -> Unit) {
  Button(
    onClick = { onJobSelected() },
    modifier = Modifier.fillMaxWidth(0.65F).clickable { onJobSelected() },
    shape =
      RoundedCornerShape(
        topStartPercent = 25,
        topEndPercent = 25,
        bottomStartPercent = 25,
        bottomEndPercent = 25,
      ),
    colors =
      ButtonDefaults.buttonColors()
        .copy(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_ring_marker),
        contentDescription = stringResource(R.string.job_site_icon),
        modifier = Modifier.size(32.dp),
        tint = Color(job.job.getDefaultColor()),
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(
        job.job.name ?: stringResource(R.string.unnamed_job),
        modifier = Modifier.padding(16.dp),
        fontSize = 24.sp,
      )
    }
  }
}
