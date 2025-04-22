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
package org.groundplatform.android.ui.datacollection.tasks.point

import androidx.lifecycle.LiveData
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment
import org.groundplatform.android.ui.map.CameraPosition
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.MapFragment

@AndroidEntryPoint
class DropPinTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<DropPinTaskViewModel>() {

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)

    // Disable pan/zoom gestures if a marker has been placed on the map.
    taskViewModel.features.observe(this) {
      if (it.isEmpty()) {
        map.enableGestures()
      } else {
        map.disableGestures()
      }
    }
  }

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
