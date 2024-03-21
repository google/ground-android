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
package com.google.android.ground.ui.datacollection.tasks.location

import android.content.res.Resources
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.location.CaptureLocationTaskResult.Companion.toCaptureLocationResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Location lock states relevant for attempting to enable it or not. */
enum class LocationLockState {
  /** The default, unknown state. */
  UNKNOWN,

  /** The location lock was already enabled, or an attempt was made. */
  ALREADY_ENABLED,

  /** The location lock was not already enabled. */
  NEEDS_ENABLE,

  /** Trigger to enable the location lock. */
  ENABLE,
}

class CaptureLocationTaskViewModel @Inject constructor(resources: Resources) :
  AbstractTaskViewModel(resources) {

  private val lastLocation = MutableStateFlow<CaptureLocationTaskResult?>(null)
  /**
   * Allows control for triggering the location lock programmatically, and captures a ternary state:
   * 1. null: The location lock was already enabled.
   * 2. false: The location lock needs to be enabled.
   * 3. true: The location lock will be enabled when emitted.
   */
  val enableLocationLockFlow = MutableStateFlow(LocationLockState.UNKNOWN)

  suspend fun updateLocation(location: Location) {
    lastLocation.emit(location.toCaptureLocationResult())
  }

  fun updateResponse() {
    if (lastLocation.value == null) {
      viewModelScope.launch { enableLocationLockFlow.emit(LocationLockState.ENABLE) }
    } else {
      setValue(lastLocation.value)
    }
  }
}
