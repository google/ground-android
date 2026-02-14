/*
 * Copyright 2023 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.groundplatform.android.common.Constants.ACCURACY_THRESHOLD_IN_M
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.submission.isNullOrEmpty
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates

class CaptureLocationTaskViewModel @Inject constructor() : AbstractMapTaskViewModel() {

  private val _lastLocation = MutableStateFlow<Location?>(null)
  val lastLocation = _lastLocation.asStateFlow()

  val isCaptureEnabled: Flow<Boolean> =
    _lastLocation.map { location ->
      val accuracy: Float = location?.getAccuracyOrNull()?.toFloat() ?: Float.MAX_VALUE
      location != null && accuracy <= ACCURACY_THRESHOLD_IN_M
    }

  override val taskActionButtonStates: StateFlow<List<ButtonActionState>> by lazy {
    combine(isCaptureEnabled, taskTaskData) { captureEnabled, taskData ->
        listOf(
          getPreviousButton(),
          getSkipButton(taskData),
          getUndoButton(taskData),
          getCaptureLocationButton(captureEnabled, taskData),
          getNextButton(taskData, hideIfEmpty = true),
        )
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())
  }

  fun updateLocation(location: Location) {
    _lastLocation.update { location }
  }

  @VisibleForTesting
  fun updateResponse() {
    val location = _lastLocation.value
    if (location == null) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    } else {
      val accuracy = location.getAccuracyOrNull()
      if (accuracy != null && accuracy > ACCURACY_THRESHOLD_IN_M) {
        error("Location accuracy $accuracy exceeds threshold $ACCURACY_THRESHOLD_IN_M")
      }
      setValue(
        CaptureLocationTaskData(
          location = Point(location.toCoordinates()),
          altitude = location.getAltitudeOrNull(),
          accuracy = accuracy,
        )
      )
    }
  }

  private fun getCaptureLocationButton(
    captureEnabled: Boolean,
    taskData: TaskData?,
  ): ButtonActionState =
    ButtonActionState(
      action = ButtonAction.CAPTURE_LOCATION,
      isEnabled = captureEnabled,
      isVisible = taskData.isNullOrEmpty(),
    )

  override fun onButtonClick(action: ButtonAction) {
    if (action == ButtonAction.CAPTURE_LOCATION) {
      updateResponse()
    } else {
      super.onButtonClick(action)
    }
  }
}
