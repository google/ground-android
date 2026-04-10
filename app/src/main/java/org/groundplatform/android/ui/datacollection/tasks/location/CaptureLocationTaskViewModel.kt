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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.common.Constants.ACCURACY_THRESHOLD_IN_M
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.AbstractMapTaskViewModel
import org.groundplatform.android.ui.datacollection.tasks.LocationLockEnabledState
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates
import org.groundplatform.domain.model.geometry.Point
import org.groundplatform.domain.model.submission.CaptureLocationTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.submission.isNullOrEmpty
import javax.inject.Inject

class CaptureLocationTaskViewModel @Inject constructor() : AbstractMapTaskViewModel() {

  private val _showPermissionDeniedDialog = MutableStateFlow(false)

  private val _lastLocation = MutableStateFlow<Location?>(null)

  private val _userDismissedAccuracyCard = MutableStateFlow(false)

  val showAccuracyCard: StateFlow<Boolean> =
    combine(_lastLocation, _userDismissedAccuracyCard) { location, dismissed ->
        location != null && !location.isAccurate() && !dismissed
      }
      .stateIn(viewModelScope, WhileSubscribed(5_000), false)

  override val taskActionButtonStates: StateFlow<List<ButtonActionState>> by lazy {
    combine(_lastLocation, taskTaskData) { location, taskData ->
        listOf(
          getPreviousButton(),
          getSkipButton(taskData),
          getUndoButton(taskData),
          getCaptureLocationButton(location.isAccurate(), taskData),
          getNextButton(taskData, hideIfEmpty = true),
        )
      }
      .distinctUntilChanged()
      .stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())
  }

  val showPermissionDeniedDialog: StateFlow<Boolean> = _showPermissionDeniedDialog.asStateFlow()

  init {
    viewModelScope.launch {
      enableLocationLockFlow.collect { lockState ->
        if (lockState == LocationLockEnabledState.NEEDS_ENABLE) {
          _showPermissionDeniedDialog.value = true
        }
      }
    }
  }

  fun dismissAccuracyCard() {
    _userDismissedAccuracyCard.value = true
  }

  private fun dismissPermissionDeniedDialog() {
    _showPermissionDeniedDialog.value = false
  }

  fun onAllowLocationClicked() {
    dismissPermissionDeniedDialog()
  }

  fun updateLocation(location: Location) {
    _lastLocation.update { location }
  }

  private fun updateResponse(location: Location?) {
    requireNotNull(location)
    require(location.isAccurate())
    setValue(location.toTaskData())
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
      updateResponse(_lastLocation.value)
    } else {
      super.onButtonClick(action)
    }
  }

  private fun Location?.isAccurate(): Boolean {
    if (this == null) return false
    val accuracy = getAccuracyOrNull()?.toFloat() ?: Float.MAX_VALUE
    return accuracy <= ACCURACY_THRESHOLD_IN_M
  }

  private fun Location.toTaskData() =
    CaptureLocationTaskData(
      location = Point(toCoordinates()),
      altitude = getAltitudeOrNull(),
      accuracy = getAccuracyOrNull(),
    )
}
