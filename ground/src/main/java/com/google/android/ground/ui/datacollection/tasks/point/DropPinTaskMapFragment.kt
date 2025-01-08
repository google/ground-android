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
package com.google.android.ground.ui.datacollection.tasks.point

import androidx.lifecycle.LiveData
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskMapFragment
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DropPinTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<DropPinTaskViewModel>() {

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    taskViewModel.updateCameraPosition(position)
  }

  override fun renderFeatures(): LiveData<Set<Feature>> = taskViewModel.features

  override fun setDefaultViewPort() {
    val feature = taskViewModel.features.value?.firstOrNull() ?: return
    val coordinates = feature.geometry.center()
    moveToPosition(coordinates)
  }
}
