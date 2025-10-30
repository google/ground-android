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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.ui.common.MapConfig
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskMapFragment
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.MapFragment
import javax.inject.Inject

@AndroidEntryPoint
class DropPinTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<DropPinTaskViewModel>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)

    // Collect location updates for location lock mode
    viewLifecycleOwner.lifecycleScope.launch {
      getMapViewModel().getLocationUpdates().collect { taskViewModel.updateLocation(it) }
    }

    return root
  }

  override fun getMapConfig(): MapConfig {
    // If allowMovingPoint is false, disable gestures (GPS location only mode)
    val allowGestures = taskViewModel.task.allowMovingPoint
    return super.getMapConfig().copy(allowGestures = allowGestures)
  }

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)

    // Initialize location updates for location lock
    viewLifecycleOwner.lifecycleScope.launch {
      taskViewModel.initLocationUpdates(getMapViewModel())
    }

    // Show center pin when there's no marker placed yet
    taskViewModel.features.observe(this) {
      setCenterMarkerVisibility(it.isEmpty())
      if (it.isEmpty()) {
        // Enable gestures if a marker has not been placed and allowMovingPoint is true
        if (taskViewModel.task.allowMovingPoint) {
          map.enableGestures()
        }
      } else {
        // Disable gestures once a marker is placed
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
