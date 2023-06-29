/*
 * Copyright 2018 Google LLC
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

package com.google.android.ground.ui.home

import com.google.android.ground.model.locationofinterest.LocationOfInterest

class BottomSheetState
private constructor(
  val visibility: Visibility,
  val locationOfInterest: LocationOfInterest? = null
) {

  enum class Visibility {
    VISIBLE,
    HIDDEN
  }

  val isVisible: Boolean
    get() = Visibility.VISIBLE == visibility

  companion object {
    fun visible(locationOfInterest: LocationOfInterest) =
      BottomSheetState(Visibility.VISIBLE, locationOfInterest)

    fun hidden() = BottomSheetState(Visibility.HIDDEN)
  }
}
