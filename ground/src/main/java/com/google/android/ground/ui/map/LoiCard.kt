/*
 * Copyright 2022 Google LLC
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

package com.google.android.ground.ui.map

import com.google.android.ground.model.locationofinterest.LocationOfInterest

data class LoiCard(val jobName: String, val loiName: String, val status: String) {

  companion object {
    fun fromLocationOfInterest(locationOfInterest: LocationOfInterest): LoiCard =
      LoiCard(
        loiName = locationOfInterest.caption ?: "empty caption",
        jobName = locationOfInterest.job.name ?: "empty name",
        status = "Completed"
      )
  }
}
