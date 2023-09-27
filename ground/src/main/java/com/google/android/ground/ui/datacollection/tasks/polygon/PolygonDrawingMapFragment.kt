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

import androidx.lifecycle.lifecycleScope
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AbstractMapFragmentWithControls::class)
class PolygonDrawingMapFragment(private val viewModel: PolygonDrawingViewModel) :
  Hilt_PolygonDrawingMapFragment() {

  override fun onMapReady(map: MapFragment) {
    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.featureValue.collect { feature: Feature? ->
        map.renderFeatures(if (feature == null) setOf() else setOf(feature))
      }
    }
  }

  override fun onMapCameraMoved(position: CameraPosition) {
    super.onMapCameraMoved(position)
    if (!viewModel.isMarkedComplete()) {
      val mapCenter = position.target!!
      viewModel.updateLastVertexAndMaybeCompletePolygon(mapCenter) { c1, c2 ->
        map.getDistanceInPixels(c1, c2)
      }
    }
  }

  companion object {
    fun newInstance(viewModel: PolygonDrawingViewModel, map: MapFragment) =
      PolygonDrawingMapFragment(viewModel).apply { this.map = map }
  }
}
