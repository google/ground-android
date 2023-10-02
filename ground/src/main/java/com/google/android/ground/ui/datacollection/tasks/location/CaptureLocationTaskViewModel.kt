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
import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.model.submission.LocationTaskData.Companion.toTaskData
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

class CaptureLocationTaskViewModel @Inject constructor(resources: Resources) :
  AbstractTaskViewModel(resources) {

  private val lastLocation = MutableStateFlow<LocationTaskData?>(null)

  suspend fun updateLocation(location: Location) {
    lastLocation.emit(location.toTaskData())
  }

  fun updateResponse() {
    setResponse(lastLocation.value)
  }
}
