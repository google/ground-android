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
package com.google.android.ground.ui.datacollection.tasks.polygon

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskMapFragment
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DrawAreaTaskMapFragment @Inject constructor() :
  AbstractTaskMapFragment<DrawAreaTaskViewModel, BaseMapViewModel>() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(BaseMapViewModel::class.java)
  }

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)
    viewLifecycleOwner.lifecycleScope.launch {
      parentViewModel.draftArea.collect { feature: Feature? ->
        map.setFeatures(if (feature == null) setOf() else setOf(feature))
      }
    }

    // If the task has any previously drawn area, restore map viewport to the feature.
    moveViewportToFeature(parentViewModel.draftArea.value)
  }

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    if (!parentViewModel.isMarkedComplete()) {
      val mapCenter = position.coordinates
      parentViewModel.updateLastVertexAndMaybeCompletePolygon(mapCenter) { c1, c2 ->
        map.getDistanceInPixels(c1, c2)
      }
    }
  }

  private fun moveViewportToFeature(feature: Feature?) {
    val geometry = feature?.geometry ?: return
    val bounds = listOf(geometry).toBounds() ?: return
    moveToBounds(bounds, padding = 200, shouldAnimate = false)
  }
}
